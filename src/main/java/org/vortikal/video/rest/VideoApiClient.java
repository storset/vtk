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
import java.util.Date;
import net.sf.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
    
    public VideoRef createVideo(Resource newResource, String path, String contentType) {

        JSONObject postJson = new JSONObject();
        postJson.element("path", path);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity request = new HttpEntity(postJson.toString(), headers);

        // Create new videoRef object
        // TODO handle errors and in particular error about invalid video
        String response = 
                this.restTemplate.postForObject("http://localhost:8080/videoapp/rest/v0/videos/{host}/", 
                request, String.class, this.repositoryId);
        
        JSONObject responseJson = JSONObject.fromObject(response);
        VideoId videoId = VideoId.fromString(responseJson.getString("id"));
        
        return videoRef(videoId).copyBuilder()
                                .uploadContentType(contentType).build();
    }
    
    public VideoRef videoRef(VideoId id) {

        String response = this.restTemplate.getForObject("http://localhost:8080/videoapp/rest/v0/videos/{host}/{numericId}",
                String.class, id.host(), id.numericId());
        JSONObject responseJson = JSONObject.fromObject(response);
        
        if (!id.equals(VideoId.fromString(responseJson.getString("videoId")))) {
            throw new RestClientException("Unexpected videoId in response"); // XXX
        }
        
        VideoRef.Builder b = VideoRef.newBuilder().videoId(id);
        b.sourceVideo(videoFileRef(responseJson.getJSONObject("sourceVideoFile")));
        b.convertedVideo(videoFileRef(responseJson.getJSONObject("conversionVideoFile")));
        
        return b.build();
    }
    
    private FileRef videoFileRef(JSONObject videoFileJson) {
        String localPath = videoFileJson.getString("localPath");
        String contentType = videoFileJson.optString("contentType", null);
        long size = videoFileJson.getLong("size");
        if (size <= 0) {
            size = getFileSize(localPath);
        }
        return new FileRef(contentType, localPath, size);
    }

    /**
     * Updated certain parts of a video ref that are non-Vortex-specific data
     * from video app. Other parts are left intact.
     * @param oldRef
     * @return a refreshed <code>VideoRef</code> instance.
     */
    public VideoRef refreshVideoRef(VideoRef oldRef) {
        VideoRef newRef = videoRef(oldRef.videoId());
        
        VideoRef.Builder refreshedBuilder = oldRef.copyBuilder();
        
        return refreshedBuilder.refUpdateTimestamp(new Date())
                        .sourceVideo(newRef.sourceVideo())
                        .convertedVideo(newRef.convertedVideo()).build();
    }
    
    private long getFileSize(String localPath) {
        return new File(localPath).length();
    }
    
    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }
    
    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

}
