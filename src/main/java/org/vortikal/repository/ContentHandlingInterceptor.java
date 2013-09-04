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
import net.sf.json.JSONObject;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.store.ContentStore;
import org.vortikal.util.io.StreamUtil;
import org.vortikal.util.io.StreamUtil.TempFile;
import org.vortikal.video.rest.VideoApiClient;
import org.vortikal.video.rest.VideoRef;

/**
 * TODO Rename and pull out interface, if we decide for this approach.
 */
public class ContentHandlingInterceptor implements InitializingBean {
    
    private String videoStorageRoot;
    private String repositoryId;
    private VideoApiClient videoapp;
    private PropertyTypeDefinition contentTypePropDef;
    
    public boolean isSupportedContentType(String contentType) {
        return contentType != null && ("video".equals(contentType) || contentType.startsWith("video/"));
    }
    
    public boolean isSupportedResourceType(String resourceType) {
        return "videoref".equals(resourceType);
    }
    
    private String getInputDirAbspath() {
        return this.videoStorageRoot + "/input/" + this.repositoryId;
    }

    /**
     * Called by {@link RepositoryImpl#createDocument(java.lang.String, org.vortikal.repository.Path, java.io.InputStream) }
     * if input has the right content type.
     */
    public ResourceImpl interceptCreate(ResourceImpl newResource,
            InputStream stream, String contentType, ContentStore contentStore) 
            throws IOException {

        // Dump input stream to file in video storage input area
        String name = newResource.getURI().getName();
        TempFile tempFile = StreamUtil.streamToTempFile(stream, -1, new File(getInputDirAbspath()), name);

        // Make a videoapp API call to create new video
        VideoRef ref = this.videoapp.createVideo(newResource, tempFile.getFile().getAbsolutePath(), contentType);
        
        if (tempFile.getFile().exists()) {
            System.out.println("Warning: expected temp file to be moved to another location, but it still exists.  API might have failed.");
        }
        
        // Store videoref info JSON as resource main content
        contentStore.storeContent(newResource.getURI(), 
                StreamUtil.stringToStream(ref.toJson().toString(2), "utf-8"));

        Property contentTypeProp = this.contentTypePropDef.createProperty("application/json");
        newResource.addProperty(contentTypeProp);
        // Return new resource for further type evaluation in repository
        return newResource;
    }
    
    public ResourceImpl interceptStore(ResourceImpl resource, InputStream stream, String contentType, ContentStore store) 
            throws IOException{
        
        throw new UnsupportedOperationException();
    }
    
    public InputStream getInputStream(ResourceImpl resource, String contentType, ContentStore store) 
            throws IOException {
        
        // XXX only one alternative content-type, ignore supplied contentType
        
        JSONObject videoRefJson = JSONObject.fromObject(
                                    StreamUtil.streamToString(
                                        store.getInputStream(resource.getURI()), "utf-8"));
        
        VideoRef ref = VideoRef.newBuilder().fromJson(videoRefJson).build();
        
        if (ref.getSourceVideoFileRef() != null) {
            return new FileInputStream(new File(ref.getSourceVideoFileRef().getPath()));
        }

        throw new IOException("Stream not found");
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

    @Required
    public void setContentTypePropDef(PropertyTypeDefinition def) {
        this.contentTypePropDef = def;
    }
}
