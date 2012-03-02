/* Copyright (c) 2007, University of Oslo, Norway
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
package org.vortikal.security;

import org.vortikal.repository.store.PrincipalMetadata;

/**
 * A representation of a principal, either user, group or pseudo.
 */
public interface Principal extends Comparable<Principal>, java.io.Serializable {

    // The principal domains we support:
    public static final String PRINCIPAL_USER_DOMAIN = "uio.no";
    public static final String PRINCIPAL_GROUP_DOMAIN = "netgroups.uio.no";
    public static final String PRINCIPAL_WEBID_DOMAIN = "webid.uio.no";

    public enum Type {
        USER, // a named user
        GROUP, // a named group
        PSEUDO // a pseudo user
    }

    /**
     * Gets the name of the principal. Cannot be <code>null</code>.
     * 
     * @return If the domain equals the principalManager's defaultDomain it
     *         returns the unqualified name, otherwise it returns the qualified
     *         name
     */
    public String getName();

    /**
     * Gets the fully qualified name of the principal. If domain is null, just
     * the user name, otherwise 'user@domain'
     * 
     * @return the fully qualified name of the principal
     */
    public String getQualifiedName();

    /**
     * Gets the unqualified name of the principal, stripped of domain
     * 
     * @return the unqualified name of the principal
     */
    public String getUnqualifiedName();

    /**
     * Returns metadata-instance for this principal.
     * 
     * @see PrincipalMetadata
     * 
     * @return An instance of <code>PrincipalMetadata</code> or null if no
     *         metadata has been retrieved for this principal.
     */
    public PrincipalMetadata getMetadata();

    /**
     * Gets the domain of the principal. May be <code>null</code>.
     * 
     * @return the domain of the principal, or <code>null</code> if it has none
     */
    public String getDomain();

    public String getURL();

    public boolean isUser();

    public Type getType();

    public String getDescription();

    /**
     * @return XXX: see the current implementation!
     */
    @Override
    public String toString();

}
