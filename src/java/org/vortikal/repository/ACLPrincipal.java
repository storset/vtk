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
package org.vortikal.repository;


/**
 * This class encapsulates meta information about a principal.
 */
public class ACLPrincipal implements java.io.Serializable, Cloneable {
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
    private String url = null;
    private boolean isUser = true;

    /**
     * Creates a new principal object with the values send as parameters.
     * Parameter <code>url</code> and <code>isUser</code> is only used if
     * <code>type</code> equals <code>TYPE_URL</code>.
     */
    public static ACLPrincipal getInstance(int type, String url, boolean isUser) {
        ACLPrincipal newPrincipal = new ACLPrincipal();

        newPrincipal.type = type;

        if (type == TYPE_URL) {
            newPrincipal.setURL(url);
            newPrincipal.setIsUser(isUser);
        }

        return newPrincipal;
    }

    public static int getTypeFromName(String name) {
        if (name.equals(NAME_DAV_ALL)) {
            return TYPE_ALL;
        } else if (name.equals(NAME_DAV_AUTHENTICATED)) {
            return TYPE_AUTHENTICATED;
        } else if (name.equals(NAME_DAV_UNAUTHENTICATED)) {
            return TYPE_UNAUTHENTICATED;
        } else if (name.equals(NAME_DAV_SELF)) {
            return TYPE_SELF;
        } else if (name.equals(NAME_DAV_OWNER)) {
            return TYPE_OWNER;
        } else {
            return TYPE_URL;
        }
    }

    /**
     * Gets the name of this ACLPrincipal.
     *
     * @return the name
     */
    public String getName() {
        if (type == TYPE_ALL) {
            return NAME_DAV_ALL;
        } else if (type == TYPE_AUTHENTICATED) {
            return NAME_DAV_AUTHENTICATED;
        } else if (type == TYPE_UNAUTHENTICATED) {
            return NAME_DAV_UNAUTHENTICATED;
        } else if (type == TYPE_SELF) {
            return NAME_DAV_SELF;
        } else if (type == TYPE_OWNER) {
            return NAME_DAV_OWNER;
        } else {
            return getURL();
        }
    }

    /**
     * Gets the type of this ACLPrincipal.
     *
     * @return the value of name
     */
    public int getType() {
        return this.type;
    }

    /**
     * Sets the type of this ACLPrincipal.
     *
     * @param type the type to set. Must be one of the types
     * <code>TYPE_URL</code>, <code>TYPE_ALL</code>,
     * <code>TYPE_UNAUTHENTICATED</code> or
     * <code>TYPE_AUTHENTICATED</code>
     */
    public void setType(int type) {
        this.type = type;
    }

    /**
     * Gets the URL of this ACLPrincipal. This value has no meaning if
     * <code>this.type != TYPE_URL</code>.
     *
     * @return a <code>String</code> representing the URL of the
     * principal, or <code>null</code> if <code>this.type != TYPE_URL</code>
     */
    public String getURL() {
        if (this.type == TYPE_URL) {
            return this.url;
        }

        return null;
    }

    /**
     * Sets the URL of this ACLPrincipal.
     *
     * @param url a <code>String</code> representing the URL.
     */
    public void setURL(String url) {
        this.url = url;
    }

    /**
     * Determines whether this principal Is a user. NOTE: this value
     * has no meaning if <code>this.type != TYPE_URL</code>.
     *
     * @return a <code>boolean</code> true valus if the principal is a
     * user, false if it is a group (default true).
     */
    public boolean isUser() {
        return isUser;
    }

    /**
     * Sets the isUser boolean of this ACLPrincipal.
     *
     * @param isUser a <code>boolean</code>; true if user, false if
     * group.
     */
    public void setIsUser(boolean isUser) {
        this.isUser = isUser;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append(this.getClass().getName()).append(": ");
        sb.append("[type = ").append(type);
        sb.append(", url = ").append(url);
        sb.append(", isUser = ").append(isUser);
        sb.append("]");

        return sb.toString();
    }

    public int hashCode() {
        // FIXME: implement properly
        return 127 + type + url.hashCode() + (isUser ? 1 : 0);
    }

    public boolean equals(Object o) {
        if ((o == null) || !(o instanceof ACLPrincipal)) {
            return false;
        }

        ACLPrincipal other = (ACLPrincipal) o;

        if (this.type != other.type) {
            return false;
        }

        if (((this.url == null) && (other.url != null)) ||
                ((this.url != null) && (other.url == null))) {
            return false;
        }

        if (this.isUser != other.isUser) {
            return false;
        }

        return true;
    }
}
