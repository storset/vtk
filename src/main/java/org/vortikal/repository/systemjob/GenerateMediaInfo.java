package org.vortikal.repository.systemjob;

import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.sf.json.JSONObject;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.util.io.StreamUtil;

public class GenerateMediaInfo {

    private String username;
    private String password;

    private String service;
    private String repositoryDataDirectory;
    private ResourceTypeTree resourceTypeTree;
    private PropertyTypeDefinition thumbnailPropDef;
    private PropertyTypeDefinition posterImagePropDef;

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

    /* Image */
    public void generateImageMetadata(Path path, Resource resource) throws Exception {
        generateImage(resource, generateConnection(path, imageThumbnailParameters), thumbnailPropDef);
        generateMetadata(resource, generateConnection(path, imageMetadataParameters),
                imageMetadataAffectedPropDefPointers);
    }

    /* Video */
    public void generateVideoInfo(Path path, Resource resource) throws Exception {
        generateImage(resource, generateConnection(path, videoThumbnailParameters), thumbnailPropDef);
        generateMetadata(resource, generateConnection(path, videoMetadataParameters),
                videoMetadataAffectedPropDefPointers);
        generateImage(resource, generateConnection(path, videoPosterImageParameters), posterImagePropDef);
    }

    /* Audio */
    public void generateAudioMetadata(Path path, Resource resource) throws Exception {
        generateMetadata(resource, generateConnection(path, audioMetadataParameters),
                audioMetadataAffectedPropDefPointers);
    }

    private URLConnection generateConnection(Path path, Map<String, String> serviceParameters) throws Exception {

        boolean first = true;
        String parameters = "";
        for (Entry<String, String> e : serviceParameters.entrySet()) {
            if (first)
                first = false;
            else
                parameters += "&";
            parameters += e.getKey() + "=" + e.getValue();
        }

        URL url = new URL(service + repositoryDataDirectory + path.toString() + "?" + parameters);
        URLConnection conn = url.openConnection();
        String val = (new StringBuffer(username).append(":").append(password)).toString();
        byte[] base = val.getBytes();
        String authorizationString = "Basic " + new String(new Base64().encode(base));
        conn.setRequestProperty ("Authorization", authorizationString);
        // conn.setConnectTimeout();
        // conn.setReadTimeout();

        return conn;

    }

    private void generateImage(Resource resource, URLConnection conn, PropertyTypeDefinition propDef) throws Exception {

        Property property = propDef.createProperty();
        property.setBinaryValue(StreamUtil.readInputStream(conn.getInputStream()), conn.getContentType());
        resource.addProperty(property);

    }

    private void generateMetadata(Resource resource, URLConnection conn, List<String> metadataAffectedPropDefPointers)
            throws Exception {

        JSONObject json = JSONObject.fromObject(StreamUtil.streamToString(conn.getInputStream()));

        Property property;
        for (String propDefPointer : metadataAffectedPropDefPointers) {
            PropertyTypeDefinition propDef = resourceTypeTree.getPropertyDefinitionByPointer(propDefPointer);
            property = propDef.createProperty();
            property.setIntValue(Integer.parseInt((String) json.get(propDef.getName())));
            resource.addProperty(property);
        }

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
    public void setService(String service) {
        this.service = service;
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

}
