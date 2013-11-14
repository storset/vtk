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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class VideoFileRef {
    
    private final String contentType;
    private final String path;
    private final long size;
    private final Map<String,Object> metadata;

    public VideoFileRef(String contentType, String path, long size, Map<String,Object> metadata) {
        if (path == null) {
            throw new IllegalArgumentException("path cannot be null");
        }
        if (size < 0) {
            throw new IllegalArgumentException("size cannot be < 0");
        }
        this.path = path;
        this.size = size;
        this.contentType = contentType;
        this.metadata = metadata != null ? new HashMap<String,Object>(metadata) : Collections.<String,Object>emptyMap();
    }

    /**
     * 
     * @return Content type of media file. May be <code>null</code>.
     */
    public String contentType() {
        return this.contentType;
    }

    /**
     * @return Local path to media file.
     */
    public String path() {
        return this.path;
    }
    
    /**
     * @return Size of referenced media file in bytes.
     */
    public long size() {
        return this.size;
    }

    /**
     * Loosely defined video file metadata. 
     * @return an immutable map of key value pairs with metadata objects. Returns
     * an empty map if no metadata is available.
     */
    public Map<String,Object> metadata() {
        return Collections.unmodifiableMap(this.metadata);
    }

}
