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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.vortikal.repository.Acl;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.Privilege;
import org.vortikal.repository.ReadOnlyException;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceLockedException;
import org.vortikal.repositoryimpl.dao.DataAccessor;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalManager;
import org.vortikal.security.PseudoPrincipal;
import org.vortikal.security.roles.RoleManager;
import org.vortikal.util.repository.URIUtil;

/**
 * <p>Manager that takes care of authorizing a principal for a specific authorization level.
 * 
 */
public class AuthorizationManager {

    // Defined authorization levels:
    
    public final static String READ_PROCESSED = "read-processed";
    public final static String READ = "read";
    public final static String CREATE = "create";
    public final static String WRITE = "write";
    public final static String WRITE_ACL = "write-acl";
    public final static String UNLOCK = "unlock";
    public final static String DELETE = "delete";
    public final static String COPY = "copy";
    public final static String MOVE = "move";
    public final static String REPOSITORY_ADMIN_ROLE_ACTION = "property-edit-admin-role";
    public final static String REPOSITORY_ROOT_ROLE_ACTION = "property-edit-root-role";

    public final static String[] ACTION_AUTHORIZATIONS = 
        new String[] {READ_PROCESSED, READ, CREATE, WRITE, WRITE_ACL, UNLOCK, 
        DELETE, COPY, MOVE, REPOSITORY_ADMIN_ROLE_ACTION, REPOSITORY_ROOT_ROLE_ACTION};
    
    public final static Set ACTION_AUTHORIZATION_SET = 
        new HashSet(Arrays.asList(ACTION_AUTHORIZATIONS));
    
    private RoleManager roleManager;
    private PrincipalManager principalManager;
    private DataAccessor dao;
    private LockManager lockManager;
    
    private boolean readOnly = false;

    public boolean isReadOnly() {
        return readOnly;
    }
    
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public void authorizeRootRoleAction(Principal principal) throws AuthorizationException {
        if (!roleManager.hasRole(principal, RoleManager.ROOT)) {
            throw new AuthorizationException("Not authorized to perform repository administration");
        }
    }
    
    private void checkReadOnly(Principal principal) throws ReadOnlyException {
        
        if (isReadOnly() && !this.roleManager.hasRole(principal, 
                RoleManager.ROOT)) {
            throw new ReadOnlyException();
        }
    }
    
