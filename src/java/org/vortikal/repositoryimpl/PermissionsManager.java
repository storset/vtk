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

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.vortikal.repository.Acl;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.PrivilegeDefinition;
import org.vortikal.repository.Resource;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalManager;
import org.vortikal.security.roles.RoleManager;

public class PermissionsManager {

    private RoleManager roleManager;
    private PrincipalManager principalManager;

    public void authorize(Resource resource, Principal principal, String action)
            throws AuthenticationException, AuthorizationException, IOException {

        Acl acl = resource.getAcl();

        /*
         * Special treatment for uio:read-processed needed: dav:read also grants
         * uio:read-processed
         */
        if (action.equals(PrivilegeDefinition.CUSTOM_PRIVILEGE_READ_PROCESSED)) {
            try {
                authorize(resource, principal, PrivilegeDefinition.READ);

                return;
            } catch (AuthenticationException e) {
                /* Handle below */
            } catch (AuthorizationException e) {
                /* Handle below */
            }
        }

        List principalList = acl.getPrincipalList(action);

        /*
         * A user is granted access if one of these conditions are met:
         * 
         * 
         * 1) (dav:all, action) is present in the resource's ACL. NOTE: Now
         * limits this to read operations
         * 
         * The rest requires that the user is authenticated:
         * 
         * 2) user is authenticated and dav:authenticated is present
         * 
         * 3a) user has role ROOT
         * 
         * 3b) action = 'read' and user has role READ_EVERYTHING
         * 
         * 4a) dav:owner evaluates to user and action is dav:read, dav:write or
         * dav:write-acl (COMMENTED OUT)
         * 
         * The rest is meaningless if principalList == null:
         * 
         * 4b) dav:owner evaluates to user and (dav:owner, action) is present in
         * the resource's ACL
         * 
         * 5) (user, action) is present in the resource's ACL
         * 
         * 6) (g, action) is present in the resource's ACL, where g is a group
         * identifier and the user is a member of that group
         */

        // Condition 1:
        if (acl.hasPrivilege("dav:all", action)
                && (PrivilegeDefinition.READ.equals(action) 
                        || PrivilegeDefinition.CUSTOM_PRIVILEGE_READ_PROCESSED
                        .equals(action))) {
            return;
        }

        // If not condition 1 - needs to be authenticated
        if (principal == null) {
            throw new AuthenticationException();
        }

        // Condition 2:
        if (acl.hasPrivilege("dav:authenticated", action)) {
            return;
        }

        // Condition 3a:
        if (roleManager.hasRole(principal.getQualifiedName(), RoleManager.ROOT)) {
            return;
        }

        // Condition 3b:
        if (PrivilegeDefinition.READ.equals(action)
                && roleManager.hasRole(principal.getQualifiedName(),
                        RoleManager.READ_EVERYTHING)) {
            return;
        }

        // Condition 4a:
        // if (resource.getOwner().equals(principal.getQualifiedName())) {
        // if (action.equals(PrivilegeDefinition.READ) ||
        // action.equals(PrivilegeDefinition.WRITE) ||
        // action.equals(PrivilegeDefinition.WRITE_ACL)) {
        // return;
        // }
        // }
        // Dont't need to test the remaining conditions if (principalList ==
        // null)
        if (principalList == null) {
            throw new AuthorizationException();
        }

        if (resource.getOwner().equals(principal.getQualifiedName())) {
            if (acl.hasPrivilege("dav:owner",action)) {
                return;
            }
        }

        // Condition 5:
        if (acl.hasPrivilege(principal.getQualifiedName(), action)) {
            return;
        }

        // Condition 6:
        if (groupMatch(principalList, principal)) {
            return;
        }

        throw new AuthorizationException();
    }

    /**
     * Adds root and read everything roles to <code>Acl</code>
     * 
     * @param originalACL an <code>Acl</code>
     * @return a new <code>Acl</code>
     */
    public void addRolesToAcl(Acl acl) throws CloneNotSupportedException {

        List rootPrincipals = roleManager.listPrincipals(RoleManager.ROOT);

        for (Iterator i = rootPrincipals.iterator(); i.hasNext();) {
            String root = (String) i.next();
            String[] rootPriv = getRootPrivileges();
            for (int j = 0; j < rootPriv.length; j++) {
                acl.addEntry(rootPriv[j], root, false);
            }
        }

        List readPrincipals = roleManager.listPrincipals(RoleManager.READ_EVERYTHING);

        for (Iterator i = readPrincipals.iterator(); i.hasNext();) {
            String read = (String) i.next();
            String[] readPriv = getReadPrivileges();
            for (int j = 0; j < readPriv.length; j++) {
                acl.addEntry(readPriv[j], read, false);
            }
        }

    }

    
    private String[] getRootPrivileges() {

        String[] rootPrivs = new String[3];

        rootPrivs[0] = PrivilegeDefinition.READ;
        rootPrivs[1] = PrivilegeDefinition.WRITE;
        rootPrivs[2] = PrivilegeDefinition.WRITE_ACL;

        return rootPrivs;
    }

    private String[] getReadPrivileges() {
        String[] readPrivs = new String[1];

        readPrivs[0] = PrivilegeDefinition.READ;

        return readPrivs;
    }

    private boolean groupMatch(List principalList, Principal principal) {

        for (Iterator i = principalList.iterator(); i.hasNext();) {
            ACLPrincipal p = (ACLPrincipal) i.next();

            if (p.isGroup()) {
                if (principalManager.isMember(principal, p.getUrl())) {
                    return true;
                }
            }
        }

        return false;
    }
    
    public void setPrincipalManager(PrincipalManager principalManager) {
        this.principalManager = principalManager;
    }

    public void setRoleManager(RoleManager roleManager) {
        this.roleManager = roleManager;
    }

}
