/* Copyright (c) 2012, University of Oslo, Norway
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
package org.vortikal.web.search.collectionlisting;

import java.io.Serializable;

public class CollectionListingCacheKey implements Serializable {

    private static final long serialVersionUID = -4326224006550057333L;

    String token;
    String name;
    String requestUri;
    String sortString;
    int searchLimit;
    int offset;

    public CollectionListingCacheKey(String token, String name, String requestUri, String sortString, int searchLimit,
            int offset) {
        this.token = token;
        this.name = name;
        this.requestUri = requestUri;
        this.sortString = sortString;
        this.searchLimit = searchLimit;
        this.offset = offset;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof CollectionListingCacheKey)) {
            return false;
        }
        CollectionListingCacheKey other = (CollectionListingCacheKey) obj;
        return this.hashCode() == other.hashCode();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 39 * hash + this.requestUri.hashCode();
        hash = 39 * hash + this.name.hashCode();
        hash = 39 * hash + (this.token != null ? this.token.hashCode() : 0);
        hash = hash + (this.sortString != null ? this.sortString.hashCode() : 0);
        hash = hash + this.searchLimit;
        hash = hash + this.offset;
        return hash;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(this.name);
        sb.append(": ").append(this.requestUri);
        if (this.token != null) {
            sb.append(" - ").append(this.token);
        }
        if (this.sortString != null) {
            sb.append(" - ").append(this.sortString.toString());
        }
        sb.append(" - search limit: ").append(this.searchLimit);
        sb.append(" - offset: ").append(this.offset);
        return sb.toString();
    }

}
