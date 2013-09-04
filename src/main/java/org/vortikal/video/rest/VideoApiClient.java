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
import net.sf.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.vortikal.repository.Resource;

/**
 * Quick prototype
 * 
 * No error handling, lots of hard-coding.
 */
public class VideoApiClient {
    
    private RestTemplate restTemplate;
    private String repositoryId;
    
    public VideoRef createVideo(Resource newResource, String inputVideoPath, String inputContentType) {

        JSONObject postJson = new JSONObject();
        postJson.element("path", inputVideoPath);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity request = new HttpEntity(postJson.toString(), headers);

        // Create new video oject
        String response = 
                this.restTemplate.postForObject("http://localhost:8080/videoapp/rest/v0/videos/{host}/", 
                request, String.class, this.repositoryId);
        
        JSONObject responseJson = JSONObject.fromObject(response);
        String numericVideoId = responseJson.getString("id");
        
        // Get info for newly created video object
        response = this.restTemplate.getForObject("http://localhost:8080/videoapp/rest/v0/videos/{host}/{id}", 
                                                      String.class, this.repositoryId, numericVideoId);
        responseJson = JSONObject.fromObject(response);
        String sourcePath = responseJson.getJSONObject("sourceVideoFile").getString("localPath");
        String convPath = responseJson.getJSONObject("conversionVideoFile").getString("localPath");
        
        return VideoRef.newBuilder().videoId(responseJson.getString("videoId"))
                                             .sourceVideo(inputContentType, sourcePath, getFileSize(sourcePath))
                                             .convertedVideo(null, convPath, getFileSize(convPath))
                                             .build();
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
