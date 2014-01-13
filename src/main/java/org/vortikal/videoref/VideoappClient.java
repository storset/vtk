/* Copyright (c) 2013,2014 University of Oslo, Norway
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.vortikal.repository.Path;
import org.vortikal.repository.Resource;

/**
 * Quick prototype
 * 
 * TODO exceptions and error handling.
 */
public class VideoappClient implements DisposableBean {
    
    private RestTemplate restTemplate;
    private String repositoryId;
    private URI apiBase;
    
    private final Log logger = LogFactory.getLog(VideoappClient.class);
    
    private final ExecutorService asyncMessageExecutor = Executors.newCachedThreadPool();

    /**
     * Create new video object in videoapp.
     * 
     * @param newResource
     * @param path
     * @param contentType
     * @return 
     */
    public VideoRef createVideo(Resource newResource, String path, String contentType) {
        JSONObject postJson = new JSONObject();
        postJson.element("uploadFilePath", path);

        // New param: "isInterlaced" from client
        // New param: "basedOnExternalVideoId" for changes to video which will require re-encoding
        // TODO: how to supply these through repositry API ? Might need alternative
        // ways of creating videos...
        
        URI newVideoLocation = this.restTemplate.postForLocation(withBaseUri("videos/{host}/"), postJson, this.repositoryId);
        logger.debug("newVideoLocation = " + newVideoLocation);
        
        return getVideo(newVideoLocation).copyBuilder().uploadContentType(contentType).build();
    }
    
    /**
     * Get video info from videoapp.
     * @param id
     * @return 
     */
    public VideoRef getVideo(VideoId id) {
        String location = withBaseUri(id.uri());
        VideoRef ref = getVideo(URI.create(location));
        if (!ref.videoId().equals(id)) {
            throw new RestClientException("Unexpected videoExternalId in response: " + ref.videoId());
        }
        
        return ref;
    }
    
    private VideoRef getVideo(URI location) {
        JSONObject response = this.restTemplate.getForObject(location, JSONObject.class);
        return fromVideoAppVideo(response);
    }
    
    private VideoRef fromVideoAppVideo(JSONObject videoObject) {
        VideoRef.Builder b = VideoRef.newBuilder().videoId(videoObject.getString("videoExternalId"));
        b.status(videoObject.getString("status"));
        b.durationSeconds(videoObject.getDouble("duration"));
        b.streamablePercentComplete(videoObject.getInt("streamablePercentComplete"));
        
        VideoFileRef[] fileRefs = videoFileRefs(videoObject);
        for (VideoFileRef ref: fileRefs) {
            if (ref.isSourceVideoFile()) {
                b.sourceVideo(ref);
            } else {
                b.convertedVideo(ref);
            }
        }
        
        if (videoObject.has("thumbnail")) {
            JSONObject thumbnailObject = videoObject.getJSONObject("thumbnail");
            b.generatedThumbnail(thumbnailObject.getString("encodedThumbnail"), thumbnailObject.getString("mimeType"));
        }
        return b.build();
    }

