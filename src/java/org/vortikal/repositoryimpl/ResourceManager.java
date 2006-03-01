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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.vortikal.repository.Ace;
import org.vortikal.repository.AclException;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.IllegalOperationException;
import org.vortikal.repository.Privilege;
import org.vortikal.repository.PrivilegeDefinition;
import org.vortikal.repository.ResourceLockedException;
import org.vortikal.repositoryimpl.dao.DataAccessor;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.InvalidPrincipalException;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalManager;
import org.vortikal.security.roles.RoleManager;
import org.vortikal.util.repository.ContentTypeHelper;
import org.vortikal.util.repository.MimeHelper;
import org.vortikal.util.repository.URIUtil;


public class ResourceManager {

    private PrincipalManager principalManager;
    private RoleManager roleManager;
    private DataAccessor dao;

    public void authorizeRecursively(Resource resource, Principal principal,
                                     String privilege)
        throws IOException, AuthenticationException, AuthorizationException {

        authorize(resource, principal, privilege);
        if (resource instanceof Collection) {
            String[] uris = this.dao.discoverACLs(resource);
            for (int i = 0; i < uris.length; i++) {
                Resource ancestor = this.dao.load(uris[i]);
                authorize(ancestor, principal, privilege);
            }
        }
    }
    

    public void lockAuthorize(Resource resource, Principal principal, String privilege)
                  throws ResourceLockedException, IOException, AuthenticationException {
        if (resource.getLock() != null) {

            if (!org.vortikal.repository.PrivilegeDefinition.WRITE.equals(
                    privilege)) {
                return;
            }

            if (principal == null) {
                throw new AuthenticationException();
            }

            if (!resource.getLock().getUser().equals(principal.getQualifiedName())) {
                throw new org.vortikal.repository.ResourceLockedException();
            }
        }
    }

    public void lockAuthorizeRecursively(Collection collection, Principal principal,
                                         String privilege) 
        throws ResourceLockedException, IOException, AuthenticationException {

        this.lockAuthorize(collection, principal, privilege);

        String[] uris = this.dao.discoverLocks(collection);

        for (int i = 0; i < uris.length;  i++) {
            Resource ancestor = this.dao.load(uris[i]);
            this.lockAuthorize(ancestor, principal, privilege);
        }
    }



    public String lockResource(Resource resource, Principal principal,
                               String ownerInfo, String depth,
                               int desiredTimeoutSeconds, boolean refresh)
        throws AuthenticationException, AuthorizationException, 
            ResourceLockedException, IOException {


        this.authorize(resource, principal,
            org.vortikal.repository.PrivilegeDefinition.WRITE);

        if (resource.getLock() != null) {
            this.lockAuthorize(resource, principal,
                org.vortikal.repository.PrivilegeDefinition.WRITE);
        }

        if (!refresh) {
            resource.setLock(null);
        }

        if (resource.getLock() == null) {
            resource.setLock(new Lock(principal, ownerInfo, depth,
                new Date(System.currentTimeMillis() +
                         (desiredTimeoutSeconds * 1000))));
        } else {
            resource.setLock(
                new Lock(resource.getLock().getLockToken(), principal.getQualifiedName(),
                         ownerInfo, depth, 
                         new Date(System.currentTimeMillis() + (desiredTimeoutSeconds * 1000))));
        }
        this.dao.store(resource);
        return resource.getLock().getLockToken();
    }

    public void unlockResource(Resource resource, Principal principal, String lockToken)
        throws AuthenticationException, AuthorizationException, 
            ResourceLockedException, IOException {

        this.authorize(
            resource, principal, org.vortikal.repository.PrivilegeDefinition.WRITE);

        if (resource.getLock() != null) {
            if (!this.roleManager.hasRole(principal.getQualifiedName(), RoleManager.ROOT)) {
                this.lockAuthorize(resource, principal,
                                   org.vortikal.repository.PrivilegeDefinition.WRITE);
            }
            resource.setLock(null);
            this.dao.store(resource);
        }
    }
    

    public void deleteResource(Resource resource, Principal principal)
        throws AuthorizationException, AuthenticationException, 
            ResourceLockedException, IOException {
        
        if (resource.getLock() != null) {
            // XXX: remove authorization here, 
            this.lockAuthorize(resource, principal,
                org.vortikal.repository.PrivilegeDefinition.WRITE);
        }
        this.dao.delete(resource);
    }


