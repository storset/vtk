/* Copyright (c) 2013, University of Oslo, Norway
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of the University of Oslo nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.vortikal.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.content.VideoRefContent;
import org.vortikal.repository.resourcetype.Content;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.store.ContentStore;
import org.vortikal.util.io.StreamUtil;
import org.vortikal.util.io.StreamUtil.TempFile;
import org.vortikal.video.rest.VideoApiClient;
import org.vortikal.video.rest.VideoRef;

/**
 * Interceptor which allows for specialized handling of resource content and
 * control over how resource shall be stored in regular repository.
 * 
 * TODO Rename and pull out a general interface, if we decide for this approach.
 *      Might be "content-centric" or just a generic set of well-defined hook points.
 */
public class ResourceContentInterceptor implements InitializingBean {

    private String videoStorageRoot;
    private String repositoryId;
    private VideoApiClient videoapp;
    
    // TODO maybe use assertions instead of these predicate methods:
    public boolean isSupportedContentType(String contentType) {
        return contentType != null &&
                ("video".equals(contentType) || contentType.startsWith("video/"));
    }
    
    public boolean isSupportedResourceType(String resourceType) {
        return "videoref".equals(resourceType);
    }
    
    public ResourceImpl interceptCreate(ResourceImpl resource,
            InputStream stream, String contentType, ContentStore defaultContentStore) 
            throws IOException {

        // Dump input stream to file in video storage input area
        String name = resource.getURI().getName();
        TempFile tempFile = StreamUtil.streamToTempFile(stream, -1, new File(getInputDirAbspath()), name);

        // Make a videoapp API call to create new getVideo
        VideoRef ref = this.videoapp.createVideo(resource, tempFile.getFile().getAbsolutePath(), contentType);
        
        tempFile.delete();
        
        // Store videoref getVideo JSON as resource main content
        storeVideoRef(ref, resource.getURI(), defaultContentStore);

        // Return new resource for further type evaluation in repository
        return resource;
    }
    
    public ResourceImpl interceptStoreContent(ResourceImpl resource, InputStream stream, String contentType,
            ContentStore defaultContentStore)
            throws IOException {

        // Dump input stream to file in video storage input area
        String name = resource.getURI().getName();
        // XXX consuming entire input stream, but need a way to revert if this all fails (fallback to regular store in repo).
        TempFile tempFile = StreamUtil.streamToTempFile(stream, -1, new File(getInputDirAbspath()), name);

        // Make a videoapp API call to create new getVideo
        VideoRef newRef = this.videoapp.createVideo(resource, tempFile.getFile().getAbsolutePath(), contentType);
        tempFile.delete();
        
        // Preserve any Vortex-specific data in old ref
        VideoRef ref;
        try {
            // Refresh old ref with new videoId
            VideoRef oldRef = loadVideoRef(resource.getURI(), defaultContentStore);
            VideoRef.Builder oldRefBuilder = oldRef.copyBuilder();
            ref = oldRefBuilder.videoId(newRef.videoId())
                         .sourceVideo(newRef.sourceVideo())
                         .convertedVideo(newRef.convertedVideo())
                         .build();
        } catch (Exception e) {
            ref = newRef;
        }
        
        ref = ref.copyBuilder().uploadContentType(contentType)
                               .refUpdateTimestamp(new Date()).build();
        
        // Store videoref getVideo JSON as resource main content
        storeVideoRef(ref, resource.getURI(), defaultContentStore);

        return resource;
    }
    
    
    public ResourceImpl interceptStore(ResourceImpl resource, ContentStore store) 
            throws IOException {
        
        // Do not allow changing contentType, set contentType from source stream or original upload type.
        VideoRef ref = loadVideoRef(resource.getURI(), store);
        String contentType;
        if (ref.sourceVideo() != null && ref.sourceVideo().contentType() != null) {
            contentType = ref.sourceVideo().contentType();
        } else if (ref.uploadContentType() != null) {
            contentType = ref.uploadContentType();
        } else {
            throw new IllegalStateException("No content-type in video ref");
        }

        // XXX Should be AuthorizationException on attempt to change contentType-prop
        //     for videoref resource type (now we just silently modify).
        Property contentTypeProp = resource.getProperty(Namespace.DEFAULT_NAMESPACE, 
                                            PropertyType.CONTENTTYPE_PROP_NAME);
        contentTypeProp.setStringValue(contentType);
        
        return resource;
    }
    
    public ResourceImpl interceptStoreSystemChange(ResourceImpl resource, ContentStore store, SystemChangeContext context) 
        throws IOException {
        return interceptStore(resource, store);
    }
    
    public ResourceImpl interceptStoreInheritableProps(ResourceImpl resource, ContentStore store,
                            InheritablePropertiesStoreContext context) 
        throws IOException {
        return interceptStore(resource, store);
    }
    
    public InputStream interceptGetInputStream(ResourceImpl resource, ContentStore store) 
            throws IOException {
        
        VideoRef ref = loadVideoRef(resource.getURI(), store);
        
        if (ref.sourceVideo() != null) {
            return new FileInputStream(new File(ref.sourceVideo().path()));
        }

        throw new IOException("Stream not found");
    }
    
    public Content interceptGetContentForEvaluation(Resource resource, 
                   final Content defaultContent, ContentStore defaultContentStore) throws IOException {
        
        return new VideoRefContent(resource, defaultContent, defaultContentStore);
    }
    
    public ResourceImpl interceptRetrieve(ResourceImpl resource, ContentStore store) throws IOException {
        
        // Currently a no-op.
        return resource;
    }

    private VideoRef loadVideoRef(Path uri, ContentStore store) throws IOException {
        String jsonString = StreamUtil.streamToString(store.getInputStream(uri), "utf-8");
        return VideoRef.fromJsonString(jsonString).build();
    }
    
    private void storeVideoRef(VideoRef ref, Path uri, ContentStore store) throws IOException {
        InputStream jsonStream = StreamUtil.stringToStream(ref.toJsonString(), "utf-8");
        store.storeContent(uri, jsonStream);
    }
    
    private String getInputDirAbspath() {
        return this.videoStorageRoot + "/videoinput/" + this.repositoryId;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        File rootDir = new File(this.videoStorageRoot);
        if (!rootDir.isDirectory()) {
            throw new IOException("Not a directory: " + this.videoStorageRoot);
        }
        if (!rootDir.isAbsolute()) {
            throw new IOException("Not an absolute path: " + this.videoStorageRoot);
        }
    }
    

    @Required
    public void setVideoStorageRoot(String rootPath) {
        this.videoStorageRoot = rootPath;
    }
    
    @Required
    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }
    
    @Required
    public void setVideoApiClient(VideoApiClient videoapp) {
        this.videoapp = videoapp;
    }

}
