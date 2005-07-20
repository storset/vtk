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
package org.vortikal.security;


import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.util.codec.MD5;


/**
 * An implementation of MD5PasswordPrincipalManager that stores its
 * user and group information in-memory.
 */
public class PropertyConfigurableMD5PrincipalStore
  implements MD5PasswordPrincipalStore, InitializingBean {

    private Map principals;
    private Map groups;
    private String realm;
    private String domain;

    
    /**
     * Sets the principal map. The map is assumed to contain entries
     * of type <code>(username, md5hash)</code>, and
     * <code>md5hash</code> is in turn assumed to be a hashed value of
     * the string <code>username:realm:password</code>.
     */
    public void setPrincipals(Map principals) {
        this.principals = principals;
    }



    /**
     * Sets the groups map. The map is assumed to contain entries of
     * type <code>(group, memberlist)</code>, where
     * <code>memberlist</code> is a <code>java.util.List</code> of
     * strings which represent the names of those principals that are
     * members of the group.
     */
    public void setGroups(Map groups) {
        this.groups = groups;
    }
    


    public boolean validatePrincipal(Principal principal) {
        if (principal == null) return false;
        if (!domain.equals(principal.getDomain())) return false;

        return this.principals.containsKey(principal.getQualifiedName());
    }
    


    public boolean validateGroup(String groupName) {
        return this.groups.containsKey(groupName);
    }
    

    /**
     * @see org.vortikal.security.MD5PasswordPrincipalStore#getMD5HashString(java.lang.String)
     * @deprecated
     */
    public String getMD5HashString(String principal) {
        return (String) this.principals.get(principal);
    }
    

    public String[] resolveGroup(String groupName) {
        if (!this.groups.containsKey(groupName)) {
            return new String[0];
        }

        List members = (List) this.groups.get(groupName);
        return (String[]) members.toArray(new String[members.size()]);
    }


    public boolean isMember(Principal principal, String groupName) {
        if (!this.groups.containsKey(groupName)) {
            return false;
        }

        List members = (List) this.groups.get(groupName);
        return members.contains(principal.getQualifiedName());
    }



    /**
     * @see org.vortikal.security.MD5PasswordPrincipalStore#getRealm()
     * @deprecated
     */
    public String getRealm() {
        return realm;
    }
    
    
    public void setRealm(String realm) {
        this.realm = realm;
    }


    public void setDomain(String domain) {
        this.domain = domain;
    }

    
    /**
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {
        if (domain == null)
            throw new BeanInitializationException(
                "Property 'domain' must be set");
    }



    public void authenticate(Principal principal, String password)
        throws AuthenticationException {
        
        String hash = getMD5HashString(principal.getQualifiedName());
        String clientHash = 
            MD5.md5sum(principal.getQualifiedName() + ":" + realm + ":" + password); 

        if (hash == null || !hash.equals(clientHash)) {
            throw new AuthenticationException(
                "Authentication failed for principal " + principal.getQualifiedName()
                + ", " + "wrong credentials.");
        }
    }

}

