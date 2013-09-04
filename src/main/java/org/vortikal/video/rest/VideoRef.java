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

import net.sf.json.JSONNull;
import net.sf.json.JSONObject;

/**
 * 
 */
public class VideoRef {
    
    private String videoId;
    private MediaFileRef sourceVideoFileRef;
    private MediaFileRef convertedVideoFileRef;
    
    private VideoRef(Builder builder) {
        this.videoId = builder.videoId;
        this.sourceVideoFileRef = builder.sourceVideoFileRef;
        this.convertedVideoFileRef = builder.convertedVideoFileRef;
    }
   
    /**
     * Whatever. Consider auto-serialization instead.
     */
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.element("mediaref", true);
        json.element("resourcetype", "videoref");
        
        JSONObject refJson = new JSONObject();
        refJson.element("videoId", this.videoId);

        JSONObject sourceJson = new JSONObject();
        if (this.sourceVideoFileRef != null) {
            sourceJson.element("contentType", this.sourceVideoFileRef.getContentType() != null ?
                                              this.sourceVideoFileRef.getContentType() : JSONNull.getInstance());
            sourceJson.element("localPath", this.sourceVideoFileRef.getPath());
            sourceJson.element("size", this.sourceVideoFileRef.getSize());
        }
        refJson.element("sourceVideoFile", sourceJson);

        JSONObject convJson = new JSONObject();
        if (this.convertedVideoFileRef != null) {
            convJson.element("contentType", this.convertedVideoFileRef.getContentType() != null ?
                                            this.convertedVideoFileRef.getContentType() : JSONNull.getInstance());
            convJson.element("localPath", this.convertedVideoFileRef.getPath());
            convJson.element("size", this.convertedVideoFileRef.getSize());
        }
        refJson.element("conversionVideoFile", convJson);
        
        json.element("ref", refJson);
        return json;
    }
    
    public static Builder newBuilder() {
        return new Builder();
    }
    
    public Builder copyBuilder() {
        Builder b = new Builder();
        b.videoId = this.videoId;
        b.sourceVideoFileRef = new MediaFileRef(this.sourceVideoFileRef.getContentType(), 
                                                this.sourceVideoFileRef.getPath(),
                                                this.sourceVideoFileRef.getSize());
        b.convertedVideoFileRef = new MediaFileRef(this.convertedVideoFileRef.getContentType(), 
                                                this.convertedVideoFileRef.getPath(),
                                                this.convertedVideoFileRef.getSize());
        return b;
    }
    
    public static final class Builder {
        private String videoId;
        private MediaFileRef sourceVideoFileRef;
        private MediaFileRef convertedVideoFileRef;
        
        public Builder videoId(String videoId) {
            this.videoId = videoId;
            return this;
        }
        
        public Builder sourceVideo(String contentType, String path, long size) {
            this.sourceVideoFileRef = new MediaFileRef(contentType, path, size);
            return this;
        }
        
        public Builder convertedVideo(String contentType, String path, long size) {
            this.convertedVideoFileRef = new MediaFileRef(contentType, path, size);
            return this;
        }

        /**
         * TOOD Accept JSON-string instead, so we don't force use of particular JSON impl
         *      on client code.
         * @return 
         */
        public Builder fromJson(JSONObject json) {
            JSONObject ref = json.getJSONObject("ref");
            String vId = ref.getString("videoId");
            MediaFileRef sourceFileRef = null;
            MediaFileRef convFileRef = null;
            
            if (ref.has("sourceVideoFile")) {
                JSONObject sourceJson = ref.getJSONObject("sourceVideoFile");
                sourceFileRef = new MediaFileRef(sourceJson.getString("contentType"),
                                                      sourceJson.getString("localPath"),
                                                      sourceJson.getLong("size"));
            }
            if (ref.has("conversionVideoFile")) {
                JSONObject convJson = ref.getJSONObject("conversionVideoFile");
                convFileRef = new MediaFileRef(convJson.getString("contentType"), 
                                                      convJson.getString("localPath"),
                                                      convJson.getLong("size"));
            }
            
            this.videoId = vId;
            this.sourceVideoFileRef = sourceFileRef;
            this.convertedVideoFileRef = convFileRef;
            return this;
        }
        
        public VideoRef build() {
            return new VideoRef(this);
        }
    }
    
    public String getVideoId() {
        return this.videoId;
    }
    
    public MediaFileRef getSourceVideoFileRef() {
        return this.sourceVideoFileRef;
    }
    
    public MediaFileRef getConvertedVideoFileRef() {
        return this.convertedVideoFileRef;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + this.videoId + "]";
    }
}