    /**
     * Creates a collection with an inherited ACL
     *
     */
    public Resource createCollection(Collection parent, Principal principal, String path)
        throws IllegalOperationException, AuthenticationException, 
            AuthorizationException, AclException, IOException {
        return this.createCollection(parent, principal, principal.getQualifiedName(), path,
            new ACL(), true);
    }

    /**
     * Creates a collection with a specified owner and (possibly inherited) ACL.
     */
    private Resource createCollection(Collection parent, Principal principal, String owner,
        String path, ACL acl, boolean inheritedACL)
        throws IllegalOperationException, AuthenticationException, 
            AuthorizationException, AclException, IOException {

        this.authorize(parent, principal, PrivilegeDefinition.WRITE);

        Resource r = new Collection(path, owner, principal.getQualifiedName(),
                principal.getQualifiedName(), new ACL(),
                true, null, dao, this.principalManager, new String[] {  });

        Date now = new Date();

        r.setCreationTime(now);
        r.setContentLastModified(now);
        r.setPropertiesLastModified(now);
        this.dao.store(r);
        r = this.dao.load(path);

        if (!inheritedACL) {
            //acl.setResource(r);
            r.setInheritedACL(false);
            this.storeACL(r, principal, toAceList(acl, null));
        }

        parent.addChildURI(r.getURI());

        // Update timestamps:
        parent.setContentLastModified(new Date());
        parent.setPropertiesLastModified(new Date());

        // Update principal info:
        parent.setContentModifiedBy(principal.getQualifiedName());
        parent.setPropertiesModifiedBy(principal.getQualifiedName());

        this.dao.store(parent);

        return r;
    }


    public Resource create(Collection parent, Principal principal, String path)
        throws IllegalOperationException, AuthenticationException, 
            AuthorizationException, AclException, IOException {
        return this.create(parent, principal, principal, path, null, true);
    }

    private Resource create(Collection parent, Principal principal,
                           Principal owner, String path, ACL acl, boolean inheritedACL)
        throws IllegalOperationException, AuthenticationException, 
            AuthorizationException, AclException, IOException {
        authorize(parent, principal, PrivilegeDefinition.WRITE);

        Resource r = new Document(path, owner.getQualifiedName(), principal.getQualifiedName(),
                principal.getQualifiedName(), new ACL(),
                true, null, this.dao, this.principalManager);

        Date now = new Date();

        r.setCreationTime(now);
        r.setContentLastModified(now);
        r.setPropertiesLastModified(now);

        r.setContentType(MimeHelper.map(r.getName()));

        this.dao.store(r);

        try {
            r = this.dao.load(path);
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }

        if (!inheritedACL) {
            r.setInheritedACL(false);
            this.storeACL(r, principal, toAceList(acl, null));
        }

        parent.addChildURI(r.getURI());

        // Update timestamps:
        parent.setContentLastModified(new Date());
        parent.setPropertiesLastModified(new Date());

        // Update principal info:
        parent.setContentModifiedBy(principal.getQualifiedName());
        parent.setPropertiesModifiedBy(principal.getQualifiedName());

        this.dao.store(parent);

        return r;
    }


