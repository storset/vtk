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

package org.vortikal.video.rest;

import java.io.File;
import java.net.URI;
import java.util.Date;
import net.sf.json.JSONObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.vortikal.repository.Resource;

/**
 * Quick prototype
 * 
 * TODO exceptions and error handling.
 */
public class VideoApiClient {
    
    private RestTemplate restTemplate;
    private String repositoryId;
    private String apiBaseUrl;
    
    private Log logger = LogFactory.getLog(VideoApiClient.class);

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
    
    private VideoRef fromVideoAppVideo(JSONObject video) {
        VideoRef.Builder b = VideoRef.newBuilder().videoId(video.getString("videoId"));
        b.sourceVideo(videoFileRef(video.getJSONObject("sourceVideoFile")));
        if (video.has("convertedVideoFile") && !video.getJSONObject("convertedVideoFile").isNullObject()) {
            b.convertedVideo(videoFileRef(video.getJSONObject("convertedVideoFile")));
        }
        return b.build();
    }

    /**
     * Updated certain parts of a video ref that are non-Vortex-specific data
     * from video app. Other parts are left intact.
     * 
     * @param oldRef
     * @return a refreshed <code>VideoRef</code> instance.
     */
    public VideoRef refreshVideo(VideoRef oldRef) {
        VideoRef newRef = getVideo(oldRef.videoId());
        
        VideoRef.Builder refreshedBuilder = oldRef.copyBuilder();
        
        return refreshedBuilder.refUpdateTimestamp(new Date())
                        .sourceVideo(newRef.sourceVideo())
                        .convertedVideo(newRef.convertedVideo()).build();
    }
    
    private FileRef videoFileRef(JSONObject videoFileJson) {
        String localPath = videoFileJson.getString("localPath");
        String contentType = videoFileJson.optString("mimeType", null);
        long size = videoFileJson.getLong("size");
        if (size <= 0) {
            size = new File(localPath).length();
        }
        return new FileRef(contentType, localPath, size);
    }
    
    /**
     * Prepends API base URL to the rest and returns the result
     * as a string.
     * @param rest The rest of the URL.
     * @return complete URL with API base URL prepended.
     */
    private String withBaseUrl(String rest) {
        if (this.apiBaseUrl.endsWith("/") 
                && rest.startsWith("/")) {
            rest = rest.substring(1);
        } else if (! (this.apiBaseUrl.endsWith("/")
                || rest.startsWith("/"))) {
            rest = "/" + rest;
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
