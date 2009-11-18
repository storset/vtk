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
package org.vortikal.security;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.RepositoryException;
import org.vortikal.repository.store.PrincipalMetadata;
import org.vortikal.repository.store.PrincipalMetadataDAO;
import org.vortikal.repository.store.PrincipalSearch;
import org.vortikal.repository.store.PrincipalSearchImpl;
import org.vortikal.repository.store.UnsupportedPrincipalDomainException;
import org.vortikal.security.Principal.Type;

public class PrincipalFactory {

    private static final Log LOG = LogFactory.getLog(PrincipalFactory.class);
    
    public static final String NAME_AUTHENTICATED = "pseudo:authenticated";
    public static final String NAME_ALL = "pseudo:all";
    public static final String NAME_OWNER = "pseudo:owner";

    public static Principal OWNER =  new PrincipalImpl(NAME_OWNER);
    public static Principal ALL =  new PrincipalImpl(NAME_ALL);
    public static Principal AUTHENTICATED =  new PrincipalImpl(NAME_AUTHENTICATED);
    
    // This dao will only be used if configured.
    private PrincipalMetadataDAO principalMetadataDao;

    public Principal getPrincipal(String id, Type type) throws InvalidPrincipalException {

        if (type == null) {
            throw new InvalidPrincipalException("Principal must have a type");
        }
        
        if (type == Type.PSEUDO) {
            return getPseudoPrincipal(id);
        }

        if (id == null) {
            throw new InvalidPrincipalException("Tried to get null principal");
        }

        id = id.trim();
        
        if (id.equals(""))
            throw new InvalidPrincipalException("Tried to get \"\" (empty string) principal");

        PrincipalImpl principal = new PrincipalImpl(id, type);
        if (principal.getType() == Type.USER) {
            if (this.principalMetadataDao != null) {

                // Set metadata for principal, if we can get any.
                try {
                    PrincipalMetadata metadata = this.principalMetadataDao.getMetadata(principal);
                    if (metadata != null) {
                        principal.setDescription((String)metadata.getValue(PrincipalMetadata.DESCRIPTION_ATTRIBUTE));
                        principal.setURL((String)metadata.getValue(PrincipalMetadata.URL_ATTRIBUTE));
                        principal.setMetadata(metadata);
                    }
                } catch (UnsupportedPrincipalDomainException d) {
                    // Ignore
                } catch (Exception e) {
                    LOG.warn(
                      "Exception while fetching principal metadata", e);
                }
            }
        }
        
        return principal;
    }
    
    public List<Principal> search(final String filter, final Type type) throws RepositoryException {
        List<Principal> retval = null;
        if (this.principalMetadataDao != null) {

            PrincipalSearch search = new PrincipalSearchImpl(type, filter);

            List<PrincipalMetadata> results = this.principalMetadataDao.search(search);
            if (results != null) {
                retval = new ArrayList<Principal>(results.size());
                for (PrincipalMetadata metadata: results) {
                    PrincipalImpl principal = new PrincipalImpl(metadata.getQualifiedName());
                    principal.setType(type);
                    principal.setDescription((String)metadata.getValue(PrincipalMetadata.DESCRIPTION_ATTRIBUTE));
                    principal.setURL((String)metadata.getValue(PrincipalMetadata.URL_ATTRIBUTE));
                    principal.setMetadata(metadata);
                    retval.add(principal);
                }
            }
        }
        return retval;
    }

    private Principal getPseudoPrincipal(String name) throws InvalidPrincipalException {
        if (NAME_ALL.equals(name)) return ALL;
        if (NAME_AUTHENTICATED.equals(name)) return AUTHENTICATED;
        if (NAME_OWNER.equals(name)) return OWNER;
        throw new InvalidPrincipalException("Pseudo principal with name '"
                + name + "' doesn't exist");
    }
    
    public void setPrincipalMetadataDao(PrincipalMetadataDAO principalMetadataDao) {
        this.principalMetadataDao = principalMetadataDao;
    }

}