    public void copy(Principal principal, Resource resource, String destUri,
                     boolean preserveACL, boolean preserveOwner)
        throws IllegalOperationException, AuthenticationException, 
            AuthorizationException, IOException, ResourceLockedException, 
            AclException {

        ACL acl = (!preserveACL || resource.isInheritedACL())
            ? new ACL() : resource.getACL();

        boolean aclInheritance = (!preserveACL || resource.isInheritedACL());

        Principal owner = (preserveOwner) ?
            principalManager.getPrincipal(resource.getOwner()) : principal;

        String parentURI = URIUtil.getParentURI(destUri);
        Collection parent = (Collection) this.dao.load(parentURI);


        if (resource instanceof Collection) {

            Collection child = (Collection) this.createCollection(parent,
                principal, owner.getQualifiedName(),
                destUri, acl, aclInheritance);

            child.setProperties(resource.getPropertyDTOs());
            dao.store(child);

            Resource[] children = this.dao.loadChildren((Collection) resource);
            for (int i = 0; i < children.length; i++) {

                Resource r = children[i];
                this.copy(principal, r,
                    child.getURI() + "/" +
                    r.getURI().substring(r.getURI().lastIndexOf("/") + 1),
                           preserveACL, preserveOwner);
            }

            return;
        }

        Document src = (Document) resource;
        Document doc = (Document) this.create(parent, principal, owner, destUri, acl,
                                              aclInheritance);

        doc.setContentType(src.getContentType());
        doc.setContentLocale(src.getContentLocale());
        doc.setCharacterEncoding(src.getCharacterEncoding());
        doc.setProperties(src.getPropertyDTOs());

        dao.store(doc);

        InputStream input = this.getResourceInputStream(src, owner, PrivilegeDefinition.READ);

        OutputStream output = null;

        output = dao.getOutputStream(doc);

        byte[] buf = new byte[1024];
        int bytesRead = 0;

        while ((bytesRead = input.read(buf)) > 0) {
            output.write(buf, 0, bytesRead);
        }

        input.close();
        output.close();
    }


    public void delete(Resource resource, Principal principal)
        throws AuthorizationException, AuthenticationException, 
            ResourceLockedException, IOException {
        if (resource.isCollection()) {
            Collection collection = (Collection) resource;
            if (collection.getLock() != null) {
                this.lockAuthorizeRecursively(collection, principal, PrivilegeDefinition.WRITE);
            }
        }
        this.dao.delete(resource);
    }


    public void storeProperties(Resource resource, Principal principal,
                                org.vortikal.repository.Resource dto)
        throws AuthenticationException, AuthorizationException, 
            ResourceLockedException, IllegalOperationException, IOException {

        this.authorize(resource, principal, PrivilegeDefinition.WRITE);
        this.lockAuthorize(resource, principal, PrivilegeDefinition.WRITE);
        
        if (!resource.getOwner().equals(dto.getOwner().getQualifiedName())) {
            /* Attempt to take ownership, only the owner of a parent
             * resource may do that, so do it in a secure manner: */
            this.setResourceOwner(resource, principal, dto, dto.getOwner().getQualifiedName());
        }

        if (dto.getOverrideLiveProperties()) {
            resource.setPropertiesLastModified(dto.getPropertiesLastModified());
            resource.setContentLastModified(dto.getContentLastModified());
            resource.setCreationTime(dto.getCreationTime());

        } else {
            resource.setPropertiesLastModified(new Date());
            resource.setPropertiesModifiedBy(principal.getQualifiedName());
        }
        
        if (!resource.isCollection()) {

            resource.setContentType(dto.getContentType());
            resource.setCharacterEncoding(null);
            ((Document) resource).setContentLocale(dto.getContentLocale());

            if ((resource.getContentType() != null)
                && ContentTypeHelper.isTextContentType(resource.getContentType()) &&
                (dto.getCharacterEncoding() != null)) {
                try {
                    /* Force checking of encoding */
                    new String(new byte[0], dto.getCharacterEncoding());

                    resource.setCharacterEncoding(dto.getCharacterEncoding());
                } catch (java.io.UnsupportedEncodingException e) {
                    // FIXME: Ignore unsupported character encodings?
                }
            }

        }

        resource.setDisplayName(dto.getDisplayName());
        resource.setProperties(dto.getProperties());
        
        this.dao.store(resource);
    }
    


    protected void setResourceOwner(Resource resource, Principal principal,
        org.vortikal.repository.Resource dto, String owner)
        throws AuthorizationException, IllegalOperationException, IOException {
        if ((owner == null) || owner.trim().equals("")) {
            throw new IllegalOperationException(
                "Unable to set owner of resource " + this +
                ": invalid owner: '" + owner + "'");
        }

        /*
         * Only principals of the ROOT role or owners are allowed to
         * set owner:
         */
        if (!(this.roleManager.hasRole(principal.getQualifiedName(), RoleManager.ROOT) ||
              principal.getQualifiedName().equals(resource.getOwner()))) {
            throw new AuthorizationException(
                "Principal " + principal.getQualifiedName()
                + " is not allowed to set owner of "
                + "resource " + resource.getURI());
        }

        Principal principal2 = null;
        
        try {
            principal2 = principalManager.getPrincipal(owner);
        } catch (InvalidPrincipalException e) {
            throw new IllegalOperationException(
                "Unable to set owner of resource " + resource.getURI()
                + ": invalid owner: '" + owner + "'");
        }
        
        if (!principalManager.validatePrincipal(principal2)) {
            throw new IllegalOperationException(
                "Unable to set owner of resource " + resource.getURI()
                + ": invalid owner: '" + owner + "'");
        }

        resource.setOwner(owner);
    }


