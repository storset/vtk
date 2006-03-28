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
package org.vortikal.repositoryimpl;

import org.vortikal.repository.Acl;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.PrivilegeDefinition;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.security.Principal;
import org.vortikal.security.roles.RoleManager;

public class Authorization {

    private Principal principal;
    private Acl acl;
    private RoleManager roleManager;
    
    Authorization(Principal principal, Acl acl, RoleManager roleManager) {
        this.principal = principal;
        this.acl = acl;
        this.roleManager = roleManager;
    }
    
    public void authorize(int protectionLevel) throws AuthorizationException {

        if (protectionLevel == PropertyType.PROTECTION_LEVEL_UNEDITABLE)
            throw new AuthorizationException("Principal not authorized for property editing.");
        
        if (this.roleManager.hasRole(principal.getQualifiedName(), RoleManager.ROOT))
            return;

        switch (protectionLevel) {
        case PropertyType.PROTECTION_LEVEL_EDITABLE:
            if (acl.hasPrivilege(principal, PrivilegeDefinition.WRITE))
                return;
            break;
        
        case PropertyType.PROTECTION_LEVEL_PROTECTED:
            if (acl.hasPrivilege(principal, PrivilegeDefinition.WRITE_ACL))
                return;
            break;
        
        case PropertyType.PROTECTION_LEVEL_OWNER_EDITABLE:
            if (principal.equals(acl.getOwner())) {
                return;
            }
        }

        throw new AuthorizationException("Principal not authorized for property editing.");
    }
}
