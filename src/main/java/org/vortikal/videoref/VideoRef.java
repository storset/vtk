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

import java.util.Date;
import net.sf.json.JSONObject;
import org.apache.commons.codec.binary.Base64;
import org.vortikal.repository.resourcetype.BufferedBinaryValue;

/**
 * Vortex video reference.
 * Instances of this class should be strictly immutable and
 * construction of new instances follows the builder pattern.
 */
public class VideoRef {
    
    private final VideoId videoId;
    private final Date refUpdateTimestamp;      // Timestamp of last reference update from video system
    private final String uploadContentType;     // Original content type of video when uploaded to Vortex
    private final String status;                // Video status
    private final double durationSeconds;        // Video duration in seconds
    private final BufferedBinaryValue generatedThumbnail;
    private final VideoFileRef sourceVideoFileRef;
    private final VideoFileRef convertedVideoFileRef;
    
    private VideoRef(Builder builder) {
        this.videoId = builder.videoId;
        this.sourceVideoFileRef = builder.sourceVideoFileRef;
        this.convertedVideoFileRef = builder.convertedVideoFileRef;
        this.refUpdateTimestamp = builder.refUpdateTimestamp;
        this.uploadContentType = builder.uploadContentType;
        this.status = builder.status;
        this.durationSeconds = builder.durationSeconds;
        this.generatedThumbnail = builder.generatedThumbnail;
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
        refJson.element("status", this.status);
        refJson.element("durationSeconds", this.durationSeconds);
        refJson.element("refUpdateTimestamp", this.refUpdateTimestamp.getTime());

        if (this.sourceVideoFileRef != null) {
            JSONObject sourceJson = new JSONObject();
            sourceJson.elementOpt("contentType", this.sourceVideoFileRef.contentType());
            sourceJson.element("localPath", this.sourceVideoFileRef.path());
            sourceJson.element("size", this.sourceVideoFileRef.size());
            sourceJson.element("metadata", this.sourceVideoFileRef.metadata());
            refJson.element("sourceVideoFile", sourceJson);
        }

        if (this.convertedVideoFileRef != null) {
            JSONObject convJson = new JSONObject();
            convJson.elementOpt("contentType", this.convertedVideoFileRef.contentType());
            convJson.element("localPath", this.convertedVideoFileRef.path());
            convJson.element("size", this.convertedVideoFileRef.size());
            convJson.element("metadata", this.convertedVideoFileRef.metadata());
            refJson.element("convertedVideoFile", convJson);
        }
        
        if (this.generatedThumbnail != null) {
            refJson.element("generatedThumbnail",
                    Base64.encodeBase64String(this.generatedThumbnail.getBytes()));
            refJson.element("generatedThumbnailMimeType", 
                    this.generatedThumbnail.getContentType());
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
        b.status = this.status;
        b.durationSeconds = this.durationSeconds;
        if (this.sourceVideoFileRef != null) {
            b.sourceVideoFileRef = new VideoFileRef(this.sourceVideoFileRef.contentType(),
                    this.sourceVideoFileRef.path(),
                    this.sourceVideoFileRef.size(),
                    this.sourceVideoFileRef.metadata());

        }
        if (this.convertedVideoFileRef != null) {
            b.convertedVideoFileRef = new VideoFileRef(this.convertedVideoFileRef.contentType(),
                    this.convertedVideoFileRef.path(),
                    this.convertedVideoFileRef.size(),
                    this.convertedVideoFileRef.metadata());
        }
        b.refUpdateTimestamp = new Date(this.refUpdateTimestamp.getTime());
        b.uploadContentType = this.uploadContentType;
        if (this.generatedThumbnail != null) {
            try {
                b.generatedThumbnail = (BufferedBinaryValue) this.generatedThumbnail.clone();
            } catch (CloneNotSupportedException c) {}
        }
        return b;
    }
    
    public static final class Builder {
        private VideoId videoId;
        private Date refUpdateTimestamp;
        private String uploadContentType;
        private String status;
        private double durationSeconds;
        private VideoFileRef sourceVideoFileRef;
        private VideoFileRef convertedVideoFileRef;
        private BufferedBinaryValue generatedThumbnail;
        
        public Builder videoId(String videoId) {
            this.videoId = VideoId.fromString(videoId);
            return this;
        }
        
        public Builder status(String status) {
            this.status = status;
            return this;
        }
        
        public Builder durationSeconds(double durationSeconds) {
            this.durationSeconds = durationSeconds;
            return this;
        }
        
        public Builder generatedThumbnail(BufferedBinaryValue generatedThumbnail) {
            try {
                this.generatedThumbnail = (BufferedBinaryValue)generatedThumbnail.clone();
            } catch (CloneNotSupportedException c) {}
            return this;
        }
        
        public Builder generatedThumbnail(String b64data, String contentType) {
            this.generatedThumbnail = new BufferedBinaryValue(Base64.decodeBase64(b64data), contentType);
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
        
        public Builder sourceVideo(VideoFileRef fileRef) {
            this.sourceVideoFileRef = fileRef;
            return this;
        }
        
        public Builder convertedVideo(VideoFileRef fileRef) {
            this.convertedVideoFileRef = fileRef;
            return this;
        }
        
        public VideoRef build() {
            if (this.videoId == null) {
                throw new IllegalStateException("Video id must be set");
            }
            if (this.refUpdateTimestamp == null) {
                this.refUpdateTimestamp = new Date();
            }
            if (this.status == null) {
                this.status = "unknown";
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
            
            VideoFileRef sourceFileRef = null;
            VideoFileRef convFileRef = null;
            
            if (ref.has("sourceVideoFile")) {
                JSONObject sourceJson = ref.getJSONObject("sourceVideoFile");
                sourceFileRef = new VideoFileRef(sourceJson.optString("contentType", null),
                                            sourceJson.getString("localPath"),
                                            sourceJson.getLong("size"),
                                            sourceJson.getJSONObject("metadata"));
            }
            if (ref.has("convertedVideoFile")) {
                JSONObject convJson = ref.getJSONObject("convertedVideoFile");
                convFileRef = new VideoFileRef(convJson.optString("contentType", null),
                                          convJson.getString("localPath"),
                                          convJson.getLong("size"),
                                          convJson.getJSONObject("metadata"));
            }
            if (ref.has("generatedThumbnail") && ref.has("generatedThumbnailMimeType")) {
                try {
                    byte[] data = Base64.decodeBase64(ref.getString("generatedThumbnail"));
                    String contentType = ref.getString("generatedThumbnailMimeType");
                    this.generatedThumbnail = new BufferedBinaryValue(data, contentType);
                } catch (Exception e) {}
            }
            
            this.videoId = VideoId.fromString(ref.getString("videoId"));
            this.status = ref.optString("status", "unknown");
            this.durationSeconds = ref.optDouble("durationSeconds", 0d);
            this.sourceVideoFileRef = sourceFileRef;
            this.convertedVideoFileRef = convFileRef;
            return this;
        }
    }
    
    public VideoId videoId() {
        return this.videoId;
    }
    
    /**
     * Get source video file reference.
     * @return source video file reference, or <code>null</code> if not
     * available.
     */
    public VideoFileRef sourceVideo() {
        return this.sourceVideoFileRef;
    }
    
    /**
     * Get converted video file reference.
     * @return converted video file reference, or <code>null</code> if not
     * available.
     */
    public VideoFileRef convertedVideo() {
        return this.convertedVideoFileRef;
    }

    /**
     * Get timestamp for when this video reference was last updated.
     * @return timestamp of last update time for this reference.
     */
    public Date refUpdateTimestamp() {
        return new Date(this.refUpdateTimestamp.getTime());
    }

    /**
     * Get content type of video as guessed by Vortex at upload time.
     * @return content type as string
     */
    public String uploadContentType() {
        return this.uploadContentType;
    }
    
    /**
     * @return video processing status as a string.
     */
    public String status() {
        return this.status;
    }
    
    public double durationSeconds() {
        return this.durationSeconds;
    }

    /**
     * @return <code>true</code> if this video file reference has
     * a generated thumbnail image.
     */
    public boolean hasGeneratedThumbnail() {
        return this.generatedThumbnail != null;
    }

    /**
     * Return binary value for thumbnail image that was automatically
     * created for the video. This data is fetched from the videoapp.
     * @return binary value buffer, or <code>null</code> if no generated
     * thumbnail is available.
     */
    public BufferedBinaryValue generatedThumbnail() {
        if (this.generatedThumbnail == null) return null;
        try {
            return (BufferedBinaryValue)this.generatedThumbnail.clone();
        } catch (CloneNotSupportedException c) {
            return null; // dead code required by Java. Hooray.
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + this.videoId + "]";
    }
}