    public void storeACL(Resource resource, Principal principal,
                         org.vortikal.repository.Ace[] aceList)
        throws AuthorizationException, AuthenticationException, 
            IllegalOperationException, IOException, AclException {

        ACL acl = buildACL(aceList);
        resource.setACL(acl);

        /* If the first ACE has set inheritance, we know that the
         * whole ACL has valid inheritance (ACL.validateACL() ensures
         * this), so we can go ahead and set it here: */
        boolean inheritedACL = aceList[0].getInheritedFrom() != null;

        if (!"/".equals(resource.getURI()) && inheritedACL) {
            /* When the ACL is inherited, make our ACL a copy of our
             * parent's ACL, since the supplied one may contain other
             * ACEs than the one we now inherit from. */
            try {
                ACL parentACL = (ACL) this.dao.load(URIUtil.getParentURI(resource.getURI())).getACL().clone();

                resource.setACL(parentACL);
            } catch (CloneNotSupportedException e) {
            }
        }

        try {
            resource.setInheritedACL(inheritedACL);
            resource.setDirtyACL(true);

            this.dao.store(resource);
        } catch (Exception e) {
            
            throw new IOException(e.getMessage());
        } finally {
            resource.setDirtyACL(false);
        }
    }


    public InputStream getResourceInputStream(Document resource,
                                              Principal principal, String privilege)
        throws AuthenticationException, AuthorizationException, IOException, 
            ResourceLockedException {
        this.authorize(resource, principal, privilege);
        this.lockAuthorize(resource, principal, privilege);

        return dao.getInputStream(resource);
    }

    public OutputStream getResourceOutputStream(Document resource, Principal principal)
        throws AuthenticationException, AuthorizationException, IOException, 
            ResourceLockedException {
        this.authorize(resource, principal, PrivilegeDefinition.WRITE);
        this.lockAuthorize(resource, principal, PrivilegeDefinition.WRITE);

        resource.setContentLastModified(new Date());
        resource.setContentModifiedBy(principal.getQualifiedName());
        this.dao.store(resource);

        return dao.getOutputStream(resource);
    }

    
    
    
    public org.vortikal.repository.Resource getResourceDTO(
            Resource resource, Principal principal) throws IOException {
            org.vortikal.repository.Resource dto = new org.vortikal.repository.Resource();

            dto.setURI(resource.getURI());
            dto.setCreationTime(resource.getCreationTime());
            dto.setContentLastModified(resource.getContentLastModified());
            dto.setPropertiesLastModified(resource.getPropertiesLastModified());
            dto.setContentModifiedBy(principalManager.getPrincipal(resource.getContentModifiedBy()));
            dto.setPropertiesModifiedBy(principalManager.getPrincipal(resource.getPropertiesModifiedBy()));
            dto.setContentType(resource.getContentType());
            dto.setCharacterEncoding(resource.getCharacterEncoding());
            dto.setDisplayName(resource.getDisplayName());
            dto.setActiveLocks((resource.lock == null)
                ? new org.vortikal.repository.Lock[] {  }
                : new org.vortikal.repository.Lock[] { resource.lock.getLockDTO(principalManager) });
            dto.setName(resource.name);
            dto.setOwner(principalManager.getPrincipal(resource.owner));
            dto.setProperties(resource.getPropertyDTOs());

            try {
                ACL originalACL = (ACL) resource.acl.clone();

                dto.setACL(addRolesToACL(resource, originalACL));

                if ("/".equals(resource.getURI())) {
                    dto.setParentACL(new Ace[0]);
                } else {
                    Resource parent = this.dao.load(URIUtil.getParentURI(resource.getURI()));
                    ACL parentACL = (ACL) parent.getACL().clone();

                    dto.setParentACL(addRolesToACL(resource, parentACL));
                    dto.setParentOwner(principalManager.getPrincipal(parent.getOwner()));
                }
            } catch (CloneNotSupportedException e) {
            }

            if (resource.isCollection()) {
               dto.setChildURIs(((Collection)resource).getChildURIs());
            } else {
               dto.setContentLength(dao.getContentLength(resource));
               dto.setContentLocale(((Document)resource).getContentLocale());
            }
            
            return dto;
        }

