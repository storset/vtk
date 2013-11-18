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

import java.net.URI;

/**
 * References to video streaming resources for a video object. Such references
 * are only valid for a limited amount of time, so the same can be said about
 * objects of this class.
 */
public class StreamingRef {
    
    private Token token;
    private URI hlsStream;
    private URI hdsStream;
    
    public StreamingRef(Token token, URI hlsStream, URI hdsStream) {
        if (token == null) {
            throw new IllegalArgumentException("token cannot be null");
        }
        if (hlsStream == null) {
            throw new IllegalArgumentException("hlsStream cannot be null");
        }
        if (hdsStream == null) {
            throw new IllegalArgumentException("hdsStream cannot be null");
        }
        
        this.token = token;
        this.hlsStream = hlsStream;
        this.hdsStream = hdsStream;
    }

    /**
     * @return the token
     */
    public Token token() {
        return token;
    }

    /**
     * @return URI to the "Apple HTTP Live Streaming" stream
     */
    public URI hlsStream() {
        return hlsStream;
    }

    /**
     * @return URI to the "Adobe HTTP Dynamic Streaming" stream
     */
    public URI hdsStream() {
        return hdsStream;
    }

}
