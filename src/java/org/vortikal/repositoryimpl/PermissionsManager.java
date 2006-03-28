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
import java.util.Set;

import org.vortikal.repository.Acl;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.IllegalOperationException;
import org.vortikal.repository.PrivilegeDefinition;
import org.vortikal.repositoryimpl.dao.DataAccessor;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalManager;
import org.vortikal.security.roles.RoleManager;

public class PermissionsManager {

    private RoleManager roleManager;
    private PrincipalManager principalManager;
    private DataAccessor dao;
    
    public void authorizeRecursively(ResourceImpl resource, Principal principal,
            String privilege) throws IOException, AuthenticationException,
            AuthorizationException {

        authorize(resource.getAcl(), principal, privilege);
        if (resource.isCollection()) {
            String[] uris = this.dao.discoverACLs(resource);
            for (int i = 0; i < uris.length; i++) {
                ResourceImpl ancestor = this.dao.load(uris[i]);
                authorize(ancestor.getAcl(), principal, privilege);
            }
        }
    }


    
    public void authorize(Acl acl, Principal principal, String action)
            throws AuthenticationException, AuthorizationException, IOException {

        /*
         * Special treatment for uio:read-processed needed: dav:read also grants
         * uio:read-processed
         */
        if (action.equals(PrivilegeDefinition.READ_PROCESSED)) {
            try {
                authorize(acl, principal, PrivilegeDefinition.READ);

                return;
            } catch (AuthenticationException e) {
                /* Handle below */
            } catch (AuthorizationException e) {
                /* Handle below */
            }
        }

        Set principalSet = acl.getPrincipalSet(action);

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
        Principal p = principalManager.getPseudoPrincipal(Principal.NAME_PSEUDO_ALL);
        if (acl.hasPrivilege(p, action)) {
            // XXX: removed this:
//            && (PrivilegeDefinition.READ.equals(action) || 
//                    PrivilegeDefinition.READ_PROCESSED.equals(action))
            return;
        }

        // If not condition 1 - needs to be authenticated
        if (principal == null) {
            throw new AuthenticationException();
        }

        // Condition 2:
        p = principalManager.getPseudoPrincipal(Principal.NAME_PSEUDO_AUTHENTICATED);
        if (acl.hasPrivilege(p, action)) {
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
        if (principalSet == null) {
            throw new AuthorizationException();
        }

        p = principalManager.getPseudoPrincipal(Principal.NAME_PSEUDO_OWNER);
        if (acl.getOwner().equals(principal) && acl.hasPrivilege(p,action)) {
            return;
        }

        // Condition 5:
        if (acl.hasPrivilege(principal, action)) {
            return;
        }

        // Condition 6:
        if (groupMatch(principalSet, principal)) {
            return;
        }

        throw new AuthorizationException();
    }

    private boolean groupMatch(Set principalList, Principal principal) {

        for (Iterator i = principalList.iterator(); i.hasNext();) {
            Principal p = (Principal) i.next();

            if (p.getType() == Principal.TYPE_GROUP) {
                if (principalManager.isMember(principal, p.getQualifiedName())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks the validity of an ACL.
     *
     * @param aceList an <code>Ace[]</code> value
     * @exception AclException if an error occurs
     * @exception IllegalOperationException if an error occurs
     * @exception IOException if an error occurs
     */
    public void validateACL(Acl acl)
        throws AclException, IllegalOperationException {
        /*
         * Enforce ((dav:owner (dav:read dav:write dav:write-acl))
         */
        Principal p = principalManager.getPseudoPrincipal(Principal.NAME_PSEUDO_OWNER);
        if (!acl.hasPrivilege(p, PrivilegeDefinition.WRITE)) {
            throw new IllegalOperationException(
                "Owner must be granted write privilege in ACL.");
        }

        if (!acl.hasPrivilege(p, PrivilegeDefinition.READ)) {
            throw new IllegalOperationException(
                "Owner must be granted read privilege in ACL.");
        }

        if (!acl.hasPrivilege(p, PrivilegeDefinition.WRITE_ACL)) {
            throw new IllegalOperationException(
                "Owner must be granted write-acl privilege in ACL.");
        }
        
        p = principalManager.getPseudoPrincipal(Principal.NAME_PSEUDO_ALL);
        if (acl.hasPrivilege(p, PrivilegeDefinition.WRITE)) {
            throw new IllegalOperationException(
            "'All users' isn't allowed write privilege in ACL.");
        }
        
        if (acl.hasPrivilege(p, PrivilegeDefinition.WRITE_ACL)) {
            throw new IllegalOperationException(
            "'All users' isn't allowed write-acl privilege in ACL.");
        }

        /*
         * Walk trough the ACL, for every ACE, enforce that:
         * 1) Every principal is valid
         * 2) Every privilege has a supported namespace and name
         */

//        for (int i = 0; i < acl.length; i++) {
//            Ace ace = acl[i];
//
//            org.vortikal.repositoryimpl.ACLPrincipal principal = ace.getPrincipal();
//
//            if (principal.getType() == org.vortikal.repositoryimpl.ACLPrincipal.TYPE_URL) {
//                boolean validPrincipal = false;
//
//                if (principal.isUser()) {
//                    Principal p = null;
//                    try {
//                        p = principalManager.getPrincipal(principal.getURL());
//                    } catch (InvalidPrincipalException e) {
//                        throw new AclException("Invalid principal '" 
//                                + principal.getURL() + "' in ACL");
//                    }
//                    validPrincipal = principalManager.validatePrincipal(p);
//                } else {
//                    validPrincipal = principalManager.validateGroup(principal.getURL());
//                }
//
//                if (!validPrincipal) {
//                    throw new AclException(AclException.RECOGNIZED_PRINCIPAL,
//                        "Unknown principal: " + principal.getURL());
//                }
//            } else {
//                if ((principal.getType() != org.vortikal.repositoryimpl.ACLPrincipal.TYPE_ALL) &&
//                        (principal.getType() != org.vortikal.repositoryimpl.ACLPrincipal.TYPE_OWNER) &&
//                        (principal.getType() != org.vortikal.repositoryimpl.ACLPrincipal.TYPE_AUTHENTICATED)) {
//                    throw new AclException(AclException.RECOGNIZED_PRINCIPAL,
//                        "Allowed principal types are " +
//                        "either TYPE_ALL, TYPE_OWNER " + "OR  TYPE_URL.");
//                }
//            }
//
//            Privilege[] privileges = ace.getPrivileges();
//
//            for (int j = 0; j < privileges.length; j++) {
//                Privilege privilege = privileges[j];
//
//                if (privilege.getNamespace().equals(Namespace.STANDARD_NAMESPACE)) {
//                    if (!(privilege.getName().equals(PrivilegeDefinition.WRITE) ||
//                            privilege.getName().equals(PrivilegeDefinition.READ) ||
//                            privilege.getName().equals(PrivilegeDefinition.WRITE_ACL))) {
//                        throw new AclException(AclException.NOT_SUPPORTED_PRIVILEGE,
//                            "Unsupported privilege name: " +
//                            privilege.getName());
//                    }
//                } else if (privilege.getNamespace().equals(Namespace.CUSTOM_NAMESPACE)) {
//                    if (!(privilege.getName().equals(PrivilegeDefinition.READ_PROCESSED))) {
//                        throw new AclException(AclException.NOT_SUPPORTED_PRIVILEGE,
//                            "Unsupported privilege name: " +
//                            privilege.getName());
//                    }
//                } else {
//                    throw new AclException(AclException.NOT_SUPPORTED_PRIVILEGE,
//                        "Unsupported privilege namespace: " +
//                        privilege.getNamespace());
//                }
//            }
//        }
    }

    
    
    
    public void setPrincipalManager(PrincipalManager principalManager) {
        this.principalManager = principalManager;
    }

    public void setRoleManager(RoleManager roleManager) {
        this.roleManager = roleManager;
    }

    public void setDao(DataAccessor dao) {
        this.dao = dao;
    }

}