    /**
     * Adds root and read everything roles to ACL
     *
     * @param originalACL an <code>Ace[]</code> value
     * @return an <code>Ace[]</code>
     */
    private Ace[] addRolesToACL(Resource resource, ACL originalACL) {

        String inheritedFrom = null;
        if (resource.isInheritedACL()) {
            inheritedFrom = URIUtil.getParentURI(resource.getURI());
        }

        List acl = new ArrayList(Arrays.asList(toAceList(originalACL, inheritedFrom)));
        List rootPrincipals = roleManager.listPrincipals(RoleManager.ROOT);

        for (Iterator i = rootPrincipals.iterator(); i.hasNext();) {
            String root = (String) i.next();
            org.vortikal.repository.ACLPrincipal aclPrincipal = org.vortikal.repository.ACLPrincipal.getInstance(org.vortikal.repository.ACLPrincipal.TYPE_URL,
                    root, true);
            Ace ace = new Ace();

            ace.setPrincipal(aclPrincipal);
            ace.setPrivileges(getRootPrivileges());
            acl.add(ace);
        }

        List readPrincipals = roleManager.listPrincipals(RoleManager.READ_EVERYTHING);

        for (Iterator i = readPrincipals.iterator(); i.hasNext();) {
            String read = (String) i.next();
            org.vortikal.repository.ACLPrincipal aclPrincipal = org.vortikal.repository.ACLPrincipal.getInstance(org.vortikal.repository.ACLPrincipal.TYPE_URL,
                    read, true);
            Ace ace = new Ace();

            ace.setPrincipal(aclPrincipal);
            ace.setPrivileges(getReadPrivileges());
            acl.add(ace);
        }

        return (Ace[]) acl.toArray(new Ace[0]);
    }

    
    private Privilege[] getRootPrivileges() {
        Privilege read = new Privilege();

        read.setName(PrivilegeDefinition.READ);

        Privilege write = new Privilege();

        write.setName(PrivilegeDefinition.WRITE);

        Privilege writeACL = new Privilege();

        writeACL.setName(PrivilegeDefinition.WRITE_ACL);

        Privilege[] rootPrivs = new Privilege[3];

        rootPrivs[0] = read;
        rootPrivs[1] = write;
        rootPrivs[2] = writeACL;

        return rootPrivs;
    }

    private Privilege[] getReadPrivileges() {
        Privilege read = new Privilege();

        read.setName(PrivilegeDefinition.READ);

        Privilege[] readPrivs = new Privilege[1];

        readPrivs[0] = read;

        return readPrivs;
    }

    /**
     * @param dao The dao to set.
     */
    public void setDao(DataAccessor dao) {
        this.dao = dao;
    }

    /**
     * @param principalManager The principalManager to set.
     */
    public void setPrincipalManager(PrincipalManager principalManager) {
        this.principalManager = principalManager;
    }

    /**
     * @param roleManager The roleManager to set.
     */
    public void setRoleManager(RoleManager roleManager) {
        this.roleManager = roleManager;
    }

