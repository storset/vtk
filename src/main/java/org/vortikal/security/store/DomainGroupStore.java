/* Copyright (c) 2006, 2008, University of Oslo, Norway
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
package org.vortikal.security.store;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.vortikal.security.AuthenticationProcessingException;
import org.vortikal.security.GroupStore;
import org.vortikal.security.Principal;
import org.vortikal.security.Principal.Type;
import org.vortikal.security.PrincipalImpl;

public class DomainGroupStore implements GroupStore {

    private int order = Integer.MAX_VALUE;
    private List<Principal> knownGroups = new ArrayList<Principal>();
    
    public boolean validateGroup(Principal group)
            throws AuthenticationProcessingException {
        if (this.knownGroups.contains(group))
            return true;
        return false;
    }

    public boolean isMember(Principal principal, Principal group) {
        if (validateGroup(group)) {
            String pDomain = principal.getDomain();
            String gDomain = group.getDomain();
            if ((pDomain == null && gDomain == null) || (pDomain != null && pDomain.equals(gDomain))) 
                return true;
        }
        return false;
    }

    public int getOrder() {
        return this.order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public void setKnownGroups(Principal[] knownGroups) {
        this.knownGroups = Arrays.asList(knownGroups);
    }
    
    public void setKnownGroups(List<String> groups) {
        this.knownGroups = new ArrayList<Principal>();
        for (String g: groups) {
            Principal p = new PrincipalImpl(g, Type.GROUP);
            this.knownGroups.add(p);
        }
    }

    public Set<Principal> getMemberGroups(Principal principal) {
        Set<Principal> groups = new HashSet<Principal>();
        for (Principal group: this.knownGroups) {
            if (isMember(principal, group))
                groups.add(group);
        }
        return groups;
    }
}
