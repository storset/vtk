/* Copyright (c) 2006, University of Oslo, Norway
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

import java.util.Map;

/**
 * Centralized principal factory
 * 
 * Controls the notion of default domain on principals
 * 
 * <p>Configurable properties:
 * <ul>
 *   <li><code>defaultDomain</code> - a {@link String} specifying the
 *   domain to append to unqualified principal names. If this
 *   property is not specified, the behavior will be as if no domains
 *   exist at all.
 *   <li><code>defaultGroupDomain</code> - the domain to append to unqualified group names 
 *   <li><code>domainURLMap</code> - a map of (<code>domain,
 *   URL-pattern</code>) entries. The domain corresponds to a
 *   principal {@link Principal#getDomain domain}, and the URL pattern
 *   is a string in which the sequence <code>%u</code> is substituted
 *   with the principal's (short) {@link Principal#getName name},
 *   thereby forming a unique URL for each principal. This URL can be
 *   acessed using the {@link Principal#getURL} method.
 * </ul>
 *
 */
public class PrincipalFactoryImpl implements PrincipalFactory {

    private static final String DOMAIN_DELIMITER = "@";

    private String defaultDomain;
    private String defaultGroupDomain;
    private Map domainURLMap;

    public Principal getUserPrincipal(String id) {
        return getPrincipal(id, Principal.TYPE_USER);
    }
    
    public Principal getGroupPrincipal(String id) {
        return getPrincipal(id, Principal.TYPE_GROUP);
    }
    
    private Principal getPrincipal(String id, int type) {
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
        String name = id;
        String domain = null;
        String qualifiedName = id;
        
        String defDomain = 
            (type == Principal.TYPE_GROUP) ? this.defaultGroupDomain : this.defaultDomain;

        if (id.indexOf(DOMAIN_DELIMITER) > 0) {

            /* id is a fully qualified principal with a domain part: */
            domain = id.substring(id.indexOf(DOMAIN_DELIMITER) + 1);

            
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

        String url = null;
        if (domain != null && this.domainURLMap != null) {
            String pattern = (String) this.domainURLMap.get(domain);
            if (pattern != null) {
                url = pattern.replaceAll("%u", name);
            }
        }

        PrincipalImpl p  = new PrincipalImpl(name, qualifiedName, domain, url);
        p.setType(type);
        return p;
    }

    public void setDefaultDomain(String defaultDomain) {
        if (defaultDomain != null) {

            if ("".equals(defaultDomain.trim())) {
                defaultDomain = null;

            } else if (defaultDomain.indexOf(DOMAIN_DELIMITER) != -1) {
                throw new InvalidPrincipalException(
                    "Invalid domain: " + defaultDomain + ": "
                    + "must not contain delimiter: '" + DOMAIN_DELIMITER + "'");
            }
            this.defaultDomain = defaultDomain;
        }
    }
    

    public void setDomainURLMap(Map domainURLMap) {
        this.domainURLMap = domainURLMap;
    }

    public void setDefaultGroupDomain(String defaultGroupDomain) {
        this.defaultGroupDomain = defaultGroupDomain;
    }
    
}