    /*
     * Build an ACL object from the Ace[] array (assumes valid
     * input, except that principal names of URL-type principals are
     * trimmed of whitespace, since the principal manager may be
     * tolerant and pass these trough validation).
     */
    private ACL buildACL(Ace[] aceList)
        throws AclException {

        ACL acl = new ACL();
        Map privilegeMap = acl.getActionMap();

        for (int i = 0; i < aceList.length; i++) {
            Ace ace = aceList[i];
            org.vortikal.repository.ACLPrincipal principal = ace.getPrincipal();
            String principalName = null;

            if (principal.getType() == org.vortikal.repository.ACLPrincipal.TYPE_URL) {
                //TODO: document behaviour where name = name@defaultdomain
                if (principal.isUser())
                    principalName = principalManager.getPrincipal(
                            principal.getURL().trim()).getQualifiedName();
                else 
                    principalName = principal.getURL().trim();
            } else if (principal.getType() == org.vortikal.repository.ACLPrincipal.TYPE_ALL) {
                principalName = "dav:all";
            } else if (principal.getType() == org.vortikal.repository.ACLPrincipal.TYPE_OWNER) {
                principalName = "dav:owner";
            } else if (principal.getType() == org.vortikal.repository.ACLPrincipal.TYPE_AUTHENTICATED) {
                principalName = "dav:authenticated";
            } else {
                throw new AclException(AclException.RECOGNIZED_PRINCIPAL,
                    "Unknown principal: " + principal.getURL());
            }

            Privilege[] privileges = ace.getPrivileges();

            for (int j = 0; j < privileges.length; j++) {
                Privilege privilege = privileges[j];

                /*
                 * We don't store the required ACE
                 * ((dav:owner (dav:read dav:write dav:write-acl))
                 */

                //                 if (principalName.equals("dav:owner") &&
                //                     (privilege.getName().equals(PrivilegeDefinition.READ) ||
                //                      privilege.getName().equals(PrivilegeDefinition.WRITE) ||
                //                      privilege.getName().equals(PrivilegeDefinition.WRITE_ACL))) {
                //                     continue;
                //                 }
                // Add an entry for (privilege, principal)
                if (!privilegeMap.containsKey(privilege.getName())) {
                    privilegeMap.put(privilege.getName(), new ArrayList());
                }

                List principals = (List) privilegeMap.get(privilege.getName());

                ACLPrincipal p = new ACLPrincipal(principalName,
                        !principal.isUser());

                if (!principals.contains(p)) {
                    principals.add(p);
                }
            }
        }

        return acl;
    }
    
    

    
    /*
     * From ACL: 
     * 
     */
    
