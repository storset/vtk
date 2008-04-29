/* Copyright (c) 2008, University of Oslo, Norway
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
package org.vortikal.repository.store.ldap;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import netscape.ldap.LDAPAttribute;
import netscape.ldap.LDAPConnection;
import netscape.ldap.LDAPEntry;
import netscape.ldap.LDAPException;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.store.PrincipalMetadataDAO;
import org.vortikal.util.cache.SimpleCache;

/**
 * LDAP based metadata DAO, first cut..
 * 
 * TODO: Url could be fetched from LDAP ..
 * XXX: Will only fetch from LDAP for uids *without* domain appended..  
 * XXX: UiO-specifics included.
 *
 */
public class LDAPPrincipalMetadataDAO implements PrincipalMetadataDAO {
    
    private LDAPConnectionManager ldapConnectionManager;
    
    private static final String PRINCIPAL_BASE_DN = 
                                            "cn=users,cn=system,dc=uio,dc=no";
    private static final String PRINCIPAL_UID_ATTRIBUTE = "uid";
    private static final String PRINCIPAL_COMMON_NAME_ATTRIBUTE = "cn";
    
    private static final Pattern REPLACE_PATTERN = Pattern.compile("%u");
    
    private Map<String, String> domainUrlMap;
    
    // Optional extra layer of cache above DAO
    private SimpleCache<String, String> descriptionCache;

    public String getDescription(String uid) throws RepositoryLDAPException {
        // This dao only supports uids without appended domain, so validate first, 
        // so we avoid spamming LDAP with queries for 'root@localhost' and the likes.
        if (!isValidUid(uid)) {
            return null;
        }
        
        // Check cache first
        if (this.descriptionCache != null) {
            String commonName = this.descriptionCache.get(uid);
            if (commonName != null) {
                return commonName;
            }
        }
        
        // Try fetching from LDAP
        try {
            String commonName = null;
            
            LDAPConnection conn = this.ldapConnectionManager.getConnection();
            String entryDn = PRINCIPAL_UID_ATTRIBUTE + "=" + uid + "," + PRINCIPAL_BASE_DN;
            LDAPEntry entry = conn.read(entryDn, new String[]{PRINCIPAL_COMMON_NAME_ATTRIBUTE});
            LDAPAttribute cnAttr = entry.getAttribute(PRINCIPAL_COMMON_NAME_ATTRIBUTE);
            if (cnAttr != null) {
                String[] values = cnAttr.getStringValueArray();
                if (values.length > 0) {
                    commonName = values[0];

                    // Populate description cache.
                    if (this.descriptionCache != null) {
                        this.descriptionCache.put(uid, commonName);
                    }
                }
            }
            
            return commonName;
        } catch (LDAPException e) {
            throw new RepositoryLDAPException(
                    "Got an LDAP exception while fetching principal description: " + e.getMessage(), e);
        }
    }
    
    public String getUrl(String uid, String domain) {
        String url = null;
        if (this.domainUrlMap != null && isValidUid(uid)){
            String urlTemplate = this.domainUrlMap.get(domain);
            if (urlTemplate != null) {
                Matcher matcher = REPLACE_PATTERN.matcher(urlTemplate);
                url = matcher.replaceAll(uid);
            }
        }
        return url;
    }
    
    private boolean isValidUid(String uid) {
        return (uid != null && uid.indexOf('@') == -1);
    }

    @Required
    public void setLdapConnectionManager(LDAPConnectionManager ldapConnectionManager) {
        this.ldapConnectionManager = ldapConnectionManager;
    }

    public void setDescriptionCache(SimpleCache<String, String> descriptionCache) {
        this.descriptionCache = descriptionCache;
    }
    
    public void setDomainUrlMap(Map<String, String> domainUrlMap) {
        this.domainUrlMap = domainUrlMap;
    }

}