    /**
     * <ul>
     *   <li>Privilege READ_PROCESSED, READ or ALL in ACL
     *   <li>Role ROOT or READ_EVERYTHING
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizeReadProcessed(String uri, Principal principal) 
    throws AuthenticationException, AuthorizationException, ResourceLockedException, IOException {
        ResourceImpl resource = this.dao.load(uri);

        if (this.roleManager.hasRole(principal, RoleManager.ROOT) ||
                this.roleManager.hasRole(principal, RoleManager.READ_EVERYTHING))
            return;

        String[] privileges = 
            new String[] {Privilege.ALL, Privilege.READ, Privilege.READ_PROCESSED};

        aclAuthorize(principal, resource, privileges);
    }

    
    /**
     * <ul>
     *   <li>Privilege READ or ALL in ACL
     *   <li>Role ROOT or READ_EVERYTHING
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizeRead(String uri, Principal principal) 
    throws AuthenticationException, AuthorizationException, ResourceLockedException, IOException {

        ResourceImpl resource = this.dao.load(uri);

        if (this.roleManager.hasRole(principal, RoleManager.ROOT) ||
                this.roleManager.hasRole(principal, RoleManager.READ_EVERYTHING))
            return;
        
        String[] privileges = 
            new String[] {Privilege.ALL, Privilege.READ};

        aclAuthorize(principal, resource, privileges);
    }

    /**
     * <ul>
     *   <li>Privilege BIND, WRITE or ALL on parent resource
     *   <li>Role ROOT
     *   <li>+ parent not locked by another principal
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizeCreate(String uri, Principal principal)
    throws AuthenticationException, AuthorizationException, ReadOnlyException, 
    ResourceLockedException, IOException {

        checkReadOnly(principal);

        ResourceImpl parent = this.dao.load(URIUtil.getParentURI(uri));
        
        this.lockManager.lockAuthorize(parent, principal, false);
        
        if (this.roleManager.hasRole(principal, RoleManager.ROOT))
            return;
        
        String[] privileges = 
            new String[] {Privilege.ALL, Privilege.WRITE, Privilege.BIND};

        aclAuthorize(principal, parent, privileges);
    }
    
    /**
     * <ul>
     *   <li>Privilege WRITE or ALL in ACL
     *   <li>Role ROOT
     *   <li>+ resource not locked by another principal
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizeWrite(String uri, Principal principal)
    throws AuthenticationException, AuthorizationException, ReadOnlyException,
    ResourceLockedException, IOException {

        checkReadOnly(principal);

        ResourceImpl resource = this.dao.load(uri);
        
        this.lockManager.lockAuthorize(resource, principal, false);

        if (this.roleManager.hasRole(principal, RoleManager.ROOT))
            return;
        
        String[] privileges = 
            new String[] {Privilege.ALL, Privilege.WRITE};

        aclAuthorize(principal, resource, privileges);
    }
    
    /**
     * <ul>
     *   <li>Privilege ALL in ACL
     *   <li>Role ROOT
     *   <li>+ resource not locked by another principal
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizeWriteAcl(String uri, Principal principal)
    throws AuthenticationException, AuthorizationException, ReadOnlyException,
    ResourceLockedException, IOException {

        checkReadOnly(principal);
        
        ResourceImpl resource = this.dao.load(uri);
        
        this.lockManager.lockAuthorize(resource, principal, false);
        
        if (this.roleManager.hasRole(principal, RoleManager.ROOT))
            return;
        
        String[] privileges = 
            new String[] {Privilege.ALL};

        aclAuthorize(principal, resource, privileges);
    }
    
    /**
     * <ul>
     *   <li>privilege WRITE or ALL in Acl + resource not locked by another principal
     *   <li>Role ROOT
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizeUnlock(String uri, Principal principal) 
    throws AuthenticationException, AuthorizationException, ReadOnlyException,
    ResourceLockedException, IOException {

        checkReadOnly(principal);
        
        if (this.roleManager.hasRole(principal, RoleManager.ROOT))
            return;
        
        ResourceImpl resource = this.dao.load(uri);
        
        this.lockManager.lockAuthorize(resource, principal, false);

        String[] privileges = 
            new String[] {Privilege.ALL, Privilege.WRITE};

        aclAuthorize(principal, resource, privileges);
    }


    /**
     * <ul>
     *   <li>Privilege ALL in ACL + parent not locked
     *   <li>Action WRITE on parent
     *   <li>+ resource tree not locked by another principal
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizeDelete(String uri, Principal principal) 
    throws AuthenticationException, AuthorizationException, ReadOnlyException, 
    ResourceLockedException, IOException {

        checkReadOnly(principal);

        Resource resource = this.dao.load(uri);
        
        this.lockManager.lockAuthorize(resource, principal, true);
        
        try {
            authorizeWrite(URIUtil.getParentURI(uri), principal);
            return;
        } catch (Exception e) {
            // Continue..
        }

        String[] privileges = 
            new String[] {Privilege.ALL};

        aclAuthorize(principal, resource, privileges);
    }
    

    /**
     * All of:
     * <ul>
     *   <li>Action write
     *   <li>Role ROOT or ADMIN
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizePropertyEditAdminRole(String uri, Principal principal) 
    throws AuthenticationException, AuthorizationException, ResourceLockedException, IOException {

        if (!(this.roleManager.hasRole(principal, RoleManager.ROOT) ||
                this.roleManager.hasRole(principal, RoleManager.ADMIN)))
            throw new AuthorizationException();
        
        authorizeWrite(uri, principal);
        
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
    public void authorizePropertyEditRootRole(String uri, Principal principal)
    throws AuthenticationException, AuthorizationException, ResourceLockedException, IOException {

        if (!this.roleManager.hasRole(principal, RoleManager.ROOT))
            throw new AuthorizationException();
        
        authorizeWrite(uri, principal);
        
    }
    
    /**
     * All of:
     * <ul>
     *   <li>READ action on source tree
     *   <li>CREATE action on destination
     *   <li>if overwrite, DELETE action on dest
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizeCopy(String srcUri, String destUri, 
            Principal principal, boolean deleteDestination) 
    throws AuthenticationException, AuthorizationException, ReadOnlyException,
    ResourceLockedException, IOException {

        checkReadOnly(principal);

        authorizeRead(srcUri, principal);

        Resource resource = this.dao.load(srcUri);

        if (resource.isCollection()) {
            String[] uris = this.dao.discoverACLs(srcUri);
            for (int i = 0; i < uris.length; i++) {
                authorizeRead(uris[i], principal);
            }
        }

        authorizeCreate(destUri, principal);

        if (deleteDestination) authorizeDelete(destUri, principal);        
    }
    

    /**
     * All of:
     * <ul>
     *   <li>COPY action
     *   <li>DELETE action on source
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizeMove(String srcUri, String destUri,
            Principal principal, boolean deleteDestination) 
    throws AuthenticationException, AuthorizationException, ReadOnlyException,
    ResourceLockedException, IOException {

        checkReadOnly(principal);

        authorizeCopy(srcUri, destUri, principal, deleteDestination);
        authorizeDelete(srcUri, principal);
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
    private void aclAuthorize(Principal principal, Resource resource, String[] privileges) 
    throws AuthenticationException, AuthorizationException {
        
        Acl acl = resource.getAcl();

        for (int i = 0; i < privileges.length; i++) {
            String privilege = privileges[i];
            Set principalSet = acl.getPrincipalSet(privilege);

            // Dont't need to test the conditions if (principalSet == null)
            if (principalSet == null || principalSet.size() == 0) {
                continue;
            }

            // Condition 1:
            if (principalSet.contains(PseudoPrincipal.ALL)) {
            // XXX: removed this:
            //            && (Privilege.READ.equals(action) || 
            //                    Privilege.READ_PROCESSED.equals(action))
                return;
            }

            // If not condition 1 - needs to be authenticated
            if (principal == null) {
                continue;
            }

            // Condition 2:
            if (principalSet.contains(PseudoPrincipal.AUTHENTICATED)) {
                return;
            }

            // Condition 3:
            if (resource.getOwner().equals(principal) && principalSet.contains(PseudoPrincipal.OWNER)) {
                return;
            }

            // Condition 4:
            if (principalSet.contains(principal)) {
                return;
            }
        }

        // XXX: is this correct?
        if (principal == null) throw new AuthenticationException();
            
        for (int i = 0; i < privileges.length; i++) {
            String action = privileges[i];
            Set principalSet = acl.getPrincipalSet(action);
            
            // Condition 5:
            if (groupMatch(principalSet, principal)) {
                return;
            }
        }
        
        throw new AuthorizationException();

    }
    
    private boolean groupMatch(Set principalList, Principal principal) {

        for (Iterator i = principalList.iterator(); i.hasNext();) {
            Principal p = (Principal) i.next();

            if (p.getType() == Principal.TYPE_GROUP) {
                if (principalManager.isMember(principal, p)) {
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


    /**
     * @param lockManager The lockManager to set.
     */
    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }

}
