/* Copyright (c) 2004, University of Oslo, Norway
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
package org.vortikal.repositoryimpl;

public class ACLPrincipal {

    /**
     * Indicates that this ACLPrincipal represents a named user or
     * group identified by a URL.
     */
    public static final int TYPE_URL = 0;

    /**
     * Indicates that this ACLPrincipal represents all (both
     * authenticated and unauthenticated) users.
     */
    public static final int TYPE_ALL = 1;
    public static final String NAME_DAV_ALL = "dav:all";

    /**
     * Indicates that this ACLPrincipal represents all authenticated
     * users.
     */
    public static final int TYPE_AUTHENTICATED = 2;
    public static final String NAME_DAV_AUTHENTICATED = "dav:authenticated";

    /**
     * Indicates that this ACLPrincipal represents all unauthenticated
     * users.
     */
    public static final int TYPE_UNAUTHENTICATED = 3;
    public static final String NAME_DAV_UNAUTHENTICATED = "dav:unauthenticated";

    /**
     * Indicates that this ACLPrincipal represents the current
     * authenticated user.
     */
    public static final int TYPE_SELF = 4;
    public static final String NAME_DAV_SELF = "dav:self";

    /**
     * Indicates that this ACLPrincipal represents the owner of a
     * resource.
     *
     * Note: to obtain the actual URL of the owner of the resource,
     * use <code>Resource.getOwner()</code>
     */
    public static final int TYPE_OWNER = 5;
    public static final String NAME_DAV_OWNER = "dav:owner";
    private int type = TYPE_URL;
    
    
    private boolean isGroup = false;
    private String url = null;
    
    
    public ACLPrincipal(String url, boolean isGroup) {
        this.url = url;
        this.isGroup = isGroup;
    
        if (url.equals("dav:all")) {
            type = TYPE_ALL;
        } else if (url.equals("dav:owner")) {
            type = TYPE_OWNER;
        } else if (url.equals("dav:authenticated")) {
            type = TYPE_AUTHENTICATED;
        } else {
            this.type = TYPE_URL;
        }
    }

    public String getUrl() {
        return this.url;
    }

    public boolean isGroup() {
        return this.isGroup;
    }

    public int getType() {
        return this.type;
    }
    
    public boolean equals(Object o) {
        if (!(o instanceof ACLPrincipal)) {
            return false;
        }

        ACLPrincipal p = (ACLPrincipal) o;

        if (this.isGroup != p.isGroup()) {
            return false;
        }

        if ((this.url == null) && (this.url == p.getUrl())) {
            return true;
        }

        if (((this.url == null) && (p.getUrl() != null)) ||
                ((this.url != null) && (p.getUrl() == null))) {
            return false;
        }

        return this.url.equals(p.getUrl());
    }

    public int hashCode() {
        return this.url.hashCode() + (isGroup ? 1 : 0);
    }
}
