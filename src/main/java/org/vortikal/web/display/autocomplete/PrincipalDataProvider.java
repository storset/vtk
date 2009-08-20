/* Copyright (c) 2009, University of Oslo, Norway
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
package org.vortikal.web.display.autocomplete;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalFactory;
import org.vortikal.security.PrincipalManager;
import org.vortikal.security.Principal.Type;

public class PrincipalDataProvider implements VocabularyDataProvider<Principal> {

    private static final Logger logger = Logger.getLogger(PrincipalDataProvider.class);

    private Type type;
    private PrincipalFactory principalFactory;
    private PrincipalManager principalManager;

    public List<Principal> getCompletions(Path scopeUri, String token) {
        return null;
    }

    public List<Principal> getPrefixCompletions(String prefix, Path contextUri, String token) {

        List<Principal> result = new ArrayList<Principal>(0);

        try {
            List<Principal> searchResult = principalFactory.search(prefix, type);
            if (Type.USER.equals(type)) {
                Principal singleUser = this.principalFactory.getPrincipal(prefix, type);
                if (this.principalManager.validatePrincipal(singleUser)) {
                    searchResult.add(singleUser);
                }
            }
            if (searchResult != null && searchResult.size() > 0) {
                result.addAll(searchResult);
                Collections.sort(result, new PrincipalComparator());
            }
        } catch (Exception e) {
            logger.error("An error occured while getting prefixcompilations", e);
        }

        return result;
    }

    private final class PrincipalComparator implements Comparator<Principal> {

        public int compare(Principal p1, Principal p2) {
            if (Type.USER.equals(type)) {
                return p1.getDescription().compareToIgnoreCase(p2.getDescription());
            }
            return p1.getUnqualifiedName().compareToIgnoreCase(p2.getUnqualifiedName());
        }

    }

    @Required
    public void setType(Principal.Type type) {
        this.type = type;
    }

    @Required
    public void setPrincipalFactory(PrincipalFactory principalFactory) {
        this.principalFactory = principalFactory;
    }

    public void setPrincipalManager(PrincipalManager principalManager) {
        this.principalManager = principalManager;
    }

}
