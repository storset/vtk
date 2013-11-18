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

/**
 *
 */
public class Token {
  
    private TokenId tokenId;
    private VideoId videoId;
    private String tokenValue;
    private Date expiryTime;
    
    public Token(TokenId tokenId, String tokenValue, Date expiryTime, VideoId videoId) {
        if (tokenId == null) {
            throw new IllegalArgumentException("tokenId cannot be null");
        }
        if (tokenValue == null) {
            throw new IllegalArgumentException("tokenValue cannot be null");
        }
        if (expiryTime == null) {
            throw new IllegalArgumentException("expiryTime cannot be null");
        }
        if (videoId == null) {
            throw new IllegalArgumentException("videoId cannot be null");
        }
        this.tokenId = tokenId;
        this.tokenValue = tokenValue;
        this.expiryTime = expiryTime;
        this.videoId = videoId;
    }
    
    /**
     * Object identifier for this token.
     * @return 
     */
    public TokenId tokenId() {
        return tokenId;
    }
    
    /**
     * @return video id for which this token applies
     */
    public VideoId videoId() {
        return videoId;
    }
    
    /**
     * Value of token.
     * @return 
     */
    public String tokenValue() {
        return tokenValue;
    }
    
    /**
     * Time of expiry for token.
     * @return time of expiry as <code>Date</code> in local time.
     */
    public Date expiryTime() {
        return new Date(expiryTime.getTime());
    }

    /**
     * @return <code>true</code> if the token has expired (local time has passed
     * expiry time, as returned by {@link #expiryTime()}).
     */
    public boolean isExpired() {
        return expiryTime.before(new Date());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + tokenId + "]";
    }
    
}
