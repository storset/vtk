/* Copyright (c) 2012, University of Oslo, Norway
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
package org.vortikal.repository.systemjob;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownServiceException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.sf.json.JSONObject;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.SystemChangeContext;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.util.io.StreamUtil;

public class RemoteMetadataProvider implements MediaMetadataProvider {

    private String username;
    private String password;

    private String protocol;
    private String host;
    private int port;
    private int thumbnailSize;
    private String repositoryDataDirectory;
    private ResourceTypeTree resourceTypeTree;
    private PropertyTypeDefinition thumbnailPropDef;
    private PropertyTypeDefinition posterImagePropDef;
    private PropertyTypeDefinition mediaWidthPropDef;
    private PropertyTypeDefinition mediaHeightPropDef;
    private PropertyTypeDefinition mediaMetadataStatusPropDef;

    private boolean scaleUp = false;
    private long maxSourceImageFileSize = 35000000;
    private long maxSourceImageRawMemoryUsage = 100000000;

    /* Image */
    private Map<String, String> imageThumbnailParameters;
    private Map<String, String> imageMetadataParameters;
    private List<String> imageMetadataAffectedPropDefPointers;

    /* Video */
    private Map<String, String> videoThumbnailParameters;
    private Map<String, String> videoMetadataParameters;
    private Map<String, String> videoPosterImageParameters;
    private List<String> videoMetadataAffectedPropDefPointers;

    /* Audio */
    private Map<String, String> audioMetadataParameters;
    private List<String> audioMetadataAffectedPropDefPointers;

    private final Log logger = LogFactory.getLog(getClass());

    @Override
    public void generateMetadata(final Repository repository, final SystemChangeContext context, String token,
            Path path, Resource resource) throws Exception {
        boolean remove = false;

        try {
            if (resource.getResourceType().equals("image")) {
                remove = generateImageMetadata(repository, context, token, path, resource);
            } else if (resource.getResourceType().equals("video")) {
                remove = generateVideoInfo(repository, context, token, path, resource);
            } else if (resource.getResourceType().equals("audio")) {
                remove = generateAudioMetadata(repository, context, token, path, resource);
            }
        } catch (UnknownServiceException use) {
            // If the protocol does not support output.
            logger.warn("UnknownServiceException: " + use.getMessage());
        } catch (IOException ioe) {
            // If an I/O error occurs while creating the output stream
            // or opening connection.
            logger.warn("IOException: " + ioe.getMessage());
        } catch (StatusCodeException sce) {
            logger.warn("StatusCodeException: " + sce.getMessage());
        }

        if (remove)
            removeMediaMetadataStatus(repository, context, token, resource);
    }

    /* Image */
    public boolean generateImageMetadata(final Repository repository, final SystemChangeContext context, String token,
            Path path, Resource resource) throws Exception {

        URLConnection conn = generateConnection(repository, context, token, resource, path, imageMetadataParameters);
        Map<String, Integer> ret = generateMetadata(repository, context, token, resource, conn,
                imageMetadataAffectedPropDefPointers);

        int width = ret.get(mediaWidthPropDef.getName());
        int height = ret.get(mediaHeightPropDef.getName());

        if (estimateMemoryUsage(height, width) > maxSourceImageRawMemoryUsage) {
            logger.info("Estimated memory usage of image exceeds limit: " + path);
            setMediaMetadataStatus(repository, context, token, resource, "MEMORY_USAGE_EXCEEDS_LIMIT");
            return false;
        }

        // Check max source content length constraint
        if (resource.getContentLength() >= maxSourceImageFileSize) {
            logger.info("Image size exceeds maximum limit: " + path);
            setMediaMetadataStatus(repository, context, token, resource, "IMAGE_SIZE_EXCEEDS_LIMIT");
            return false;
        }

        if (scaleUp || (thumbnailSize < width)) {
            conn = generateConnection(repository, context, token, resource, path, imageThumbnailParameters);
            generateImage(repository, context, token, resource, conn, thumbnailPropDef);
        } else {
            logger.info("Image is smaller than or equal to thumbnail size. Not scaling up: " + path);
            setMediaMetadataStatus(repository, context, token, resource, "CONFIGURED_NOT_TO_SCALE_UP");
            return false;
        }

        return true;
    }

    /* Video */
    public boolean generateVideoInfo(final Repository repository, final SystemChangeContext context, String token,
            Path path, Resource resource) throws Exception {
        boolean remove = true;

        URLConnection conn = generateConnection(repository, context, token, resource, path, videoMetadataParameters);
        Map<String, Integer> ret = generateMetadata(repository, context, token, resource, conn,
                videoMetadataAffectedPropDefPointers);

        int width = ret.get(mediaWidthPropDef.getName());
        int height = ret.get(mediaHeightPropDef.getName());

        if (estimateMemoryUsage(height, width) > maxSourceImageRawMemoryUsage) {
            logger.info("Estimated memory usage of image extraction exceeds limit: " + path);
            setMediaMetadataStatus(repository, context, token, resource, "MEMORY_USAGE_EXCEEDS_LIMIT");
            return false;
        }

        if (scaleUp || (thumbnailSize < width)) {
            conn = generateConnection(repository, context, token, resource, path, videoThumbnailParameters);
            generateImage(repository, context, token, resource, conn, thumbnailPropDef);
        } else {
            logger.info("Extracted image is smaller than or equal to thumbnail size. Not scaling up.: " + path);
            setMediaMetadataStatus(repository, context, token, resource, "CONFIGURED_NOT_TO_SCALE_UP");
            remove = false;
        }
        conn = generateConnection(repository, context, token, resource, path, videoPosterImageParameters);
        generateImage(repository, context, token, resource, conn, posterImagePropDef);

        return remove;
    }

    /* Audio */
    public boolean generateAudioMetadata(final Repository repository, final SystemChangeContext context, String token,
            Path path, Resource resource) throws Exception {
        URLConnection conn = generateConnection(repository, context, token, resource, path, audioMetadataParameters);
        generateMetadata(repository, context, token, resource, conn, audioMetadataAffectedPropDefPointers);

        return true;
    }

    private URLConnection generateConnection(final Repository repository, final SystemChangeContext context,
            String token, Resource resource, Path path, Map<String, String> serviceParameters) throws Exception {

        boolean first = true;
        String parameters = "";
        for (Entry<String, String> e : serviceParameters.entrySet()) {
            if (first)
                first = false;
            else
                parameters += "&";
            parameters += e.getKey() + "=" + e.getValue();
        }

        URL url = new URI(protocol, null, host, port, repositoryDataDirectory + path.toString(), parameters, null)
                .toURL();
        URLConnection conn = url.openConnection();
        String val = (new StringBuffer(username).append(":").append(password)).toString();
        byte[] base = val.getBytes();
        String authorizationString = "Basic " + new String(new Base64().encode(base));
        conn.setRequestProperty("Authorization", authorizationString);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);
        conn.connect();

        int responseCode = ((HttpURLConnection) conn).getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
            setMediaMetadataStatus(repository, context, token, resource, "UNSUPPORTED_FORMAT");
            throw new StatusCodeException("Media format for request " + url.toExternalForm()
                    + " is not supported. Status code: " + responseCode);
        } else if ((responseCode == HttpURLConnection.HTTP_UNSUPPORTED_TYPE)
                || (responseCode == HttpURLConnection.HTTP_INTERNAL_ERROR)
                || (responseCode == HttpURLConnection.HTTP_NOT_FOUND)) {
            setMediaMetadataStatus(repository, context, token, resource, "CORRUPT");
            throw new StatusCodeException("Service returned error when requesting " + url.toExternalForm()
                    + ". Status code: " + responseCode);
        }

        return conn;

    }

    private void generateImage(final Repository repository, final SystemChangeContext context, String token,
            Resource resource, URLConnection conn, PropertyTypeDefinition propDef) throws Exception {

        if (!conn.getContentType().equals("image/jpeg")) {
            setMediaMetadataStatus(repository, context, token, resource, "CORRUPT");
            throw new Exception("Content type of URLConnection is not of image/jpeg.");
        }

        Property property = propDef.createProperty();
        property.setBinaryValue(StreamUtil.readInputStream(conn.getInputStream()), conn.getContentType());
        resource.addProperty(property);

    }

    private Map<String, Integer> generateMetadata(final Repository repository, final SystemChangeContext context,
            String token, Resource resource, URLConnection conn, List<String> metadataAffectedPropDefPointers)
            throws Exception {

        if (!conn.getContentType().equals("application/json")) {
            setMediaMetadataStatus(repository, context, token, resource, "CORRUPT");
            throw new Exception("Content type of URLConnection is not of application/json.");
        }

        JSONObject json = JSONObject.fromObject(StreamUtil.streamToString(conn.getInputStream()));

        int value;
        Property property;
        Map<String, Integer> ret = new HashMap<String, Integer>();
        for (String propDefPointer : metadataAffectedPropDefPointers) {
            PropertyTypeDefinition propDef = resourceTypeTree.getPropertyDefinitionByPointer(propDefPointer);
            value = Integer.parseInt((String) json.get(propDef.getName()));
            ret.put(propDefPointer, value);
            property = propDef.createProperty();
            property.setIntValue(value);
            resource.addProperty(property);
        }

        return ret;
    }

    private void setMediaMetadataStatus(final Repository repository, final SystemChangeContext context,
            final String token, Resource resource, String status) {
        Property statusProp = mediaMetadataStatusPropDef.createProperty();
        statusProp.setValue(new Value(status, org.vortikal.repository.resourcetype.PropertyType.Type.STRING));
        resource.addProperty(statusProp);
        try {
            repository.store(token, resource, context);
        } catch (Exception e) {
            e.printStackTrace();
            // Resource currently locked or moved.. try again in next
            // batch
        }
    }

    private void removeMediaMetadataStatus(final Repository repository, final SystemChangeContext context,
            final String token, Resource resource) throws Exception {
        if (resource.getLock() == null) {
            resource.removeProperty(mediaMetadataStatusPropDef);
            repository.store(token, resource, context);
            logger.info("Created metadata for " + resource);
        } else {
            logger.warn("Resource " + resource + " currently locked, will not invoke store.");
        }
    }

    /**
     * Estimates the raw memory usage for an image where each pixel uses 24 bits
     * or 3 bytes of memory.
     * 
     * @param dim
     *            The <code>Dimension</code> of the image.
     * @return The estimated raw memory usage in bytes.
     */
    private long estimateMemoryUsage(int height, int width) {
        return (long) height * (long) width * 24 / 8;
    }

    @Required
    public void setUsername(String username) {
        this.username = username;
    }

    @Required
    public void setPassword(String password) {
        this.password = password;
    }

    @Required
    public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
    }

    @Required
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @Required
    public void setHost(String host) {
        this.host = host;
    }

    @Required
    public void setPort(int port) {
        this.port = port;
    }

    @Required
    public void setThumbnailSize(int thumbnailSize) {
        this.thumbnailSize = thumbnailSize;
    }

    @Required
    public void setRepositoryDataDirectory(String repositoryDataDirectory) {
        this.repositoryDataDirectory = repositoryDataDirectory;
    }

    @Required
    public void setThumbnailPropDef(PropertyTypeDefinition thumbnailPropDef) {
        this.thumbnailPropDef = thumbnailPropDef;
    }

    @Required
    public void setPosterImagePropDef(PropertyTypeDefinition posterImagePropDef) {
        this.posterImagePropDef = posterImagePropDef;
    }

    @Required
    public void setMediaWidthPropDef(PropertyTypeDefinition mediaWidthPropDef) {
        this.mediaWidthPropDef = mediaWidthPropDef;
    }

    @Required
    public void setMediaHeightPropDef(PropertyTypeDefinition mediaHeightPropDef) {
        this.mediaHeightPropDef = mediaHeightPropDef;
    }

    @Required
    public void setMediaMetadataStatusPropDef(PropertyTypeDefinition mediaMetadataStatusPropDef) {
        this.mediaMetadataStatusPropDef = mediaMetadataStatusPropDef;
    }

    /* Image */
    @Required
    public void setImageThumbnailParameters(Map<String, String> imageThumbnailParameters) {
        this.imageThumbnailParameters = imageThumbnailParameters;
    }

    @Required
    public void setImageMetadataParameters(Map<String, String> imageMetadataParameters) {
        this.imageMetadataParameters = imageMetadataParameters;
    }

    @Required
    public void setImageMetadataAffectedPropDefPointers(List<String> imageMetadataAffectedPropDefPointers) {
        this.imageMetadataAffectedPropDefPointers = imageMetadataAffectedPropDefPointers;
    }

    /* Video */
    @Required
    public void setVideoThumbnailParameters(Map<String, String> videoThumbnailParameters) {
        this.videoThumbnailParameters = videoThumbnailParameters;
    }

    @Required
    public void setVideoMetadataParameters(Map<String, String> videoMetadataParameters) {
        this.videoMetadataParameters = videoMetadataParameters;
    }

    @Required
    public void setVideoPosterImageParameters(Map<String, String> videoPosterImageParameters) {
        this.videoPosterImageParameters = videoPosterImageParameters;
    }

    @Required
    public void setVideoMetadataAffectedPropDefPointers(List<String> videoMetadataAffectedPropDefPointers) {
        this.videoMetadataAffectedPropDefPointers = videoMetadataAffectedPropDefPointers;
    }

    /* Audio */
    @Required
    public void setAudioMetadataParameters(Map<String, String> audioMetadataParameters) {
        this.audioMetadataParameters = audioMetadataParameters;
    }

    @Required
    public void setAudioMetadataAffectedPropDefPointers(List<String> audioMetadataAffectedPropDefPointers) {
        this.audioMetadataAffectedPropDefPointers = audioMetadataAffectedPropDefPointers;
    }

    public void setScaleUp(boolean scaleUp) {
        this.scaleUp = scaleUp;
    }

    public void setMaxSourceImageFileSize(long maxSourceImageFileSize) {
        if (maxSourceImageFileSize < 1) {
            throw new IllegalArgumentException("maxSourceImageFileSize must be >= 1");
        }
        this.maxSourceImageFileSize = maxSourceImageFileSize;
    }

    /**
     * Set cap on estimated raw memory usage on image during scale operation.
     * The estimate is based upon a memory usage of 24 bits per pixel, which
     * should be the most common type. 32bpp images will consume more than
     * actual estimate. To fix that, one needs to provide the bpp value from the
     * {@link org.vortikal.repository.content.ImageContentFactory}.
     * 
     * Default value of 100MB is roughly equivalent to an image of about 33
     * megapixels.
     * 
     * @param maxSourceImageRawMemoryUsage
     */
    public void setMaxSourceImageRawMemoryUsage(long maxSourceImageRawMemoryUsage) {
        if (maxSourceImageRawMemoryUsage < 1) {
            throw new IllegalArgumentException("maxSourceImageRawMemoryUsage must be >= 1");
        }
        this.maxSourceImageRawMemoryUsage = maxSourceImageRawMemoryUsage;
    }

    private static class StatusCodeException extends Exception {
        private static final long serialVersionUID = 1L;

        public StatusCodeException(String msg) {
            super(msg);
        }
    }

}
