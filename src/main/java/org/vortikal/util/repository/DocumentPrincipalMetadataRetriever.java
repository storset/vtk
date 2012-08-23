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
package org.vortikal.util.repository;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.vortikal.repository.store.Metadata;
import org.vortikal.repository.store.PrincipalMetadata;
import org.vortikal.repository.store.PrincipalMetadataDAO;
import org.vortikal.repository.store.PrincipalSearchImpl;
import org.vortikal.security.Principal;
import org.vortikal.security.Principal.Type;
import org.vortikal.security.PrincipalImpl;

public class DocumentPrincipalMetadataRetriever {

    // Used when configured
    private PrincipalMetadataDAO personDocumentPrincipalMetadataDao;

    // For the supplied set of principals, resolve/swap those for which there
    // exists a person document
    public Set<Principal> getPrincipalDocuments(List<Principal> principals, Locale preferredLocale) {

        Set<String> uids = new HashSet<String>();
        for (Principal principal : principals) {
            uids.add(principal.getName());
        }

        return this.getPrincipalDocumentsByUid(uids, preferredLocale);

    }

    public Set<Principal> getPrincipalDocumentsByUid(Set<String> uids, Locale preferredLocale) {

        Set<Principal> result = new HashSet<Principal>();
        if (this.personDocumentPrincipalMetadataDao != null && uids != null && !uids.isEmpty()) {

            PrincipalSearchImpl ps = new PrincipalSearchImpl(uids, preferredLocale);
            List<PrincipalMetadata> principalDocuments = this.personDocumentPrincipalMetadataDao.search(ps,
                    preferredLocale);

            if (principalDocuments != null && principalDocuments.size() > 0) {
                for (PrincipalMetadata metadata : principalDocuments) {
                    PrincipalImpl principal = new PrincipalImpl(metadata.getUid(), Type.USER);
                    Object descriptionObj = metadata.getValue(PrincipalMetadata.DESCRIPTION_ATTRIBUTE);
                    if (descriptionObj != null) {
                        principal.setDescription(descriptionObj.toString());
                    }
                    Object urlObj = metadata.getValue(Metadata.URL_ATTRIBUTE);
                    if (urlObj != null) {
                        principal.setURL(urlObj.toString());
                    }
                    principal.setMetadata(metadata);

                    result.add(principal);

                }
            }
        }

        return result;
    }

    public Map<String, Principal> getPrincipalDocumentsMapByUid(Set<String> uids, Locale preferredLocale) {
        Set<Principal> principals = this.getPrincipalDocumentsByUid(uids, preferredLocale);
        Map<String, Principal> result = new HashMap<String, Principal>();
        for (Principal p : principals) {
            result.put(p.getName(), p);
        }
        return result;
    }

    public void setPersonDocumentPrincipalMetadataDao(PrincipalMetadataDAO personDocumentPrincipalMetadataDao) {
        this.personDocumentPrincipalMetadataDao = personDocumentPrincipalMetadataDao;
    }

    public boolean isDocumentSearchConfigured() {
        return this.personDocumentPrincipalMetadataDao != null;
    }

}