    public void authorize(Resource resource, Principal principal, String action) 
        throws AuthenticationException, AuthorizationException, IOException {
 
        ACL acl = resource.getACL();
        
        /*
         * Special treatment for uio:read-processed needed: dav:read also grants
         * uio:read-processed
         */
        if (action
                .equals(org.vortikal.repository.Resource.CUSTOM_PRIVILEGE_READ_PROCESSED)) {
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
         * 1) (dav:all, action) is present in the resource's ACL. 
         * NOTE: Now limits this to read operations
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
        if (userMatch(principalList, "dav:all")
                && (PrivilegeDefinition.READ.equals(action) 
                    || org.vortikal.repository.Resource.CUSTOM_PRIVILEGE_READ_PROCESSED.equals(action))) {
            return;
        }

        // If not condition 1 - needs to be authenticated
        if (principal == null) {
            throw new AuthenticationException();
        }

        // Condition 2:
        if (userMatch(principalList, "dav:authenticated")) {
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
        //                 action.equals(PrivilegeDefinition.WRITE) ||
        //                 action.equals(PrivilegeDefinition.WRITE_ACL)) {
        //                 return;
        //             }
        //         }
        // Dont't need to test the remaining conditions if (principalList == null)
        if (principalList == null) {
            throw new AuthorizationException();
        }

        if (resource.getOwner().equals(principal.getQualifiedName())) {
            if (userMatch(principalList, "dav:owner")) {
                return;
            }
        }

        // Condition 5:
        if (userMatch(principalList, principal.getQualifiedName())) {
            return;
        }

        // Condition 6:
        if (groupMatch(principalList, principal)) {
            return;
        }

        throw new AuthorizationException();
    }

    /**
     * Decides whether a given principal exists in a principal list.
     *
     * @param principalList a <code>List</code> value
     * @param username a <code>String</code> value
     * @return a <code>boolean</code>
     */
    private boolean userMatch(List principalList, String username) {
        if (principalList != null) {
            return principalList.contains(new ACLPrincipal(username));
        }

        return false;
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

    
    
    /** To be removed...?
     * 
     * @deprecated
     * @param principal
     * @param resource
     * @param principalManager
     * @param roleManager
     * @return
     * @throws IOException
     */
    protected Privilege[] getCurrentUserPrivileges(
        Principal principal, Resource resource, PrincipalManager principalManager,
        RoleManager roleManager) throws IOException {

        ArrayList privs = new ArrayList();

        String[] testedPrivileges = new String[] {
                PrivilegeDefinition.WRITE, PrivilegeDefinition.READ,
                PrivilegeDefinition.WRITE_ACL,
                org.vortikal.repository.Resource.CUSTOM_PRIVILEGE_READ_PROCESSED
            };

        for (int i = 0; i < testedPrivileges.length; i++) {
            String action = testedPrivileges[i];

            /* Try to authorize against every privilege, and if it
             * works, add the privilege to the list: */
            try {
                authorize(resource, principal, action);

                Privilege priv = new Privilege();

                priv.setName(action);
                privs.add(priv);
            } catch (AuthenticationException e) {
                // ignore
            } catch (AuthorizationException e) {
                // ignore
            }
        }

        return (Privilege[]) privs.toArray(new Privilege[] {});
    }

    /**
     * Generates a list of Ace objects (for data exchange).
     *
     * @return an <code>Ace[]</code>
     */
    public Ace[] toAceList(ACL acl, String inheritedFrom) {

        Set actions = acl.getActionMap().keySet();

        HashMap userMap = new HashMap();

        /*
         * Add ((dav:owner (dav:read dav:write dav:write-acl))
         */

        //         HashSet owner = new HashSet();
        //         owner.add(PrivilegeDefinition.READ);
        //         owner.add(PrivilegeDefinition.WRITE);
        //         owner.add(PrivilegeDefinition.WRITE_ACL);
        //         userMap.put(new ACLPrincipal("dav:owner"), owner);
        for (Iterator i = actions.iterator(); i.hasNext();) {
            String action = (String) i.next();

            List principalList = acl.getPrincipalList(action);

            for (Iterator j = principalList.iterator(); j.hasNext();) {
                ACLPrincipal p = (ACLPrincipal) j.next();

                if (!userMap.containsKey(p)) {
                    HashSet actionSet = new HashSet();

                    userMap.put(p, actionSet);
                }

                HashSet actionSet = (HashSet) userMap.get(p);

                actionSet.add(action);
            }
        }

        Ace[] aces = new Ace[userMap.size()];
        int aclIndex = 0;

        /* Create the ACE's  */
        for (Iterator i = userMap.keySet().iterator(); i.hasNext();) {
            ACLPrincipal p = (ACLPrincipal) i.next();

            /* Create the principal */
            org.vortikal.repository.ACLPrincipal principal = new org.vortikal.repository.ACLPrincipal();

            if (p.getUrl().equals("dav:all")) {
                principal.setType(org.vortikal.repository.ACLPrincipal.TYPE_ALL);
            } else if (p.getUrl().equals("dav:owner")) {
                principal.setType(org.vortikal.repository.ACLPrincipal.TYPE_OWNER);
            } else if (p.getUrl().equals("dav:authenticated")) {
                principal.setType(org.vortikal.repository.ACLPrincipal.TYPE_AUTHENTICATED);
            } else {
                principal.setType(org.vortikal.repository.ACLPrincipal.TYPE_URL);
                principal.setIsUser(!p.isGroup());
                principal.setURL(p.getUrl());
            }

            /* Create the ACE */
            Ace element = new Ace();

            element.setPrincipal(principal);

            ArrayList privs = new ArrayList();

            for (Iterator j = ((Set) userMap.get(p)).iterator(); j.hasNext();) {
                String action = (String) j.next();
                Privilege priv = new Privilege();

                priv.setName(action);

                // FIXME: Hack coming up:
                if (action.equals(org.vortikal.repository.Resource.CUSTOM_PRIVILEGE_READ_PROCESSED)) {
                    priv.setNamespace(org.vortikal.repository.Resource.CUSTOM_NAMESPACE);
                } else {
                    priv.setNamespace(PrivilegeDefinition.STANDARD_NAMESPACE);
                }

                privs.add(priv);
            }

            Privilege[] privileges = (Privilege[]) privs.toArray(new Privilege[] {
                        
                    });

            element.setPrivileges(privileges);
            element.setGranted(true);

            element.setInheritedFrom(inheritedFrom);

            aces[aclIndex++] = element;
        }

        return aces;
    }


}
