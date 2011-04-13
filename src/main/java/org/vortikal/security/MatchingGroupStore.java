/* Copyright (c) 2011, University of Oslo, Norway
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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.security.Principal.Type;

public class MatchingGroupStore implements GroupStore {
    private int order = Integer.MAX_VALUE;
    private Map<Principal, Pattern> groupsMap = new HashMap<Principal, Pattern>();
    
    @Override
    public int getOrder() {
        return this.order;
    }
    
    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public boolean validateGroup(Principal group)
            throws AuthenticationProcessingException {
        return this.groupsMap.containsKey(group);
    }

    @Override
    public boolean isMember(Principal principal, Principal group) {
        Pattern p = this.groupsMap.get(group.getQualifiedName());
        if (p == null) {
            return false;
        }
        Matcher m = p.matcher(principal.getQualifiedName());
        return m.matches();
    }

    @Override
    public Set<Principal> getMemberGroups(Principal principal) {
        Set<Principal> groups = null;
        for (Principal group: this.groupsMap.keySet()) {
            Pattern p = this.groupsMap.get(group);
            Matcher m = p.matcher(principal.getQualifiedName());
            if (m.matches()) {
                if (groups == null) {
                    groups = new HashSet<Principal>();
                }
                groups.add(group);
            }
        }
        if (groups == null) {
            return Collections.emptySet();
        }
        return groups;
    }

    @Required
    public void setGroupsMap(Map<String, String> map) {
        for (String id: map.keySet()) {
            Principal group = new PrincipalImpl(id, Type.GROUP);
            this.groupsMap.put(group, Pattern.compile(map.get(id)));
        }
    }

}
