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

import java.util.Set;

import org.springframework.core.Ordered;

public interface GroupStore extends Ordered {

    /**
     * Validates the existence of a given group.
     *
     * @param group the group to validate
     * @return <code>true</code> if the group exists,
     * <code>false</code> otherwise.
     */
    public boolean validateGroup(Principal group)
        throws AuthenticationProcessingException;

    
    /**
     * Convenience method for determining whether a principal is a
     * member of a group.
     *
     * @param principal the name of the principal
     * @param group the group in question 
     * @return true if the group exists and the given principal is a
     * member of that group, false otherwise.
     */
    public boolean isMember(Principal principal, Principal group);


    /**
     * Lists groups of a given principal.
     *
     * @param principal the principal in question
     * @return a set of the groups of which this principal is a
     * member. If no such groups exists, the return value should be an
     * empty set (not <code>null</code>).
     */
    public Set<Principal> getMemberGroups(Principal principal);
}
