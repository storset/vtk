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
package org.vortikal.videoref;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.ContentStream;
import org.vortikal.repository.InheritablePropertiesStoreContext;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.NoSuchContentException;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.ResourceImpl;
import org.vortikal.repository.SystemChangeContext;
import org.vortikal.repository.hooks.DefaultTypeHanderHooks;
import org.vortikal.repository.resourcetype.BufferedBinaryValue;
import org.vortikal.repository.resourcetype.Content;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.store.ContentStore;
import org.vortikal.util.io.StreamUtil;
import org.vortikal.util.io.StreamUtil.TempFile;

/**
 * Type handler extension which allows for specialized handling of video content
 * and control over how resource shall be stored in regular repository.
 */
public class VideoContentHooks extends DefaultTypeHanderHooks
        implements InitializingBean {

    private static final String VIDEO_INPUT_DIRNAME = "videoinput";

    private String videoStorageRoot;
    private VideoappClient videoapp;
    private String repositoryId;

    private PropertyTypeDefinition thumbnailPropDef;
    private PropertyTypeDefinition mediaMetadataStatusPropDef;
    private PropertyTypeDefinition mediaHeightPropDef;
    private PropertyTypeDefinition mediaWidthPropDef;
    private PropertyTypeDefinition mediaDurationPropDef;
    
    private final Log logger = LogFactory.getLog(VideoContentHooks.class);

    @Override
    public boolean handleCreateForContent(String contentType) {
        return contentType.startsWith("video/");
    }

    @Override
    public boolean handleResourceType(String resourceType) {
        return "videoref".equals(resourceType);
    }

    @Override
    public boolean handleCreateCollection() {
        return false;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
    
    @Override
    public ResourceImpl storeContentOnCreate(ResourceImpl resource, InputStream stream,
            String contentType)
            throws Exception {

        return storeContent(resource, stream, contentType);
    }

    @Override
    public ContentStream onGetAlternativeContentStream(ResourceImpl resource, String contentIdentifier)
            throws NoSuchContentException, Exception {

        if ("application/json".equals(contentIdentifier)) {
            long length = getContentStore().getContentLength(resource.getURI());
            InputStream stream = getContentStore().getInputStream(resource.getURI());
            return new ContentStream(stream, length);
        }

        throw new NoSuchContentException("No such alternative content :"
                + contentIdentifier + " for resource at " + resource.getURI());
    }

    @Override
    public ResourceImpl storeContent(ResourceImpl resource, InputStream stream, String contentType)
            throws IOException {

        // Dump input stream to file in video storage input area
        String name = resource.getURI().getName();
        // XXX consuming entire input stream, but need a way to revert if this all fails (fallback to regular store in repo).
        TempFile tempFile = StreamUtil.streamToTempFile(stream, -1, new File(getInputDirAbspath()), name);

        // Make a videoapp call to create a new video object
        VideoRef newVideoRef = videoapp.createVideo(resource, tempFile.getFile().getAbsolutePath(), contentType);
        tempFile.delete();

        // Store new video object ref on resource
        VideoRef ref = newVideoRef.copyBuilder().uploadContentType(contentType)
                          .refUpdateTimestamp(new Date()).build();

        storeVideoRef(ref, resource.getURI(), getContentStore());

        updateMediaProperties(ref, resource);

        return resource;
    }
    

    @Override
    public ResourceImpl onStore(ResourceImpl resource)
            throws IOException {

        VideoRef ref = loadVideoRef(resource.getURI(), getContentStore());

        // Try updating videoref metadata from videoapp
        try {
            ref = videoapp.refreshFromVideoapp(ref);
            storeVideoRef(ref, resource.getURI(), getContentStore());
        } catch (Exception e) {
            // Don't fail entire store just because videoapp is down
            logger.warn("Failed to refresh video metadata for " + resource.getURI() + ": " 
                    + e.getClass().getSimpleName() + ":" + e.getMessage());
        }

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
        
        updateMediaProperties(ref, resource);

        return resource;
    }

    @Override
    public ResourceImpl onStoreSystemChange(ResourceImpl resource, SystemChangeContext context)
            throws IOException {
        return onStore(resource);
    }

    @Override
    public ResourceImpl onStoreInheritableProps(ResourceImpl resource, InheritablePropertiesStoreContext context)
            throws IOException {
        return onStore(resource);
    }

    @Override
    public InputStream getInputStream(ResourceImpl resource) throws IOException {

        VideoRef ref = loadVideoRef(resource.getURI(), getContentStore());

        if (ref.sourceVideo() != null) {
            videoapp.notifyDownload(ref.videoId());
            return new FileInputStream(new File(ref.sourceVideo().path()));
        }

        throw new IOException("Stream not found");
    }

    @Override
    public Content getContentForEvaluation(ResourceImpl resource, Content defaultContent) throws IOException {
        return new VideoRefContent(resource, defaultContent, getContentStore());
    }

    // Adds media mixin props to resource based on data in video ref
    private void updateMediaProperties(VideoRef ref, ResourceImpl resource) {
        Property prop = mediaDurationPropDef.createProperty();
        prop.setIntValue((int)ref.durationSeconds());
        resource.addProperty(prop);
        
        if (ref.sourceVideo() != null) {
            VideoFileRef sourceVideo = ref.sourceVideo();
            Object value = sourceVideo.metadata().get("width");
            if (value != null && value.getClass() == Integer.class) {
                prop = mediaWidthPropDef.createProperty();
                prop.setIntValue(((Integer)value).intValue());
                resource.addProperty(prop);
            }

            value = sourceVideo.metadata().get("height");
            if (value != null && value.getClass() == Integer.class) {
                prop = mediaHeightPropDef.createProperty();
                prop.setIntValue(((Integer)value).intValue());
                resource.addProperty(prop);
            }
        }

        if (ref.hasGeneratedThumbnail()) {
            BufferedBinaryValue b = ref.generatedThumbnail();
            prop = thumbnailPropDef.createProperty();
            prop.setBinaryValue(b.getBytes(), b.getContentType());
            resource.addProperty(prop);
        }
        
        // Metadata status set according to video status
        // TODO need to formalize which video states in videoapp can be considered final
        //      with no need for further refreshing.
        // Trenger task scheduler i videoapp (for opprydding).
        
        if ("completed".equals(ref.status()) || "not-converted".equals(ref.status())) {
            resource.removeProperty(mediaMetadataStatusPropDef);
        } else {
            prop = mediaMetadataStatusPropDef.createProperty();
            prop.setStringValue("GENERATE");
            resource.addProperty(prop);
        }
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
        return videoStorageRoot + "/" + VIDEO_INPUT_DIRNAME + "/" + repositoryId;
    }

    @Override
    public void afterPropertiesSet() throws IOException {
        File rootDir = new File(videoStorageRoot);
        if (!rootDir.isDirectory()) {
            throw new IOException("Not a directory: " + videoStorageRoot);
        }
        if (!rootDir.isAbsolute()) {
            throw new IOException("Not an absolute path: " + videoStorageRoot);
        }

        File inputDir = new File(getInputDirAbspath());
        if (!inputDir.exists()) {
            inputDir.mkdirs();
            logger.info("Created video input directory: " + inputDir);
        }
    }

    @Required
    public void setVideoStorageRoot(String rootPath) {
        this.videoStorageRoot = rootPath;
    }

    @Required
    public void setVideoApiClient(VideoappClient videoapp) {
        this.videoapp = videoapp;
    }

    @Required
    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    /**
     * @param mediaMetadataStatusPropDef the mediaMetadataStatusPropDef to set
     */
    @Required
    public void setMediaMetadataStatusPropDef(PropertyTypeDefinition mediaMetadataStatusPropDef) {
        this.mediaMetadataStatusPropDef = mediaMetadataStatusPropDef;
    }

    /**
     * @param mediaHeightPropDef the mediaHeightPropDef to set
     */
    @Required
    public void setMediaHeightPropDef(PropertyTypeDefinition mediaHeightPropDef) {
        this.mediaHeightPropDef = mediaHeightPropDef;
    }

    /**
     * @param mediaWidthPropDef the mediaWidthPropDef to set
     */
    @Required
    public void setMediaWidthPropDef(PropertyTypeDefinition mediaWidthPropDef) {
        this.mediaWidthPropDef = mediaWidthPropDef;
    }

    /**
     * @param mediaDurationPropDef the mediaDurationPropDef to set
     */
    @Required
    public void setMediaDurationPropDef(PropertyTypeDefinition mediaDurationPropDef) {
        this.mediaDurationPropDef = mediaDurationPropDef;
    }

    /**
     * @param thumbnailPropDef the thumbnailPropDef to set
     */
    @Required
    public void setThumbnailPropDef(PropertyTypeDefinition thumbnailPropDef) {
        this.thumbnailPropDef = thumbnailPropDef;
    }

}
