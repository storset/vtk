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

import java.util.Date;
import net.sf.json.JSONObject;

/**
 * Vortex video reference.
 */
public class VideoRef {
    
    private final VideoId videoId;
    private final Date refUpdateTimestamp;      // Timestamp of last reference update from video system
    private final String uploadContentType;     // Original content type of video when uploaded to Vortex
    private final FileRef sourceVideoFileRef;
    private final FileRef convertedVideoFileRef;
    
    private VideoRef(Builder builder) {
        this.videoId = builder.videoId;
        this.sourceVideoFileRef = builder.sourceVideoFileRef;
        this.convertedVideoFileRef = builder.convertedVideoFileRef;
        this.refUpdateTimestamp = builder.refUpdateTimestamp;
        this.uploadContentType = builder.uploadContentType;
    }
    
    /**
     * Whatever. Consider auto-serialization instead.
     * 
     * @return object formatted as JSON string, parseable by {@link Builder#fromJsonString(java.lang.String) }.
     */
    public String toJsonString() {
        JSONObject json = new JSONObject();
        json.element("mediaref", true);
        json.element("resourcetype", "videoref");
        json.elementOpt("uploadContentType", this.uploadContentType);

        JSONObject refJson = new JSONObject();

        refJson.element("videoId", this.videoId.toString());
        refJson.element("refUpdateTimestamp", this.refUpdateTimestamp.getTime());

        if (this.sourceVideoFileRef != null) {
            JSONObject sourceJson = new JSONObject();
            sourceJson.elementOpt("contentType", this.sourceVideoFileRef.contentType());
            sourceJson.element("localPath", this.sourceVideoFileRef.path());
            sourceJson.element("size", this.sourceVideoFileRef.size());
            refJson.element("sourceVideoFile", sourceJson);
        }

        if (this.convertedVideoFileRef != null) {
            JSONObject convJson = new JSONObject();
            convJson.elementOpt("contentType", this.convertedVideoFileRef.contentType());
            convJson.element("localPath", this.convertedVideoFileRef.path());
            convJson.element("size", this.convertedVideoFileRef.size());
            refJson.element("convertedVideoFile", convJson);
        }

        json.element("ref", refJson);
        return json.toString(2);
    }
    
    public static Builder fromJsonString(String json) {
        return newBuilder().fromJsonString(json);
    }
    
    public static Builder newBuilder() {
        return new Builder();
    }
    
    public Builder copyBuilder() {
        Builder b = new Builder();
        b.videoId = this.videoId;
        if (this.sourceVideoFileRef != null) {
            b.sourceVideoFileRef = new FileRef(this.sourceVideoFileRef.contentType(),
                    this.sourceVideoFileRef.path(),
                    this.sourceVideoFileRef.size());

        }
        if (this.convertedVideoFileRef != null) {
            b.convertedVideoFileRef = new FileRef(this.convertedVideoFileRef.contentType(),
                    this.convertedVideoFileRef.path(),
                    this.convertedVideoFileRef.size());
        }
        b.refUpdateTimestamp = new Date(this.refUpdateTimestamp.getTime());
        b.uploadContentType = this.uploadContentType;
        return b;
    }
    
    public static final class Builder {
        private VideoId videoId;
        private Date refUpdateTimestamp;
        private String uploadContentType;
        private FileRef sourceVideoFileRef;
        private FileRef convertedVideoFileRef;
        
        public Builder videoId(String videoId) {
            this.videoId = VideoId.fromString(videoId);
            return this;
        }
        
        public Builder videoId(VideoId videoId) {
            this.videoId = videoId;
            return this;
        }
        
        public Builder refUpdateTimestamp(Date ts) {
            this.refUpdateTimestamp = new Date(ts.getTime());
            return this;
        }
        
        public Builder uploadContentType(String contentType) {
            this.uploadContentType = contentType;
            return this;
        }
        
        public Builder sourceVideo(FileRef fileRef) {
            this.sourceVideoFileRef = fileRef;
            return this;
        }
        
        public Builder convertedVideo(FileRef fileRef) {
            this.convertedVideoFileRef = fileRef;
            return this;
        }
        
        public Builder sourceVideo(String contentType, String path, long size) {
            this.sourceVideoFileRef = new FileRef(contentType, path, size);
            return this;
        }
        
        public Builder convertedVideo(String contentType, String path, long size) {
            this.convertedVideoFileRef = new FileRef(contentType, path, size);
            return this;
        }

        public VideoRef build() {
            if (this.videoId == null) {
                throw new IllegalStateException("Video id must be set");
            }
            if (this.refUpdateTimestamp == null) {
                this.refUpdateTimestamp = new Date();
            }
            return new VideoRef(this);
        }
        
        /**
         * @param jsonString JSON-formatted string
         * @return A <code>Builder</code> initialized by JSON string representing a VideoRef obj.
         */
        private Builder fromJsonString(String jsonString) {
            JSONObject json = JSONObject.fromObject(jsonString);
            if (!json.has("mediaref")
                     || !json.getBoolean("mediaref")
                     || !json.has("resourcetype")
                     || !"videoref".equals(json.getString("resourcetype"))) {

                throw new IllegalArgumentException("Not a proper videoref: " + json);
            }

            this.uploadContentType = json.optString("uploadContentType", null);
            
            JSONObject ref = json.getJSONObject("ref");
            
            this.refUpdateTimestamp = new Date(ref.getLong("refUpdateTimestamp"));
            
            FileRef sourceFileRef = null;
            FileRef convFileRef = null;
            
            if (ref.has("sourceVideoFile")) {
                JSONObject sourceJson = ref.getJSONObject("sourceVideoFile");
                sourceFileRef = new FileRef(sourceJson.optString("contentType", null),
                                            sourceJson.getString("localPath"),
                                            sourceJson.getLong("size"));
            }
            if (ref.has("convertedVideoFile")) {
                JSONObject convJson = ref.getJSONObject("convertedVideoFile");
                convFileRef = new FileRef(convJson.optString("contentType", null),
                                          convJson.getString("localPath"),
                                          convJson.getLong("size"));
            }
            
            this.videoId = VideoId.fromString(ref.getString("videoId"));
            this.sourceVideoFileRef = sourceFileRef;
            this.convertedVideoFileRef = convFileRef;
            return this;
        }
        
    }
    
    public VideoId videoId() {
        return this.videoId;
    }
    
    public FileRef sourceVideo() {
        return this.sourceVideoFileRef;
    }
    
    public FileRef convertedVideo() {
        return this.convertedVideoFileRef;
    }
    
    public Date refUpdateTimestamp() {
        return new Date(this.refUpdateTimestamp.getTime());
    }
    
    public String uploadContentType() {
        return this.uploadContentType;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + this.videoId + "]";
    }
}
