/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.vortikal.webdav.ifheader;

import org.vortikal.repository.Resource;

/**
 * The <code>IfHeaderInterface</code> interface abstracts away the difference of
 * tagged and untagged <em>If</em> header lists. The single method provided
 * by this interface is to check whether a request may be applied to a
 * resource with given token and etag.
 */
public interface StateEntryList {

    /**
     * Matches the resource, token, and etag against this
     * <code>IfHeaderInterface</code> instance.
     *
     * @param resource The resource to match this instance against. This
     *      must be absolute URI of the resource as defined in Section 3
     *      (URI Syntactic Components) of RFC 2396 Uniform Resource
     *      Identifiers (URI): Generic Syntax.
     * @param token The resource's lock token to match
     * @param etag The resource's etag to match
     *
     * @return <code>true</code> if the header matches the resource with
     *      token and etag, which means that the request is applicable
     *      to the resource according to the <em>If</em> header.
     */
    public boolean matches(Resource resource);
    
    public boolean matchesEtags(Resource resource);
    
}
