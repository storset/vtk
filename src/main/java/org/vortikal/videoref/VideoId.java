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

/**
 * Video object "URNs" or identifiers.
 *
 */
public class VideoId {

    private static final String IDTYPE = "video";
    private final String host;
    private final long numericId;

    private VideoId(String host, long numericId) {
        this.host = host;
        this.numericId = numericId;
    }

    public static VideoId fromString(String videoId) {
        String[] parts = videoId.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid video id: " + videoId);
        }
        if (!IDTYPE.equals(parts[0])) {
            throw new IllegalArgumentException("Invalid video id: " + videoId);
        }
        if (parts[1].isEmpty()) {
            throw new IllegalArgumentException("Empty host in video id: " + videoId);
        }
        String host = parts[1];
        long numericId = Long.parseLong(parts[2]);
        if (numericId < 0) {
            throw new IllegalArgumentException("Negative numeric identifier in video id: " + videoId);
        }
        return new VideoId(host, numericId);
    }

    public long numericId() {
        return this.numericId;
    }

    public String host() {
        return this.host;
    }

    @Override
    public String toString() {
        return IDTYPE + ":" + this.host + ":" + this.numericId;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + (this.host != null ? this.host.hashCode() : 0);
        hash = 53 * hash + (int) (this.numericId ^ (this.numericId >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final VideoId other = (VideoId) obj;
        if ((this.host == null) ? (other.host != null) : !this.host.equals(other.host)) {
            return false;
        }
        if (this.numericId != other.numericId) {
            return false;
        }
        return true;
    }

}
