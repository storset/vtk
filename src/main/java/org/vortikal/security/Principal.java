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

import java.util.HashMap;
import java.util.Map;


/**
 * A representation of a principal, either user, group or pseudo.
 */
public class Principal implements Comparable<Principal>, java.io.Serializable {
    
    private static final long serialVersionUID = 3257848766467092530L;

    private static final String DOMAIN_DELIMITER = "@";

    private static String DEFAULT_DOMAIN = "uio.no";
    private static String DEFAULT_GROUP_DOMAIN = "netgroups.uio.no";
    private static Map<String, String> DOMAIN_URL_MAP = new HashMap<String, String>();

    static {
        DOMAIN_URL_MAP.put("uio.no", "http://www.uio.no/sok?person=%u");
    }
    
    public enum Type {
        USER, // a named user
        GROUP, // a named group
        PSEUDO // a pseudo user
     }
     
    
    private String name;
    private String qualifiedName;
    private String domain;
    private String url;
    private Type type;
    
    
    public static final String NAME_AUTHENTICATED = "pseudo:authenticated";
    public static final String NAME_ALL = "pseudo:all";
    public static final String NAME_OWNER = "pseudo:owner";
    
    public static Principal OWNER =  new Principal(NAME_OWNER);
    public static Principal ALL =  new Principal(NAME_ALL);
    public static Principal AUTHENTICATED =  new Principal(NAME_AUTHENTICATED);
    
    private Principal(String name) {
        this.name = name;
        this.qualifiedName = name;
        this.domain = "pseudo:";
        this.type = Type.PSEUDO;
    }
    
    public static Principal getPseudoPrincipal(String name) throws InvalidPrincipalException {
        if (NAME_ALL.equals(name)) return ALL;
        if (NAME_AUTHENTICATED.equals(name)) return AUTHENTICATED;
        if (NAME_OWNER.equals(name)) return OWNER;
        throw new InvalidPrincipalException("Pseudo principal with name '"
                + name + "' doesn't exist");
    }

    
    public Principal(String id, Type type) throws InvalidPrincipalException {

        if (type == null) {
            throw new InvalidPrincipalException("Principal must have a type");
        }
        
        if (type == Type.PSEUDO) {
            throw new InvalidPrincipalException("Principal must be USER or GROUP");
        }
        
        this.type = type;

        if (id == null) {
            throw new InvalidPrincipalException("Tried to get null principal");
        }

        id = id.trim();
        
        if (id.equals(""))
            throw new InvalidPrincipalException("Tried to get \"\" (empty string) principal");
        
        if (id.startsWith(DOMAIN_DELIMITER)) {
            throw new InvalidPrincipalException(
                "Invalid principal id: " + id + ": "
                + "must not start with delimiter: '" + DOMAIN_DELIMITER + "'");
        }
        if (id.endsWith(DOMAIN_DELIMITER)) {
            throw new InvalidPrincipalException(
                "Invalid principal id: " + id + ": "
                + "must not end with delimiter: '" + DOMAIN_DELIMITER + "'");
        }

        if (id.indexOf(DOMAIN_DELIMITER) != id.lastIndexOf(DOMAIN_DELIMITER)) {
            throw new InvalidPrincipalException(
                "Invalid principal id: " + id + ": "
                + "must not contain more that one delimiter: '"
                + DOMAIN_DELIMITER + "'");
        }


        /* Initialize name, domain and qualifiedName to default values
         * matching a setup "without" domains: */
        this.name = id;
        this.qualifiedName = id;
        
        String defDomain = 
            (type == Principal.Type.GROUP) ? DEFAULT_GROUP_DOMAIN : DEFAULT_DOMAIN;

        if (id.indexOf(DOMAIN_DELIMITER) > 0) {

            /* id is a fully qualified principal with a domain part: */
            this.domain = id.substring(id.indexOf(DOMAIN_DELIMITER) + 1);

            
            if (defDomain != null && defDomain.equals(domain)) {
                /* In cases where domain equals default domain, strip
                 * the domain part off the name: */
                name = id.substring(0, id.indexOf(DOMAIN_DELIMITER));
            } 
                        
        } else if (defDomain != null) {

            /* id is not a fully qualified principal, but since we
             * have a default domain, we append it: */
            domain = defDomain;
            qualifiedName = name + DOMAIN_DELIMITER + domain;
        }

        if (domain != null && DOMAIN_URL_MAP != null) {
            String pattern = DOMAIN_URL_MAP.get(domain);
            if (pattern != null) {
                this.url = pattern.replaceAll("%u", name);
            }
        }

    }
    
    public boolean equals(Object another) {
        if (another instanceof Principal) {
            String anotherName = ((Principal)another).getQualifiedName();
            if (getQualifiedName().equals(anotherName)) {
                return true;
            }
        }   
        return false;
    }
    

    public int hashCode() {
        return this.qualifiedName.hashCode();
    }
    

    /**
     * Gets the name of the principal. Cannot be <code>null</code>.
     * @return If the domain equals the principalManager's defaultDomain
     * it returns the unqualified name, otherwise it returns the qualified name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets the fully qualified name of the principal. If domain is
     * null, just the user name, otherwise 'user@domain'
     * 
     * @return the fully qualified name of the principal
     */
    public String getQualifiedName() {
        return this.qualifiedName;
    }

    /**
     * Gets the domain of the principal. May be <code>null</code>.
     *
     * @return the domain of the principal, or <code>null</code> if it
     * has none
     */
    public String getDomain() {
        return this.domain;
    }
    
    public String getURL() {
        return this.url;
    }
    

    public String toString() {
        return this.qualifiedName;
    }

    /**
     * Gets the unqualified name of the principal, stripped of domain
     * 
     * @return the unqualified name of the principal
     */
    public String getUnqualifiedName() {
        if (this.domain == null) return this.name;
        //FIXME: principalmanager's delimiter shouldn't be here!
        return this.qualifiedName.substring(0, this.qualifiedName.indexOf("@"));
    }


    public boolean isUser() {
        return this.type == Principal.Type.USER;
    }


    public Type getType() {
        return this.type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public int compareTo(Principal other) {
        if (other == null) {
            throw new IllegalArgumentException(
                "Cannot compare to a null value");
        }
        return this.qualifiedName.compareTo(other.getQualifiedName());
    }

}
