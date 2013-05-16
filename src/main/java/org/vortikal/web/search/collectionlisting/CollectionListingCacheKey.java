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

import org.vortikal.repository.Path;

public class CollectionListingCacheKey implements Serializable {

    private static final long serialVersionUID = -4326224006550057333L;

    String searchComponentName;
    Path resourcePath;
    String lastModified;
    String token;

    public CollectionListingCacheKey(String searchComponentName, Path resourcePath, String lastModified, String token) {

        if (searchComponentName == null) {
            throw new IllegalArgumentException("Must supply search component name");
        }

        if (resourcePath == null) {
            throw new IllegalArgumentException("Must supply resource path");
        }

        if (lastModified == null) {
            throw new IllegalArgumentException("Must supply last modified date as string");
        }

        this.searchComponentName = searchComponentName;
        this.resourcePath = resourcePath;
        this.lastModified = lastModified;
        this.token = token;
    }

    @Override
    public boolean equals(Object otherObj) {

        if (otherObj == null || !(otherObj instanceof CollectionListingCacheKey)) {
            return false;
        }

        CollectionListingCacheKey other = (CollectionListingCacheKey) otherObj;
        if (!searchComponentName.equals(other.searchComponentName)) {
            return false;
        }
        if (!resourcePath.equals(other.resourcePath)) {
            return false;
        }
        if (!lastModified.equals(other.lastModified)) {
            return false;
        }
        if (token != null && !token.equals(other.token)) {
            return false;
        } else if (other.token != null && !other.token.equals(token)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 9;
        hash = 7 * hash + this.resourcePath.hashCode();
        hash = 7 * hash + this.lastModified.hashCode();
        hash = 7 * hash + this.searchComponentName.hashCode();
        hash = 7 * hash + (this.token != null ? this.token.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(this.searchComponentName);
        sb.append(": ").append(this.resourcePath);
        sb.append(" - ").append(this.lastModified);
        sb.append(" - ").append(this.token);
        return sb.toString();
    }

}
