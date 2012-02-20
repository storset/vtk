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

public interface MultiHostSearch {

    public static final String URL_PROP_NAME = "solr.url";
    public static final String LANG_PROP_NAME = "solr.lang";
    public static final String MULTIHOST_RESOURCE_PROP_NAME = "solr.isSolrResource";
    public static final String NAME_PROP_NAME = "solr.name";

    public static enum Type {
        // The simplest of all searches. Just map a supplied original search to
        // multi host search and execute
        SIMPE_SEARCH,
        // Resource listing search, map an original search to multi host search,
        // including aggregation and manually approved resources
        RESOURCE_LISTING_SEARCH,
        // Search for resources on a host under a given prefix
        URI_PREFIX_SEARCH,
        // Search for specific resource types on a given host
        RESOURCE_TYPE_SEARCH
    }

    public String getName();

    public String getToken();

    public Search getOriginalSearch();

    public Type getType();

    public Resource getOriginalResource();

    public String getUri();

    public String getResourceType();

}
