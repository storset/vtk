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
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import net.sf.json.JSONObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.vortikal.repository.Resource;

/**
 * Quick prototype
 * 
 * TODO exceptions and error handling.
 */
public class VideoappClient {
    
    private RestTemplate restTemplate;
    private String repositoryId;
    private String apiBaseUrl;
    
    private final Log logger = LogFactory.getLog(VideoappClient.class);

    /**
     * Create new video object in videoapp.
     * @param newResource
     * @param path
     * @param contentType
     * @return 
     */
    public VideoRef createVideo(Resource newResource, String path, String contentType) {
        JSONObject postJson = new JSONObject();
        postJson.element("path", path);
        
        URI newVideoLocation = this.restTemplate.postForLocation(withBaseUrl("/videos/{host}/"), postJson, this.repositoryId);
        logger.debug("newVideoLocation = " + newVideoLocation);
        
        return getVideo(newVideoLocation).copyBuilder().uploadContentType(contentType).build();
    }
    
    /**
     * Get video info from videoapp.
     * @param id
     * @return 
     */
    public VideoRef getVideo(VideoId id) {
        JSONObject response = this.restTemplate.getForObject(
                withBaseUrl("/videos/{host}/{numericId}"),
                JSONObject.class, id.host(), id.numericId());
        
        VideoRef ref = fromVideoAppVideo(response);
        if (!ref.videoId().equals(id)) {
            throw new RestClientException("Unexpected videoId in response: " + ref.videoId());
        }
        
        return ref;
    }
    
    private VideoRef getVideo(URI location) {
        JSONObject response = this.restTemplate.getForObject(location, JSONObject.class);
        return fromVideoAppVideo(response);
    }
    
    private VideoRef fromVideoAppVideo(JSONObject videoObject) {
        VideoRef.Builder b = VideoRef.newBuilder().videoId(videoObject.getString("videoId"));
        b.status(videoObject.getString("status"));
        b.durationSeconds(videoObject.getDouble("duration"));
        b.sourceVideo(videoFileRef(videoObject.getJSONObject("sourceVideoFile")));
        if (videoObject.has("convertedVideoFile") && !videoObject.getJSONObject("convertedVideoFile").isNullObject()) {
            b.convertedVideo(videoFileRef(videoObject.getJSONObject("convertedVideoFile")));
        }
        if (videoObject.has("thumbnailGenerated") && videoObject.has("thumbnailGeneratedMimeType")) {
            b.generatedThumbnail(videoObject.getString("thumbnailGenerated"), videoObject.getString("thumbnailGeneratedMimeType"));
        }
        return b.build();
    }

    private VideoFileRef videoFileRef(JSONObject videoFileJson) {
        String localPath = videoFileJson.getString("localPath");
        String contentType = videoFileJson.optString("mimeType", null);
        long size = videoFileJson.getLong("size");
        if (size <= 0) {
            size = new File(localPath).length();
        }
        
        Map<String,Object> metadata = new HashMap<String,Object>();
        metadata.put("width", videoFileJson.optInt("width"));
        metadata.put("height", videoFileJson.optInt("height"));
        metadata.put("acodec", videoFileJson.optString("acodec"));
        metadata.put("vcodec", videoFileJson.optString("vcodec"));
        
        return new VideoFileRef(contentType, localPath, size, metadata);
    }
    
    /**
     * Updated certain parts of a video ref that are non-Vortex-specific data
     * from video app. This method will fetch updated data about the video
     * from the videoapp and apply that to a refreshed reference.
     * 
     * @see #refreshVideoRef(org.vortikal.videoref.VideoRef, org.vortikal.videoref.VideoRef) 
     * @param oldRef the old referenced to be updated
     * @return a refreshed <code>VideoRef</code> instance with updated data
     * from videoapp.
     */
    public VideoRef refreshFromVideoapp(VideoRef oldRef) {
        VideoRef newRef = getVideo(oldRef.videoId());
        return refreshVideoRef(oldRef, newRef);
    }
    
    /**
     * Refresh data in <code>oldRef</code> with data in <code>newRef</code>
     * that are non-Vortex-specific from videoapp. The video id in both
     * references must be the same.
     * 
     * @param oldRef the old reference
     * @param newRef the new reference 
     * @return a new video reference based on <code>oldRef</code>, but with
     *         refreshed videoapp-data from <code>newRef</code>.
     */
    public VideoRef refreshVideoRef(VideoRef oldRef, VideoRef newRef) {
        if (!oldRef.videoId().equals(newRef.videoId())) {
            throw new IllegalArgumentException("videoId must be the same in oldRef and newRef");
        }
        
        return oldRef.copyBuilder().refUpdateTimestamp(new Date())
                        .sourceVideo(newRef.sourceVideo())
                        .convertedVideo(newRef.convertedVideo())
                        .status(newRef.status())
                        .durationSeconds(newRef.durationSeconds())
                        .generatedThumbnail(newRef.generatedThumbnail()).build();
    }
    
    /**
     * Requesting streaming of video.
     * 
     * @param videoId the video object identifier to 
     * @return a {@link StreamingRef} with info about streams. Note that the
     * validity of such a reference will expire in a certain amount of time.
     */
    public StreamingRef requestStreaming(VideoId videoId) {
        // TODO @videoapp, give Location of newly created object and use that here
        // Create token for streaming

        ResponseEntity<JSONObject> responseEntity = 
                this.restTemplate.postForEntity(withBaseUrl("/videos/{host}/{videoId}/tokens/?return-stream-uris=true"), 
                        new JSONObject(), JSONObject.class, repositoryId, videoId.numericId());
        
        final URI location = responseEntity.getHeaders().getLocation();
        JSONObject response = responseEntity.getBody();

        // TODO: these should not be part of 201 Created response, but instead part of GET on token object or something
        final String hlsStreamUri = response.getString("appleHttp");
        final String hdsStreamUri = response.getString("flashHttp");
        
        final TokenId tokenId = TokenId.fromString(response.getString("tokenId"));
        
        // Get newly created token object
        response = this.restTemplate.getForObject(location, JSONObject.class);
        
        if (! TokenId.fromString(response.getString("tokenId")).equals(tokenId)) {
            throw new RestClientException("Unexpected token id in response");
        }
        if (! VideoId.fromString(response.getString("videoRef")).equals(videoId)) {
            throw new RestClientException("Unexpected video id in response");
        }
        
        final String tokenValue = response.getString("tokenValue");
        
        // TODO do not hard code 60s expiry time here, get value from videoapp instead
        Token token = new Token(tokenId, tokenValue, new Date(new Date().getTime() + 60000), videoId);
        
        return new StreamingRef(token, URI.create(hlsStreamUri), URI.create(hdsStreamUri));
    }
    
    /**
     * Prepends API base URL to the rest and returns the result
     * as a string.
     * @param rest The rest of the URL.
     * @return complete URL with API base URL prepended.
     */
    private String withBaseUrl(String rest) {
        
        if (! (this.apiBaseUrl.endsWith("/") 
                || rest.startsWith("/"))) {
            rest = "/" + rest;
        } else if (this.apiBaseUrl.endsWith("/") && rest.startsWith("/")) {
            rest = rest.substring(1);
        }
        
        return this.apiBaseUrl + rest;
    }
    
    @Required
    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    @Required
    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Required
    public void setApiBaseUrl(URI baseUrl) {
        this.apiBaseUrl = baseUrl.toString();
    }
}
