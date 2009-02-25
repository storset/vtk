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
package org.vortikal.repo2;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import org.vortikal.repository.Acl;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.Privilege;
import org.vortikal.repository.ReadOnlyException;
import org.vortikal.repository.RepositoryAction;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceLockedException;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalFactory;
import org.vortikal.security.PrincipalManager;
import org.vortikal.security.roles.RoleManager;

/**
 * Manager for authorizing principals at specific authorization level.
 */
public class AuthorizationManager {

    private RoleManager roleManager;
    private PrincipalManager principalManager;
    private PrincipalFactory principalFactory;
//    private LockManager lockManager;
    private NodeStore nodeStore;
    
    private boolean readOnly = false;

    public boolean isReadOnly() {
        return this.readOnly;
    }
    
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    /**
     * Authorizes a principal for a root role action. Should throw an
     * AuthorizationException if the principal in question does not
     * have root privileges.
     */
    public void authorizeRootRoleAction(Principal principal) throws AuthorizationException {
        if (!this.roleManager.hasRole(principal, RoleManager.ROOT)) {
            throw new AuthorizationException(
                "Principal '" + principal
                + "' not authorized to perform repository administration");
        }
    }
    
    private void checkReadOnly(Principal principal) throws ReadOnlyException {
        
        if (isReadOnly() && !this.roleManager.hasRole(principal, 
                RoleManager.ROOT)) {
            throw new ReadOnlyException();
        }
    }

    /**
     * Authorizes a principal for a given action on a resource
     * URI. Equivalent to calling one of the <code>authorizeYYY(uri,
     * principal)</code> methods of this interface (with
     * <code>YYY</code> mapping to one of the actions).
     *
     * @param node a resource URI
     * @param action the action to perform. One of the action types
     * defined in {@link #ACTION_AUTHORIZATIONS}.
     * @param principal the principal performing the action
     */
//    public void authorizeAction(Resource node, RepositoryAction action, 
//            Principal principal) throws AuthenticationException, AuthorizationException,
//            ResourceLockedException, IOException {
//
//        if (!RepositoryAction.REPOSITORY_ACTION_SET.contains(action)
//                || RepositoryAction.COPY.equals(action)
//                || RepositoryAction.MOVE.equals(action)) {
//            throw new IllegalArgumentException(
//                "Unable to authorize for action " + action
//                + ": must be one of (except COPY/MOVE) " + RepositoryAction.REPOSITORY_ACTION_SET);
//        }
//
//        if (RepositoryAction.UNEDITABLE_ACTION.equals(action)) {
//            throw new AuthorizationException("Uneditable");
//        } else if (RepositoryAction.READ_PROCESSED.equals(action)) {
//            authorizeReadProcessed(node, principal);
//
//        } else if (RepositoryAction.READ.equals(action)) {
//            authorizeRead(node, principal);
//
//        } else if (RepositoryAction.CREATE.equals(action)) {
//            authorizeCreate(node, principal);
//
//        } else if (RepositoryAction.WRITE.equals(action)) {
//            authorizeWrite(node, principal);
//
//        } else if (RepositoryAction.EDIT_COMMENT.equals(action)) {
//            authorizeEditComment(node, principal);
//
//        } else if (RepositoryAction.ADD_COMMENT.equals(action)) {
//            authorizeAddComment(node, principal);
//
//        } else if (RepositoryAction.WRITE_ACL.equals(action) ||
//                RepositoryAction.ALL.equals(action)) {
//            authorizeAll(node, principal);
//
//        } else if (RepositoryAction.UNLOCK.equals(action)) {
//            authorizeUnlock(node, principal);
//
//        } else if (RepositoryAction.DELETE.equals(action)) {
//            authorizeDelete(node, principal);
//
//        } else if (RepositoryAction.REPOSITORY_ADMIN_ROLE_ACTION.equals(action)) {
//            authorizePropertyEditAdminRole(node, principal);
//
//        } else if (RepositoryAction.REPOSITORY_ROOT_ROLE_ACTION.equals(action)) {
//            authorizePropertyEditRootRole(node, principal);
//        }
//    }
    
    
    private static final RepositoryAction[] READ_PROCESSED_AUTH_PRIVILEGES = 
        new RepositoryAction[] {Privilege.ALL, Privilege.READ, Privilege.READ_PROCESSED};