    private VideoFileRef[] videoFileRefs(JSONObject videoObject) {
        JSONArray a = videoObject.getJSONArray("videoFiles");
        VideoFileRef[] refs = new VideoFileRef[a.size()];
        for (int i=0; i<refs.length; i++) {
            JSONObject videoFileJson = a.getJSONObject(i);
            
            String localPath = videoFileJson.getString("localPath");
            String contentType = videoFileJson.optString("mimeType", null);
            long size = videoFileJson.getLong("size");
            if (size <= 0) {
                size = new File(localPath).length();
            }
            boolean sourceVideo = videoFileJson.getBoolean("isSourceVideo");

            Map<String, Object> metadata = new HashMap<String, Object>();
            metadata.put("width", videoFileJson.optInt("width"));
            metadata.put("height", videoFileJson.optInt("height"));
            metadata.put("acodec", videoFileJson.optString("acodec"));
            metadata.put("vcodec", videoFileJson.optString("vcodec"));
            metadata.put("interlaced", videoFileJson.optBoolean("isInterlaced"));

            refs[i] = new VideoFileRef(contentType, localPath, size, sourceVideo, metadata);
        }
        
        return refs;
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
    private VideoRef refreshVideoRef(VideoRef oldRef, VideoRef newRef) {
        if (!oldRef.videoId().equals(newRef.videoId())) {
            throw new IllegalArgumentException("videoId must be the same in oldRef and newRef");
        }
        
        return oldRef.copyBuilder()
                     .refUpdateTimestamp(new Date())
                     .sourceVideo(newRef.sourceVideo())
                     .convertedVideo(newRef.convertedVideo())
                     .generatedThumbnail(newRef.generatedThumbnail())
                     .status(newRef.status())
                     .streamablePercentComplete(newRef.streamablePercentComplete())
                     .durationSeconds(newRef.durationSeconds()).build();
    }
    
    /**
     * Notify videoapp about download of source stream from Vortex.
     * 
     * <p>This method is non-blocking and returns immediately, and the result
     * of the notification is discarded.
     * 
     * @param vortexUri the Vortex resource through which download of the video occured
     * @param videoId the video id for which source stream has been requested in Vortex.
     */
    public void notifyDownload(Path vortexUri, VideoId videoId) {
        logger.debug("notifyDownload: " + videoId + ", path: " + vortexUri);

        URI requestPath = URI.create(videoId.toString() + "/notifyDownload").normalize();

        JSONObject requestEntity = new JSONObject();
        requestEntity.put("userUrlPath", vortexUri.toString());
                        
        asyncPostForResponseEntity(withBaseUri(requestPath), requestEntity, JSONObject.class);
    }
    
    /**
     * Requesting streaming of video.
     * 
     * @param vortexUri the Vortex resource for which video streaming is requested
     * @param videoId the video object identifier
     * @return a {@link StreamingRef} with info about streams. Note that the
     * validity of such a reference will expire in a certain amount of time.
     */
    public StreamingRef requestStreaming(Path vortexUri, VideoId videoId) {
        // Create token for streaming
        
        URI requestPath = URI.create(videoId.uri().getPath() + "/tokens").normalize();
        JSONObject requestEntity = new JSONObject();
        requestEntity.put("userUrlPath", vortexUri.toString());

        ResponseEntity<JSONObject> responseEntity = 
                this.restTemplate.postForEntity(withBaseUri(requestPath), 
                        requestEntity, JSONObject.class);
        
        JSONObject response = responseEntity.getBody();

        URI hlsStreamUri = URI.create(response.getString("appleHttpUrl"));
        URI hdsStreamUri = URI.create(response.getString("flashHttpUrl"));
        TokenId tokenId = TokenId.fromString(response.getString("tokenExternalId"));
        String tokenValue = response.getString("tokenValue");
        Date tokenExpiry = new Date(new Date().getTime() + response.getInt("tokenExpiry")*1000);
        
        Token token = new Token(tokenId, tokenValue, tokenExpiry, videoId);
        
        return new StreamingRef(token, hlsStreamUri, hdsStreamUri);
    }
    
    private <T> Future<ResponseEntity<T>> asyncGetForResponseEntity(
             final String url, final Class<T> responseType, final Object... urlVars) {
        return asyncMessageExecutor.submit(new Callable<ResponseEntity<T>>(){
            @Override
            public ResponseEntity<T> call() throws Exception {
                return restTemplate.getForEntity(url, responseType, urlVars);
            }
        });
    }
    
    private <T> Future<ResponseEntity<T>> asyncPostForResponseEntity(
            final String url, final Object request, 
            final Class<T> responseType, final Object... urlVars) {
        return asyncMessageExecutor.submit(new Callable<ResponseEntity<T>>(){
            @Override
            public ResponseEntity<T> call() throws Exception {
                return restTemplate.postForEntity(url, request, responseType, urlVars);
            }
        });
    }
    
    /**
     * Prepends API base URI to the rest and returns the result
     * as a string (not encoded). If the rest is an absolute path, any path
     * in the API base URI is replaced.
     * 
     * @param rest The rest of the URL, typically a path. Can contain {}-params for later interpolation.
     * @return complete URI with API base prepended as a string, not encoded.
     */
    private String withBaseUri(String rest) {
        if (rest.startsWith("/")) {
            return apiBase.getScheme() + "://" + apiBase.getHost() 
                    + (apiBase.getPort() != 80 ? ":" + apiBase.getPort() : "") + rest;
        } else {
            String base = apiBase.toString();
            if (!base.endsWith("/")) {
                return base + "/" + rest;
            } else {
                return base + rest;
            }
        }
    }
    
    private String withBaseUri(URI rest) {
        return apiBase.resolve(rest).toString();
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
        this.apiBase = baseUrl;
    }

    @Override
    public void destroy() throws Exception {
        asyncMessageExecutor.shutdown();
    }
}
