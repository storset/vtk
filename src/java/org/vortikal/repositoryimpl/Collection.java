/* Copyright (c) 2004, University of Oslo, Norway
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
import java.util.Iterator;
import java.util.List;

import org.vortikal.repository.AclException;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.FailedDependencyException;
import org.vortikal.repository.IllegalOperationException;
import org.vortikal.repository.PrivilegeDefinition;
import org.vortikal.repository.ResourceLockedException;
import org.vortikal.repositoryimpl.dao.DataAccessor;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalManager;
import org.vortikal.security.roles.RoleManager;


public class Collection extends Resource implements Cloneable {
    private String[] childURIs = null;

    public Collection(String uri, String owner, String contentModifiedBy,
        String propertiesModifiedBy, ACL acl, boolean inheritedACL, Lock lock,
        DataAccessor dao, PrincipalManager principalManager, String[] childURIs) {
        super(uri, owner, contentModifiedBy, propertiesModifiedBy, acl,
            inheritedACL, lock, dao, principalManager);
        this.childURIs = childURIs;
        setContentType("application/x-vortex-collection");
    }

    public Object clone() throws CloneNotSupportedException {
        ACL acl = (this.acl == null) ? null : (ACL) this.acl.clone();
        Lock lock = (this.lock == null) ? null : (Lock) this.lock.clone();
        String[] clonedChildURIs = new String[childURIs.length];

        System.arraycopy(childURIs, 0, clonedChildURIs, 0, childURIs.length);

        return new Collection(uri, owner, contentModifiedBy,
            propertiesModifiedBy, acl, inheritedACL, lock, dao, principalManager,
            clonedChildURIs);
    }

    public List getChildren() throws IOException {
        long start = System.currentTimeMillis();

        //Resource[] children = dao.load(childURIs);
        Resource[] children = dao.loadChildren(this);
        List retVal = new ArrayList();

        for (int i = 0; i < children.length; i++) {
            retVal.add(children[i]);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Loading " + childURIs.length + " resources took " +
                (System.currentTimeMillis() - start) + " ms");
        }

        return retVal;
    }

    public void setChildURIs(String[] childURIs) {
        this.childURIs = childURIs;
    }

    public String[] getChildURIs() {
        return this.childURIs;
    }
    

    public Resource create(Principal principal, String path,
        RoleManager roleManager)
        throws IllegalOperationException, AuthenticationException, 
            AuthorizationException, AclException, IOException {
        return create(principal, principal, path,
            new ACL(new HashMap(), principalManager), true, roleManager);
    }

    public Resource create(Principal principal, Principal owner, String path,
        ACL acl, boolean inheritedACL, RoleManager roleManager)
        throws IllegalOperationException, AuthenticationException, 
            AuthorizationException, AclException, IOException {
        authorize(principal, PrivilegeDefinition.WRITE, roleManager);

        String childName = path.substring(path.indexOf(uri));

        /* FIXME: if write access = "dav:all": who creates the resource? */
        if (principal == null) {
            throw new AuthenticationException(
                "Principal must be specified in order to create a resource");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("creating " + childName);
        }

        Resource r = new Document(path, owner.getQualifiedName(), principal.getQualifiedName(),
                principal.getQualifiedName(), new ACL(new HashMap(), principalManager),
                true, null, dao, principalManager);

        Date now = new Date();

        r.setCreationTime(now);
        r.setContentLastModified(now);
        r.setPropertiesLastModified(now);

        r.setContentType(MimeHelper.map(r.getName()));

        dao.store(r);

        try {
            r = dao.load(path);
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }

        if (!inheritedACL) {
            acl.setResource(r);
            r.setInheritedACL(false);
            r.storeACL(principal, acl.toAceList(), roleManager);
        }

        addChildURI(r.getURI());

        return r;
    }

    /**
     * Creates a collection with an inherited ACL
     *
     */
    public Resource createCollection(Principal principal, String path,
        RoleManager roleManager)
        throws IllegalOperationException, AuthenticationException, 
            AuthorizationException, AclException, IOException {
        return createCollection(principal, principal.getQualifiedName(), path,
            new ACL(new HashMap(), principalManager), true, roleManager);
    }

    /**
     * Creates a collection with a specified owner and (possibly inherited) ACL.
     */
    public Resource createCollection(Principal principal, String owner,
        String path, ACL acl, boolean inheritedACL, RoleManager roleManager)
        throws IllegalOperationException, AuthenticationException, 
            AuthorizationException, AclException, IOException {
        authorize(principal, PrivilegeDefinition.WRITE, roleManager);

        Resource r = new Collection(path, owner, principal.getQualifiedName(),
                principal.getQualifiedName(), new ACL(new HashMap(), principalManager),
                true, null, dao, principalManager, new String[] {  });

        Date now = new Date();

        r.setCreationTime(now);
        r.setContentLastModified(now);
        r.setPropertiesLastModified(now);
        dao.store(r);
        r = dao.load(path);

        if (!inheritedACL) {
            acl.setResource(r);
            r.setInheritedACL(false);
            r.storeACL(principal, acl.toAceList(), roleManager);
        }

        addChildURI(r.getURI());

        return r;
    }

    public void copy(Principal principal, Resource resource, String destUri,
                     boolean preserveACL, boolean preserveOwner, PrincipalManager principalManager,
                     RoleManager roleManager)
        throws IllegalOperationException, AuthenticationException, 
            AuthorizationException, IOException, ResourceLockedException, 
            AclException {
        if (logger.isDebugEnabled()) {
            logger.debug("copy(" + resource.getURI() + "," + destUri + "), " +
                "principal = " + principal.getQualifiedName());
        }

        ACL acl = (!preserveACL || resource.getInheritedACL())
            ? new ACL(new HashMap(), principalManager) : resource.getACL();

        boolean aclInheritance = (!preserveACL || resource.getInheritedACL());

//         Principal owner = (preserveOwner)
//             ? new PrincipalImpl(resource.getOwner()) : principal;
        Principal owner = (preserveOwner) ?
            principalManager.getPrincipal(resource.getOwner()) : principal;

        if (resource instanceof Collection) {
            Collection child = (Collection) createCollection(
                principal, owner.getQualifiedName(),
                destUri, acl, aclInheritance, roleManager);

            child.setProperties(resource.getPropertyDTOs());
            dao.store(child);

            for (Iterator i = ((Collection) resource).getChildren().iterator();
                    i.hasNext();) {
                Resource r = (Resource) i.next();

                child.copy(principal, r,
                    child.getURI() + "/" +
                    r.getURI().substring(r.getURI().lastIndexOf("/") + 1),
                           preserveACL, preserveOwner, principalManager, roleManager);
            }

            return;
        }

        Document src = (Document) resource;
        Document doc = (Document) create(principal, owner, destUri, acl,
                aclInheritance, roleManager);

        doc.setContentType(src.getContentType());
        doc.setContentLocale(src.getContentLocale());
        doc.setCharacterEncoding(src.getCharacterEncoding());
        doc.setProperties(src.getPropertyDTOs());

        dao.store(doc);

        InputStream input = src.getInputStream(owner, PrivilegeDefinition.READ,
                roleManager);

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

    public void delete(Principal principal, RoleManager roleManager)
        throws AuthorizationException, AuthenticationException, 
            ResourceLockedException, FailedDependencyException, IOException {
        if (lock != null) {
            recursiveLockAuthorize(principal, PrivilegeDefinition.WRITE,
                roleManager);
        }

        dao.delete(this);
    }

    public void recursiveLockAuthorize(Principal principal, String privilege,
        RoleManager roleManager)
        throws ResourceLockedException, FailedDependencyException, IOException, 
            AuthenticationException {
        if (lock != null) {
            lockAuthorize(principal, privilege, roleManager);
        }

        for (Iterator i = getChildren().iterator(); i.hasNext();) {
            try {
                Resource r = (Resource) i.next();

                if (r instanceof Collection) {
                    ((Collection) r).recursiveLockAuthorize(principal,
                        privilege, roleManager);
                } else {
                    r.lockAuthorize(principal, privilege, roleManager);
                }
            } catch (ResourceLockedException e) {
                throw new FailedDependencyException();
            } catch (AuthenticationException e) {
                throw new FailedDependencyException();
            }
        }
    }

    public void recursiveAuthorize(Principal principal, String privilege,
        RoleManager roleManager)
        throws AuthorizationException, AuthenticationException, IOException {
        authorize(principal, privilege, roleManager);

        for (Iterator i = getChildren().iterator(); i.hasNext();) {
            Resource r = (Resource) i.next();

            if (r instanceof Document) {
                r.authorize(principal, privilege, roleManager);
            } else {
                ((Collection) r).recursiveAuthorize(principal, privilege,
                    roleManager);
            }
        }
    }

    public void store(Principal principal,
        org.vortikal.repository.Resource dto, RoleManager roleManager)
        throws AuthenticationException, AuthorizationException, 
            ResourceLockedException, IllegalOperationException, IOException {
        acl.authorize(principal, PrivilegeDefinition.WRITE, dao, roleManager);

        if (lock != null) {
            lockAuthorize(principal, PrivilegeDefinition.WRITE, roleManager);
        }

        if (!this.owner.equals(dto.getOwner().getQualifiedName())) {
            /* Attempt to take ownership, only the owner of a parent
             * resource may do that, so do it in a secure manner: */
            setOwner(principal, dto, dto.getOwner().getQualifiedName(), roleManager);
        }

        setPropertiesModifiedBy(principal.getQualifiedName());

        if (!dto.getOverrideLiveProperties()) {
            setContentLastModified(new Date());
            setPropertiesLastModified(new Date());
        } else {
            setContentLastModified(dto.getContentLastModified());
            setPropertiesLastModified(dto.getPropertiesLastModified());

            if (logger.isDebugEnabled()) {
                logger.debug("Setting lastModified to supplied date: " +
                    dto.getLastModified());
            }
        }

        if (dto.getOverrideLiveProperties()) {
            setCreationTime(dto.getCreationTime());

            if (logger.isDebugEnabled()) {
                logger.debug("Setting creation time to supplied date: " +
                    dto.getCreationTime());
            }
        }

        setContentType(dto.getContentType());
        setCharacterEncoding(null);
        setDisplayName(dto.getDisplayName());
        setProperties(dto.getProperties());

        dao.store(this);
    }

    /**
     * Overridden version of getResourceDTO(), adding child pointers.
     *
     * @param principal a <code>Principal</code> value
     * @param roleManager a <code>RoleManager</code> value
     * @return a <code>org.vortikal.repository.Resource</code>
     * @exception IOException if an error occurs
     */
    public org.vortikal.repository.Resource getResourceDTO(
        Principal principal, PrincipalManager principalManager,
        RoleManager roleManager) throws IOException {

        org.vortikal.repository.Resource dto = super.getResourceDTO(
            principal, principalManager, roleManager);
        String[] children = new String[this.childURIs.length];

        System.arraycopy(this.childURIs, 0, children, 0, childURIs.length);

        dto.setChildURIs(children);

        return dto;
    }

    /**
     * Adds a URI to the child URI list.
     *
     * @param childURI a <code>String</code> value
     */
    private void addChildURI(String childURI) {
        synchronized (childURIs) {
            ArrayList l = new ArrayList();

            l.addAll(Arrays.asList(childURIs));
            l.add(childURI);

            String[] newChildren = (String[]) l.toArray(new String[] {  });

            this.childURIs = newChildren;
        }
    }
}