    /**
     * <ul>
     *   <li>Privilege READ_PROCESSED, READ or ALL in ACL
     *   <li>Role ROOT or READ_EVERYTHING
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizeReadProcessed(Resource node, Principal principal) 
        throws AuthenticationException, AuthorizationException, IOException {
        if (this.roleManager.hasRole(principal, RoleManager.ROOT) ||
                this.roleManager.hasRole(principal, RoleManager.READ_EVERYTHING))
            return;
        aclAuthorize(principal, node, READ_PROCESSED_AUTH_PRIVILEGES);
    }

    

    private static final RepositoryAction[] READ_AUTH_PRIVILEGES = 
        new RepositoryAction[] {Privilege.ALL, Privilege.READ};

    /**
     * <ul>
     *   <li>Privilege READ or ALL in ACL
     *   <li>Role ROOT or READ_EVERYTHING
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizeRead(Resource node, Principal principal) 
        throws AuthenticationException, AuthorizationException,
        ResourceLockedException, IOException {

//        ResourceImpl resource = this.dao.load(uri);

        if (this.roleManager.hasRole(principal, RoleManager.ROOT) ||
                this.roleManager.hasRole(principal, RoleManager.READ_EVERYTHING))
            return;
        
        aclAuthorize(principal, node, READ_AUTH_PRIVILEGES);
    }


    private static final RepositoryAction[] CREATE_AUTH_PRIVILEGES = 
        new RepositoryAction[] {Privilege.ALL, Privilege.WRITE, Privilege.BIND};

    /**
     * <ul>
     *   <li>Privilege BIND, WRITE or ALL on resource
     *   <li>Role ROOT
     *   <li>+ parent not locked by another principal
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizeCreate(Resource node, Principal principal)
        throws AuthenticationException, AuthorizationException, ReadOnlyException, 
        ResourceLockedException, IOException {

        checkReadOnly(principal);

//        this.lockManager.lockAuthorize(resource, principal, false);
        
        if (this.roleManager.hasRole(principal, RoleManager.ROOT))
            return;
        
        aclAuthorize(principal, node, CREATE_AUTH_PRIVILEGES);
    }
    

    private static final RepositoryAction[] WRITE_AUTH_PRIVILEGES = 
        new RepositoryAction[] {Privilege.ALL, Privilege.WRITE};

    /**
     * <ul>
     *   <li>Privilege WRITE or ALL in ACL
     *   <li>Role ROOT
     *   <li>+ resource not locked by another principal
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizeWrite(Resource node, Principal principal)
        throws AuthenticationException, AuthorizationException, ReadOnlyException,
        ResourceLockedException, IOException {

        checkReadOnly(principal);

//        this.lockManager.lockAuthorize(resource, principal, false);

        if (this.roleManager.hasRole(principal, RoleManager.ROOT))
            return;
        
        aclAuthorize(principal, node, WRITE_AUTH_PRIVILEGES);
    }
    

    private static final RepositoryAction[] ADD_COMMENT_AUTH_PRIVILEGES = 
        new RepositoryAction[] {Privilege.ADD_COMMENT};

    /**
     * <ul>
     *   <li>Privilege ADD_COMMENT in ACL
     *   <li>+ resource not locked by another principal
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizeAddComment(Resource node, Principal principal)
        throws AuthenticationException, AuthorizationException, ReadOnlyException,
        ResourceLockedException, IOException {

        checkReadOnly(principal);

//        this.lockManager.lockAuthorize(resource, principal, false);

        if (this.roleManager.hasRole(principal, RoleManager.ROOT))
            return;
        
        aclAuthorize(principal, node, ADD_COMMENT_AUTH_PRIVILEGES);
    }
    


    private static final RepositoryAction[] EDIT_COMMENT_AUTH_PRIVILEGES = 
        new RepositoryAction[] {Privilege.ALL, Privilege.WRITE};

    /**
     * <ul>
     *   <li>Privilege WRITE or ALL in ACL
     *   <li>Role ROOT
     *   <li>+ resource not locked by another principal
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizeEditComment(Resource node, Principal principal)
        throws AuthenticationException, AuthorizationException, ReadOnlyException,
        ResourceLockedException, IOException {

        checkReadOnly(principal);

//        this.lockManager.lockAuthorize(resource, principal, false);

        if (this.roleManager.hasRole(principal, RoleManager.ROOT))
            return;
        
        aclAuthorize(principal, node, EDIT_COMMENT_AUTH_PRIVILEGES);
    }
    



    private static final RepositoryAction[] ALL_AUTH_PRIVILEGES = 
        new RepositoryAction[] {Privilege.ALL};


    /**
     * <ul>
     *   <li>Privilege ALL in ACL
     *   <li>Role ROOT
     *   <li>+ resource not locked by another principal
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizeAll(Resource node, Principal principal)
        throws AuthenticationException, AuthorizationException, ReadOnlyException,
        ResourceLockedException, IOException {

        checkReadOnly(principal);
        
//        this.lockManager.lockAuthorize(resource, principal, false);
        
        if (this.roleManager.hasRole(principal, RoleManager.ROOT))
            return;
        
        aclAuthorize(principal, node, ALL_AUTH_PRIVILEGES);
    }
    


    
    private static final RepositoryAction[] UNLOCK_AUTH_PRIVILEGES = 
        new RepositoryAction[] {Privilege.ALL, Privilege.WRITE};


    /**
     * <ul>
     *   <li>privilege WRITE or ALL in Acl + resource not locked by another principal
     *   <li>Role ROOT
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizeUnlock(Resource node, Principal principal) 
        throws AuthenticationException, AuthorizationException, ReadOnlyException,
        ResourceLockedException, IOException {

        checkReadOnly(principal);
        
        if (this.roleManager.hasRole(principal, RoleManager.ROOT))
            return;
        
//        this.lockManager.lockAuthorize(resource, principal, false);

        aclAuthorize(principal, node, UNLOCK_AUTH_PRIVILEGES);
    }


    private static final RepositoryAction[] DELETE_AUTH_PRIVILEGES = 
        new RepositoryAction[] {Privilege.ALL};


    /**
     * <ul>
     *   <li>Privilege ALL in ACL + parent not locked
     *   <li>Action WRITE on parent
     *   <li>+ resource tree not locked by another principal
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizeDelete(Resource node, Resource parent, Principal principal) 
        throws AuthenticationException, AuthorizationException, ReadOnlyException, 
        ResourceLockedException, IOException {

        checkReadOnly(principal);

        try {
            authorizeWrite(parent, principal);
            return;
        } catch (Exception e) {
            // Continue..
        }
        aclAuthorize(principal, node, DELETE_AUTH_PRIVILEGES);
    }
    

    private static final RepositoryAction[] ADMIN_AUTH_PRIVILEGES = 
        new RepositoryAction[] {Privilege.ALL};


    /**
     * All of:
     * <ul>
     *   <li>Action WRITE
     *   <li>Action ALL or role ROOT
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizePropertyEditAdminRole(Resource node, Principal principal) 
        throws AuthenticationException, AuthorizationException,
        ResourceLockedException, IOException {
        if (principal == null) {
            throw new AuthorizationException(
                "NULL principal not authorized to edit properties using admin privilege ");
        }
        
        if (this.roleManager.hasRole(principal, RoleManager.ROOT)) {
            return;
        }

//        Resource resource = this.dao.load(uri);
        aclAuthorize(principal, node, ADMIN_AUTH_PRIVILEGES);
        authorizeWrite(node, principal);
    }


    /**
     * All of:
     * <ul>
     *   <li>Action WRITE
     *   <li>Role ROOT
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizePropertyEditRootRole(Resource node, Principal principal)
        throws AuthenticationException, AuthorizationException,
        ResourceLockedException, IOException {

        if (!this.roleManager.hasRole(principal, RoleManager.ROOT))
            throw new AuthorizationException();
        
        authorizeWrite(node, principal);
    }
    
    

    /**
     * All of:
     * <ul>
     *   <li>COPY action
     *   <li>Action DELETE on source
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizeMove(Resource srcNode, Resource srcParent, 
            Resource destParent, Principal principal, boolean deleteDestination) 
        throws AuthenticationException, AuthorizationException, ReadOnlyException,
        ResourceLockedException, IOException {

        checkReadOnly(principal);

        if (this.roleManager.hasRole(principal, RoleManager.ROOT))
            return;
        authorizeRead(srcNode, principal);
        authorizeCreate(destParent, principal);
        authorizeDelete(srcNode, srcParent, principal);
    }

    
    /**
     * A principal is granted access if one of these conditions are met for one
     * of the privileges supplied:
     * 
     * <p>1) (ALL, privilege) is present in the resource's ACL.<br> 
     * NOTE: This is limited to read privileges
     * 
     * <p>The rest requires that the user is authenticated:
     * 
     * <p>2) principal != null and (AUTHENTICATED, privilege) is present
     * 
     * <p>The rest is meaningless if principalList == null:
     * 
     * <p>3) Principal is resource owner and (OWNER, privilege) is present in
     * the resource's ACL
     * 
     * <p>4) (principal, privilege) is present in the resource's ACL
     * 
     * <p>5) (g, privilege) is present in the resource's ACL, where g is a group
     * identifier and the user is a member of that group
     **/
    private void aclAuthorize(Principal principal, Resource node, RepositoryAction[] privileges) 
        throws AuthenticationException, AuthorizationException {

        Acl acl = node.getAcl();
        Principal owner = node.getOwner();
        for (int i = 0; i < privileges.length; i++) {
            RepositoryAction privilege = privileges[i];
            Set<Principal> principalSet = acl.getPrincipalSet(privilege);
            
            // Dont't need to test the conditions if (principalSet == null)
            if (principalSet == null || principalSet.size() == 0) {
                continue;
            }

            // Condition 1:
            if (principalSet.contains(PrincipalFactory.ALL)) {
                return;
            }

            // If not condition 1 - needs to be authenticated
            if (principal == null) {
                continue;
            }

            // Condition 2:
            if (principalSet.contains(PrincipalFactory.AUTHENTICATED)) {
                return;
            }

            // Condition 3:
            if (owner.equals(principal)
                && principalSet.contains(PrincipalFactory.OWNER)) {
                return;
            }

            // Condition 4:

            if (principalSet.contains(principal)) {
                return;
            }
        }

        // At this point a principal should always be available:
        if (principal == null) throw new AuthenticationException();
            
        for (int i = 0; i < privileges.length; i++) {
            RepositoryAction action = privileges[i];
            Set<Principal> principalSet = acl.getPrincipalSet(action);
            
            // Condition 5:
            if (groupMatch(principalSet, principal)) {
                return;
            }
        }
       throw new AuthorizationException("principal: " + principal 
               + ", node: " + node.getURI() 
               + ", privs: " + Arrays.asList(privileges));
    }
    

    private boolean groupMatch(Set<Principal> principalList, Principal principal) {

        for (Principal p: principalList) {

            if (p.getType() == Principal.Type.GROUP) {
                if (this.principalManager.isMember(principal, p)) {
                    return true;
                }
            }
        }
        return false;
    }


    public void setNodeStore(NodeStore nodeStore) {
        this.nodeStore = nodeStore;
    }

    public void setPrincipalManager(PrincipalManager principalManager) {
        this.principalManager = principalManager;
    }

    public void setPrincipalFactory(PrincipalFactory principalFactory) {
        this.principalFactory = principalFactory;
    }

    public void setRoleManager(RoleManager roleManager) {
        this.roleManager = roleManager;
    }

//    public void setLockManager(LockManager lockManager) {
//        this.lockManager = lockManager;
//    }

    
}
