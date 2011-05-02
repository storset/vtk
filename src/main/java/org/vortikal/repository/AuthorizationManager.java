/* Copyright (c) 2006, 2007, University of Oslo, Norway
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
package org.vortikal.repository;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import org.vortikal.repository.store.DataAccessor;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalFactory;
import org.vortikal.security.PrincipalManager;
import org.vortikal.security.roles.RoleManager;

/**
 * Manager for authorizing principals at specific authorization level.
 */
public final class AuthorizationManager {

    private RoleManager roleManager;
    private PrincipalManager principalManager;
    private DataAccessor dao;
    
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
        if (!this.roleManager.hasRole(principal, RoleManager.Role.ROOT)) {
            throw new AuthorizationException(
                "Principal '" + principal
                + "' not authorized to perform repository administration");
        }
    }
    
    private void checkReadOnly(Principal principal) throws ReadOnlyException {
        
        if (isReadOnly() && !this.roleManager.hasRole(principal, 
                RoleManager.Role.ROOT)) {
            throw new ReadOnlyException();
        }
    }

    /**
     * Authorizes a principal for a given action on a resource
     * URI. Equivalent to calling one of the <code>authorizeYYY(uri,
     * principal)</code> methods of this interface (with
     * <code>YYY</code> mapping to one of the actions).
     *
     * @param uri a resource URI
     * @param action the action to perform. One of the action types
     * defined in {@link #ACTION_AUTHORIZATIONS}.
     * @param principal the principal performing the action
     */
    public void authorizeAction(Path uri, RepositoryAction action, 
            Principal principal) throws AuthenticationException, AuthorizationException,
            IOException {
        if (uri == null) {
            throw new IllegalArgumentException("URI cannot be NULL");
        }
        if (action == null) {
            throw new IllegalArgumentException("Action cannot be NULL");
        }
        if (RepositoryAction.COPY == action || RepositoryAction.MOVE == action) {
            throw new IllegalArgumentException(
                    "Unable to authorize for COPY/MOVE actions");
        }
        
        switch (action) {
        case UNEDITABLE_ACTION:
            throw new AuthorizationException(uri + ": uneditable");
        case READ_PROCESSED:
            authorizeReadProcessed(uri, principal);
            break;
        case READ:
            authorizeRead(uri, principal);
            break;
        case CREATE:
            authorizeCreate(uri, principal);
            break;
        case WRITE:
        case READ_WRITE:
            authorizeReadWrite(uri, principal);
            break;
        case EDIT_COMMENT:
            authorizeEditComment(uri, principal);
            break;
        case ADD_COMMENT:
            authorizeAddComment(uri, principal);
            break;
        case ALL:
        case WRITE_ACL:
            authorizeAll(uri, principal);
            break;
        case UNLOCK:
            authorizeUnlock(uri, principal);
            break;
        case DELETE:
            authorizeDelete(uri, principal);
            break;
        case REPOSITORY_ADMIN_ROLE_ACTION:
            authorizePropertyEditAdminRole(uri, principal);
            break;
        case REPOSITORY_ROOT_ROLE_ACTION:
            authorizePropertyEditRootRole(uri, principal);
            break;
            default:
                throw new IllegalArgumentException("Cannot authorize action " + action);
        }
    }
    
    
    private static final Privilege[] READ_PROCESSED_AUTH_PRIVILEGES = 
        new Privilege[] {Privilege.ALL, Privilege.READ_WRITE, Privilege.READ, Privilege.READ_PROCESSED};

    /**
     * <ul>
     *   <li>Privilege READ_PROCESSED, READ, READ_WRITE or ALL in ACL
     *   <li>Role ROOT or READ_EVERYTHING
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizeReadProcessed(Path uri, Principal principal) 
        throws AuthenticationException, AuthorizationException, IOException, ResourceNotFoundException {

        ResourceImpl resource = loadResource(uri);

        if (this.roleManager.hasRole(principal, RoleManager.Role.ROOT) ||
                this.roleManager.hasRole(principal, RoleManager.Role.READ_EVERYTHING))
            return;

        aclAuthorize(principal, resource, READ_PROCESSED_AUTH_PRIVILEGES);
    }

    

    private static final Privilege[] READ_AUTH_PRIVILEGES = 
        new Privilege[] {Privilege.ALL, Privilege.READ_WRITE, Privilege.READ};

    /**
     * <ul>
     *   <li>Privilege READ, READ_WRITE or ALL in ACL
     *   <li>Role ROOT or READ_EVERYTHING
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizeRead(Path uri, Principal principal) 
        throws AuthenticationException, AuthorizationException,
        IOException, ResourceNotFoundException {

        ResourceImpl resource = loadResource(uri);

        if (this.roleManager.hasRole(principal, RoleManager.Role.ROOT) ||
                this.roleManager.hasRole(principal, RoleManager.Role.READ_EVERYTHING)) {
            return;
        }
        aclAuthorize(principal, resource, READ_AUTH_PRIVILEGES);
    }


    private static final Privilege[] CREATE_AUTH_PRIVILEGES = 
        new Privilege[] {Privilege.ALL, Privilege.READ_WRITE};

    /**
     * TODO: CREATE is used for old bind ("bare opprett") which is currently disabled
     *       by not having the privilege in CREATE_AUTH_PRIVILEGES. Needs other changes
     *       as part of VTK-2135 before it can be used again
     * <ul>
     *   <li>Privilege READ_WRITE or ALL on resource
     *   <li>Role ROOT
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizeCreate(Path uri, Principal principal)
        throws AuthenticationException, AuthorizationException, ReadOnlyException, 
        IOException, ResourceNotFoundException {

        checkReadOnly(principal);

        ResourceImpl resource = loadResource(uri);
        
        if (this.roleManager.hasRole(principal, RoleManager.Role.ROOT)) {
            return;
        }
        aclAuthorize(principal, resource, CREATE_AUTH_PRIVILEGES);
    }
    

    private static final Privilege[] READ_WRITE_AUTH_PRIVILEGES = 
        new Privilege[] {Privilege.ALL, Privilege.READ_WRITE};

    /**
     * <ul>
     *   <li>Privilege READ_WRITE or ALL in ACL
     *   <li>Role ROOT
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizeReadWrite(Path uri, Principal principal)
        throws AuthenticationException, AuthorizationException, ReadOnlyException,
        IOException, ResourceNotFoundException {

        checkReadOnly(principal);

        ResourceImpl resource = loadResource(uri);
        
        if (this.roleManager.hasRole(principal, RoleManager.Role.ROOT)) {
            return;
        }
        aclAuthorize(principal, resource, READ_WRITE_AUTH_PRIVILEGES);
    }
    

    private static final Privilege[] ADD_COMMENT_AUTH_PRIVILEGES = 
        new Privilege[] {Privilege.ALL, Privilege.READ_WRITE, Privilege.ADD_COMMENT};

    /**
     * <ul>
     *   <li>Privilege ALL, READ_WRITE or ADD_COMMENT in ACL
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizeAddComment(Path uri, Principal principal)
        throws AuthenticationException, AuthorizationException, ReadOnlyException,
        IOException, ResourceNotFoundException {

        checkReadOnly(principal);

        ResourceImpl resource = loadResource(uri);
        
        if (this.roleManager.hasRole(principal, RoleManager.Role.ROOT)) {
            return;
        }
        aclAuthorize(principal, resource, ADD_COMMENT_AUTH_PRIVILEGES);
    }
    


    private static final Privilege[] EDIT_COMMENT_AUTH_PRIVILEGES = 
        new Privilege[] {Privilege.ALL};

    /**
     * <ul>
     *   <li>Privilege WRITE or ALL in ACL
     *   <li>Role ROOT
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizeEditComment(Path uri, Principal principal)
        throws AuthenticationException, AuthorizationException, ReadOnlyException,
        IOException, ResourceNotFoundException {

        checkReadOnly(principal);

        ResourceImpl resource = loadResource(uri);
        
        if (this.roleManager.hasRole(principal, RoleManager.Role.ROOT)) {
            return;
        }
        aclAuthorize(principal, resource, EDIT_COMMENT_AUTH_PRIVILEGES);
    }
    



    private static final Privilege[] ALL_AUTH_PRIVILEGES = 
        new Privilege[] {Privilege.ALL};


    /**
     * <ul>
     *   <li>Privilege ALL in ACL
     *   <li>Role ROOT
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizeAll(Path uri, Principal principal)
        throws AuthenticationException, AuthorizationException, ReadOnlyException,
        IOException, ResourceNotFoundException {

        checkReadOnly(principal);
        
        ResourceImpl resource = loadResource(uri);
        
        if (this.roleManager.hasRole(principal, RoleManager.Role.ROOT)) {
            return;
        }
        aclAuthorize(principal, resource, ALL_AUTH_PRIVILEGES);
    }
    


    
    private static final Privilege[] UNLOCK_AUTH_PRIVILEGES = 
        new Privilege[] {Privilege.ALL};


    /**
     * <ul>
     *   <li>principal owns lock
     *   <li>privilege ALL in Acl 
     *   <li>Role ROOT
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizeUnlock(Path uri, Principal principal) 
        throws AuthenticationException, AuthorizationException, ReadOnlyException,
        IOException, ResourceNotFoundException {

        checkReadOnly(principal);
        
        if (this.roleManager.hasRole(principal, RoleManager.Role.ROOT)) {
            return;
        }

        ResourceImpl resource = loadResource(uri);

        Lock lock = resource.getLock();
        if (lock == null) {
            return;
        }
        if (principal == null) {
            throw new AuthenticationException();
        }
        if (lock.getPrincipal().equals(principal)) {
            return;
        }

        aclAuthorize(principal, resource, UNLOCK_AUTH_PRIVILEGES);
    }


    private static final Privilege[] DELETE_AUTH_PRIVILEGES = 
        new Privilege[] {Privilege.ALL};


    /**
     * <ul>
     *   <li>Privilege ALL in ACL
     *   <li>Action READ_WRITE on parent
     *   <li>Resource is not the root resource '/'.
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizeDelete(Path uri, Principal principal) 
        throws AuthenticationException, AuthorizationException, ReadOnlyException, 
        IOException, ResourceNotFoundException {

        checkReadOnly(principal);

        if (uri.isRoot()) {
            // Not allowed to delete root resource.
            // Avoid sending null as Path to DAO layer (uri.getParent() below ..),
            // which results in a NullPointerException in Cache, hidden by catch(Exception) below.
            throw new AuthorizationException("Not allowed to delete root resource");
        }
        
        Resource resource = loadResource(uri);

        // Delete is authorized if either of these conditions hold:
        try {
            // 1. Principal has write permission on the parent resource, or
            authorizeReadWrite(uri.getParent(), principal);
            return;
        } catch (AuthorizationException e) {
            // Continue to #2
        }
        // 2. Principal has delete permission directly on the resource itself
        aclAuthorize(principal, resource, DELETE_AUTH_PRIVILEGES);
    }
    

    private static final Privilege[] ADMIN_AUTH_PRIVILEGES = 
        new Privilege[] {Privilege.ALL};


    /**
     * All of:
     * <ul>
     *   <li>Action ALL or role ROOT
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizePropertyEditAdminRole(Path uri, Principal principal) 
        throws AuthenticationException, AuthorizationException,
        IOException, ResourceNotFoundException {
        if (principal == null) {
            throw new AuthorizationException(
                "NULL principal not authorized to edit properties using admin privilege ");
        }
        
        if (this.roleManager.hasRole(principal, RoleManager.Role.ROOT)) {
            return;
        }

        Resource resource = loadResource(uri);
        aclAuthorize(principal, resource, ADMIN_AUTH_PRIVILEGES);
        authorizeReadWrite(uri, principal);
        
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
    public void authorizePropertyEditRootRole(Path uri, Principal principal)
        throws AuthenticationException, AuthorizationException,
        IOException {

        if (!this.roleManager.hasRole(principal, RoleManager.Role.ROOT)) {
            throw new AuthorizationException();
        }
        authorizeReadWrite(uri, principal);
        
    }
    
    /**
     * All of:
     * <ul>
     *   <li>Action READ on source tree
     *   <li>Action CREATE on destination
     *   <li>if overwrite, action DELETE on dest
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizeCopy(Path srcUri, Path destUri, 
            Principal principal, boolean deleteDestination) 
        throws AuthenticationException, AuthorizationException, ReadOnlyException,
        IOException, ResourceNotFoundException {

        checkReadOnly(principal);

        authorizeRead(srcUri, principal);

        Resource resource = loadResource(srcUri);

        if (resource.isCollection()) {
            Path[] uris = this.dao.discoverACLs(srcUri);
            for (int i = 0; i < uris.length; i++) {
                authorizeRead(uris[i], principal);
            }
        }

        Path destParentUri = destUri.getParent();
        authorizeCreate(destParentUri, principal);

        if (deleteDestination) authorizeDelete(destUri, principal);
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
    public void authorizeMove(Path srcUri, Path destUri,
            Principal principal, boolean deleteDestination) 
        throws AuthenticationException, AuthorizationException, ReadOnlyException,
        IOException {

        checkReadOnly(principal);

        authorizeDelete(srcUri, principal);

        boolean srcHasACLs = (this.dao.discoverACLs(srcUri).length > 0);
        if (srcHasACLs) {
            /* src has ACLs and move will therefore impact ACLs of sutbree rooted 
             * at destination.
             * Therefore require all privilege at destination (which would be required to 
             * create similar subtree at destination using other operations).
             * 
             *  All on destParentUri implies delete on destUri
             */
            
            Path destParentUri = destUri.getParent();
            authorizeAll(destParentUri, principal);                  
        } else {
            /* Source does not contain ACLs. 
             * Only need create (and possibly delete).
             */
            Path destParentUri = destUri.getParent();
            authorizeCreate(destParentUri, principal);
    
            if (deleteDestination) authorizeDelete(destUri, principal);
        }
        
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
     * <p>The rest is meaningless if principalList == null:
     * 
     * <p>2) (principal, privilege) is present in the resource's ACL
     * 
     * <p>3) (g, privilege) is present in the resource's ACL, where g is a group
     * identifier and the user is a member of that group
     **/
    private void aclAuthorize(Principal principal, Resource resource, Privilege[] privileges) 
        throws AuthenticationException, AuthorizationException {
        
        Acl acl = resource.getAcl();

        for (int i = 0; i < privileges.length; i++) {
            Privilege privilege = privileges[i];
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

            if (principalSet.contains(principal)) {
                return;
            }
        }

        // At this point a principal should always be available:
        if (principal == null) {
            throw new AuthenticationException(
                    "Principal NULL not authorized to access " 
                    + resource.getURI() + " for privilege(s) " 
                    + Arrays.asList(privileges));
        }

        for (int i = 0; i < privileges.length; i++) {
            Privilege action = privileges[i];
            Set<Principal> principalSet = acl.getPrincipalSet(action);
            
            // Condition 3:
            if (groupMatch(principalSet, principal)) {
                return;
            }
        }
        throw new AuthorizationException(
                "Principal " + principal + " not authorized to access " 
                + resource.getURI() + " for privilege(s) " 
                + Arrays.asList(privileges));
    }
    
    // Temporary fix for problems with DAO returning null for resources not found
    // and this class does not handle it. Throw ResourceNotFoundException(Path) instead
    // of NullPointerException.
    private ResourceImpl loadResource(Path uri) 
        throws ResourceNotFoundException {
        ResourceImpl resource = this.dao.load(uri);
        if (resource == null) {
            throw new ResourceNotFoundException(uri);
        }
        return resource;
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
