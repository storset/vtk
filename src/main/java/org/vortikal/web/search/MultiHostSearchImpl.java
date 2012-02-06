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
package org.vortikal.web.search;

import org.vortikal.repository.Resource;
import org.vortikal.repository.search.Search;

public class MultiHostSearchImpl implements MultiHostSearch {

    // Name of original search component
    private String name;
    private String token;
    // The original search, i.e the search we want to run on multiple hosts
    private Search originalSearch;
    private Type type;
    private Resource originalResource;
    private String uri;
    private String resourceType;

    public MultiHostSearchImpl(String token, Search originalSearch) {
        this.token = token;
        this.originalSearch = originalSearch;
        this.type = Type.SIMPE_SEARCH;
    }

    public MultiHostSearchImpl(String token, String uri, String resourceType) {
        this.token = token;
        this.uri = uri;
        this.resourceType = resourceType;
        if (uri != null) {
            this.type = Type.URI_PREFIX_SEARCH;
        }
        if (resourceType != null) {
            this.type = Type.RESOURCE_TYPE_SEARCH;
        }
    }

    public MultiHostSearchImpl(String name, String token, Search originalSearch, Resource originalResource) {
        if (originalSearch == null) {
            throw new IllegalArgumentException("Original search cannot be null");
        }
        this.name = name;
        this.token = token;
        this.type = Type.RESOURCE_LISTING_SEARCH;
        this.originalSearch = originalSearch;
        this.originalResource = originalResource;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getToken() {
        return this.token;
    }

    @Override
    public Search getOriginalSearch() {
        return this.originalSearch;
    }

    @Override
    public Type getType() {
        return this.type;
    }

    @Override
    public Resource getOriginalResource() {
        return this.originalResource;
    }

    @Override
    public String getUri() {
        return this.uri;
    }

    @Override
    public String getResourceType() {
        return this.resourceType;
    }

}
