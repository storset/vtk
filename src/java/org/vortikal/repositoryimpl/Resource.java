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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.Ace;
import org.vortikal.repository.AclException;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.FailedDependencyException;
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


public abstract class Resource implements Cloneable {
    public final static org.vortikal.repository.PrivilegeDefinition standardPrivilegeDefinition;
    public final static org.vortikal.repository.AclRestrictions standardRestrictions;
    public final static String CUSTOM_NAMESPACE = "uio";
    public final static String CUSTOM_PRIVILEGE_READ_PROCESSED = "read-processed";

    static {
        /*
         * Declare the standard ACL supported privilege tree (will be
         * the same for all resources):
         *
         * [dav:all] (abstract)
         *     |
         *     |---[dav:read]
         *     |       |
         *     |       `---[uio:read-processed]
         *     |
         *     |---[dav:write]
         *     |
         *     `---[dav:write-acl]
         *
         */
        standardPrivilegeDefinition = new org.vortikal.repository.PrivilegeDefinition();

        org.vortikal.repository.PrivilegeDefinition all = new org.vortikal.repository.PrivilegeDefinition();

        all.setName(org.vortikal.repository.PrivilegeDefinition.ALL);
        all.setNamespace(org.vortikal.repository.PrivilegeDefinition.STANDARD_NAMESPACE);
        all.setAbstractACE(true);

        org.vortikal.repository.PrivilegeDefinition read = new org.vortikal.repository.PrivilegeDefinition();

        read.setName(org.vortikal.repository.PrivilegeDefinition.READ);
        read.setNamespace(org.vortikal.repository.PrivilegeDefinition.STANDARD_NAMESPACE);
        read.setAbstractACE(false);

        org.vortikal.repository.PrivilegeDefinition readProcessed = new org.vortikal.repository.PrivilegeDefinition();

        readProcessed.setName(CUSTOM_PRIVILEGE_READ_PROCESSED);
        readProcessed.setNamespace(CUSTOM_NAMESPACE);
        readProcessed.setAbstractACE(false);
        read.setMembers(new org.vortikal.repository.PrivilegeDefinition[] {
                readProcessed
            });

        org.vortikal.repository.PrivilegeDefinition write = new org.vortikal.repository.PrivilegeDefinition();

        write.setName(org.vortikal.repository.PrivilegeDefinition.WRITE);
        write.setNamespace(org.vortikal.repository.PrivilegeDefinition.STANDARD_NAMESPACE);
        write.setAbstractACE(false);

        org.vortikal.repository.PrivilegeDefinition writeACL = new org.vortikal.repository.PrivilegeDefinition();

        writeACL.setName(org.vortikal.repository.PrivilegeDefinition.WRITE_ACL);
        writeACL.setNamespace(org.vortikal.repository.PrivilegeDefinition.STANDARD_NAMESPACE);
        writeACL.setAbstractACE(false);

        org.vortikal.repository.PrivilegeDefinition[] members = new org.vortikal.repository.PrivilegeDefinition[3];

        members[0] = read;
        members[1] = write;
        members[2] = writeACL;

        all.setMembers(members);

        /* Set ACL restrictions: */
        standardRestrictions = new org.vortikal.repository.AclRestrictions();
        standardRestrictions.setGrantOnly(true);
        standardRestrictions.setNoInvert(true);
        standardRestrictions.setPrincipalOnlyOneAce(true);

        org.vortikal.repository.ACLPrincipal owner = new org.vortikal.repository.ACLPrincipal();

        owner.setType(org.vortikal.repository.ACLPrincipal.TYPE_OWNER);

        org.vortikal.repository.ACLPrincipal[] requiredPrincipals = new org.vortikal.repository.ACLPrincipal[] {
                owner
            };

        standardRestrictions.setRequiredPrincipals(requiredPrincipals);
    }

    protected Log logger = LogFactory.getLog(this.getClass());

    /* Numeric ID, required by database */
    private int id = -1;
    protected DataAccessor dao;
    protected String uri = null;
    protected String owner = null;
    protected String contentModifiedBy = null;
    protected String propertiesModifiedBy = null;
    protected ACL acl = null;
    protected boolean inheritedACL = true;
    protected Lock lock = null;
    protected Date creationTime = null;
    protected Date contentLastModified = null;
    protected Date propertiesLastModified = null;
    protected String name;
    protected PrincipalManager principalManager;
    protected String displayName = "";
    protected String contentType = "";
    protected String characterEncoding = null;
    protected Vector properties = new Vector();
    protected boolean dirtyACL = false;

    public Resource(String uri, String owner, String contentModifiedBy,
        String propertiesModifiedBy, ACL acl, boolean inheritedACL, Lock lock,
        DataAccessor dao, PrincipalManager principalManager) {
        this.uri = uri;
        this.owner = owner;
        this.contentModifiedBy = contentModifiedBy;
        this.propertiesModifiedBy = propertiesModifiedBy;
        this.acl = acl;
        this.inheritedACL = inheritedACL;
        this.lock = lock;
        this.dao = dao;

        if (this.uri.equals("/")) {
            this.name = uri;
        } else {
            this.name = uri.substring(uri.lastIndexOf("/") + 1, uri.length());
        }

        this.owner = owner;
        this.principalManager = principalManager;
        acl.setResource(this);
    }

    public abstract Object clone() throws CloneNotSupportedException;

    /**
     * Persists a resource.
     *
     * @param principal a <code>Principal</code> value
     * @param dto a <code>org.vortikal.repository.Resource</code> value
     * @exception AuthenticationException if an error occurs
     * @exception AuthorizationException if an error occurs
     * @exception ResourceLockedException if an error occurs
     * @exception IllegalOperationException if an error occurs
     * @exception IOException if an error occurs
     */
    public abstract void store(Principal principal,
        org.vortikal.repository.Resource dto, RoleManager roleManager)
        throws AuthenticationException, AuthorizationException, 
            ResourceLockedException, IllegalOperationException, IOException;

    public void setACL(ACL acl) {
        this.acl = acl;
        acl.setResource(this);
    }

    public void setDataAccessor(DataAccessor dao) {
        this.dao = dao;
    }

    public ACL getACL() {
        return this.acl;
    }

    public Lock getLock() {
        return this.lock;
    }

    public void setLock(Lock lock) {
        this.lock = lock;
    }

    public String getOwner() {
        return this.owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getContentModifiedBy() {
        return this.contentModifiedBy;
    }

    public void setContentModifiedBy(String contentModifiedBy) {
        this.contentModifiedBy = contentModifiedBy;
    }

    public String getPropertiesModifiedBy() {
        return this.propertiesModifiedBy;
    }

    public void setPropertiesModifiedBy(String propertiesModifiedBy) {
        this.propertiesModifiedBy = propertiesModifiedBy;
    }

    /**
     * Sets a lock on a resource.
     *
     * @param principal the <code>Principal</code> wishing to set the lock
     * @param ownerInfo a user supplied value describing the owner
     * (contact info, etc.)
     * @param depth a <code>String</code> specifying the lock depth
     * (legal values are 0, 1 or Infinite).
     * @param desiredTimeoutSeconds the number of seconds before timeout
     * @return the lock token
     * @exception AuthenticationException if an error occurs
     * @exception AuthorizationException if an error occurs
     * @exception ResourceLockedException if an error occurs
     * @exception IOException if an error occurs
     */
    public String lock(Principal principal, String ownerInfo, String depth,
                       int desiredTimeoutSeconds, RoleManager roleManager, boolean refresh)
        throws AuthenticationException, AuthorizationException, 
            ResourceLockedException, IOException {
        authorize(principal,
            org.vortikal.repository.PrivilegeDefinition.WRITE, roleManager);

        if (this.lock != null) {
            lockAuthorize(principal,
                org.vortikal.repository.PrivilegeDefinition.WRITE, roleManager);
            if (!refresh) {
                this.lock = null;
                this.dao.store(this);
            }
        }

        if (this.lock == null) {
            this.lock = new Lock(principal, ownerInfo, depth,
                new Date(System.currentTimeMillis() +
                         (desiredTimeoutSeconds * 1000)));
        } else {
            this.lock = new Lock(
                this.lock.getLockToken(), principal.getQualifiedName(),
                ownerInfo, depth, 
                new Date(System.currentTimeMillis() + (desiredTimeoutSeconds * 1000)));
        }

//         setLock(new Lock(principal, ownerInfo, depth,
//                 new Date(System.currentTimeMillis() +
//                     (desiredTimeoutSeconds * 1000))));
        this.dao.store(this);
        return this.lock.getLockToken();
    }

    public void unlock(Principal principal, String lockToken,
        RoleManager roleManager)
        throws AuthenticationException, AuthorizationException, 
            ResourceLockedException, IOException {
        this.authorize(
            principal, org.vortikal.repository.PrivilegeDefinition.WRITE, roleManager);

        if (this.lock != null) {
            if (!roleManager.hasRole(principal.getQualifiedName(), RoleManager.ROOT)) {
                this.lockAuthorize(principal, org.vortikal.repository.PrivilegeDefinition.WRITE,
                                   roleManager);
            }

            this.lock = null;
            this.dao.store(this);
        }
    }

    public String getURI() {
        return this.uri;
    }

    public int getID() {
        return this.id;
    }

    public void setID(int id) {
        this.id = id;
    }

    public boolean getInheritedACL() {
        return this.inheritedACL;
    }

    public void setInheritedACL(boolean inheritedACL) {
        this.inheritedACL = inheritedACL;
    }

    /**
     * Gets the creation time for this resource.
     *
     * @return the <code>Date</code> object representing the creation
     * time
     */
    public Date getCreationTime() {
        return this.creationTime;
    }

    /**
     * Sets a resource's creation time.
     *
     * @param creationTime the date to set
     */
    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    /**
     * Gets the date of this resource's last content modification.
     *
     * @return the time of last modification
     */
    public Date getContentLastModified() {
        return this.contentLastModified;
    }

    /**
     * Sets a resource's content modification time.
     *
     * @param contentLastModified the date to set
     */
    public void setContentLastModified(Date contentLastModified) {
        this.contentLastModified = contentLastModified;
    }

    /**
     * Gets the date of this resource's last properties modification.
     *
     * @return the time of last modification
     */
    public Date getPropertiesLastModified() {
        return this.propertiesLastModified;
    }

    /**
     * Sets a resource's properties modification time.
     *
     * @param propertiesLastModified the date to set
     */
    public void setPropertiesLastModified(Date propertiesLastModified) {
        this.propertiesLastModified = propertiesLastModified;
    }

    /**
     * Gets this resource's display name.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return ((this.displayName == null) || this.displayName.equals("")) ? this.name
                                                                 : this.displayName;
    }

    /**
     * Sets a resource's display name.
     *
     * @param displayName the name to set
     */
    public void setDisplayName(String displayName) {
        if (!((displayName == null) || displayName.equals(""))) {
            this.displayName = displayName;
        }
    }

    /**
     * Gets a resource's content (MIME) type.
     *
     * @return the content type
     */
    public String getContentType() {
        return this.contentType;
    }

    public String getName() {
        return this.name;
    }

    public String getCharacterEncoding() {
        return this.characterEncoding;
    }

    public void setCharacterEncoding(String characterEncoding) {
        this.characterEncoding = characterEncoding;
    }

    /**
     * Sets a resource's content (MIME) type.
     *
     * @param contentType the MIME type to set
     */
    public void setContentType(String contentType) {
        if (!(this instanceof Collection)) {
            this.contentType = contentType;
        } else {
            this.contentType = "application/x-vortex-collection";
        }
    }

    public String getParentURI() {
        return getParent(this.uri);
    }

    public static String getParent(String uri) {
        if (uri == null) {
            return null;
        }

        if ("/".equals(uri)) {
            return null;
        }

        String parentURI = uri.substring(0, uri.lastIndexOf("/") + 1);

        if (parentURI.endsWith("/") && !parentURI.equals("/")) {
            parentURI = parentURI.substring(0, parentURI.length() - 1);
        }

        return parentURI;
    }

    public void addProperty(String namespace, String name, String value) {
        Vector property = null;

        for (Iterator i = this.properties.iterator(); i.hasNext();) {
            Vector v = (Vector) i.next();
            String existingNamespace = (String) v.get(0);
            String existingName = (String) v.get(1);

            if (existingNamespace.equals(namespace) &&
                    existingName.equals(name)) {
                property = v;
            }
        }

        if (property == null) {
            property = new Vector();
            property.add(namespace);
            property.add(name);
            property.add(value);
            this.properties.add(property);
        } else {
            property.remove(2);
            property.add(value);
        }
    }

    public Vector getProperties() {
        return this.properties;
    }

    public void setProperties(org.vortikal.repository.Property[] properties) {
        this.properties = new Vector();

        for (int i = 0; i < properties.length; i++) {
            String namespace = properties[i].getNamespace();

            // FIXME handle the protected DAV: namespace this in a
            // more formalized manner:
            if ("dav".equals(namespace.toLowerCase())) {
                throw new IllegalOperationException("Cannot set property '" +
                    properties[i].getNamespace() + ":" +
                    properties[i].getName() + "': namespace is protected.");
            }

            String name = properties[i].getName();
            String value = properties[i].getValue();

            addProperty(namespace, name, value);
        }
    }

    protected org.vortikal.repository.Property[] getPropertyDTOs() {
        ArrayList dtoList = new ArrayList();

        for (Iterator i = this.properties.iterator(); i.hasNext();) {
            org.vortikal.repository.Property dto = new org.vortikal.repository.Property();
            Vector element = (Vector) i.next();
            String namespace = (String) element.get(0);
            String name = (String) element.get(1);
            String value = (String) element.get(2);

            dto.setNamespace(namespace);
            dto.setName(name);
            dto.setValue(value);

            dtoList.add(dto);
        }

        return (org.vortikal.repository.Property[])
            dtoList.toArray(new org.vortikal.repository.Property[0]);
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
     * Adds root and read everything roles to ACL
     *
     * @param originalACL an <code>Ace[]</code> value
     * @return an <code>Ace[]</code>
     */
    private Ace[] addRolesToACL(ACL originalACL, RoleManager roleManager) {
        List acl = new ArrayList(Arrays.asList(originalACL.toAceList()));
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

    public org.vortikal.repository.Resource getResourceDTO(
        Principal principal, PrincipalManager principalManager,
        RoleManager roleManager) throws IOException {
        org.vortikal.repository.Resource dto = new org.vortikal.repository.Resource();

        dto.setURI(getURI());
        dto.setCreationTime(getCreationTime());
        dto.setContentLastModified(getContentLastModified());
        dto.setPropertiesLastModified(getPropertiesLastModified());
        dto.setContentModifiedBy(principalManager.getPrincipal(getContentModifiedBy()));
        dto.setPropertiesModifiedBy(principalManager.getPrincipal(getPropertiesModifiedBy()));
        dto.setContentType(getContentType());
        dto.setCharacterEncoding(getCharacterEncoding());
        dto.setDisplayName(getDisplayName());
        dto.setActiveLocks((this.lock == null)
            ? new org.vortikal.repository.Lock[] {  }
            : new org.vortikal.repository.Lock[] { this.lock.getLockDTO(principalManager) });
        dto.setName(this.name);
        dto.setOwner(principalManager.getPrincipal(this.owner));
        dto.setSupportedPrivileges(standardPrivilegeDefinition);
        dto.setAclRestrictions(standardRestrictions);
        dto.setProperties(getPropertyDTOs());

        try {
            ACL originalACL = (ACL) this.acl.clone();

            dto.setACL(addRolesToACL(originalACL, roleManager));

            if ("/".equals(this.uri)) {
                dto.setParentACL(new Ace[0]);
            } else {
                Resource parent = this.dao.load(getParentURI());
                ACL parentACL = (ACL) parent.getACL().clone();

                dto.setParentACL(addRolesToACL(parentACL, roleManager));
                dto.setParentOwner(principalManager.getPrincipal(parent.getOwner()));
            }
        } catch (CloneNotSupportedException e) {
        }

        return dto;
    }

    public boolean isCollection() {
        return "application/x-vortex-collection".equals(getContentType());
    }

    public void delete(Principal principal, RoleManager roleManager)
        throws AuthorizationException, AuthenticationException, 
            ResourceLockedException, FailedDependencyException, IOException {
        if (this.lock != null) {
            lockAuthorize(principal,
                org.vortikal.repository.PrivilegeDefinition.WRITE, roleManager);
        }

        try {
            this.dao.delete(this);
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    protected void setOwner(Principal principal,
        org.vortikal.repository.Resource dto, String owner,
        RoleManager roleManager)
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
        if (!(roleManager.hasRole(principal.getQualifiedName(), RoleManager.ROOT) ||
                principal.getQualifiedName().equals(this.owner))) {
            throw new AuthorizationException("Principal " +
                principal.getQualifiedName() + " is not allowed to set owner of " +
                "resource " + this.uri);
        }

        Principal principal2 = null;
        
        try {
            principal2 = principalManager.getPrincipal(owner);
        } catch (InvalidPrincipalException e) {
            throw new IllegalOperationException(
                    "Unable to set owner of resource " + this.uri +
                    ": invalid owner: '" + owner + "'");
        }
        
        if (!principalManager.validatePrincipal(principal2)) {
            throw new IllegalOperationException(
                "Unable to set owner of resource " + this.uri +
                ": invalid owner: '" + owner + "'");
        }

        this.owner = owner;
    }

    public void storeACL(Principal principal,
        org.vortikal.repository.Ace[] aceList, RoleManager roleManager)
        throws AuthorizationException, AuthenticationException, 
            IllegalOperationException, IOException, AclException {
        authorize(principal,
            org.vortikal.repository.PrivilegeDefinition.WRITE_ACL, roleManager);

        acl.validateACL(aceList);

        this.acl = acl.buildACL(aceList);
        this.acl.setResource(this);

        /* If the first ACE has set inheritance, we know that the
         * whole ACL has valid inheritance (ACL.validateACL() ensures
         * this), so we can go ahead and set it here: */
        this.inheritedACL = aceList[0].getInheritedFrom() != null;

        if (!"/".equals(this.uri) && this.inheritedACL) {
            /* When the ACL is inherited, make our ACL a copy of our
             * parent's ACL, since the supplied one may contain other
             * ACEs than the one we now inherit from. */
            try {
                ACL parentACL = (ACL) this.dao.load(getParentURI()).getACL().clone();

                parentACL.setResource(this);
                this.acl = parentACL;
            } catch (CloneNotSupportedException e) {
            }
        }

        try {
            this.dirtyACL = true;

            this.dao.store(this);
        } catch (Exception e) {
            
            throw new IOException(e.getMessage());
        } finally {
            this.dirtyACL = false;
        }
    }

    public boolean dirtyACL() {
        return this.dirtyACL;
    }

    public void lockAuthorize(Principal principal, String privilege,
        RoleManager roleManager)
        throws AuthenticationException, ResourceLockedException {
        if (this.lock != null) {
            this.lock.authorize(principal, privilege, roleManager);
        }
    }

    public void authorize(Principal principal, String privilege,
        RoleManager roleManager)
        throws AuthorizationException, AuthenticationException, IOException {
        this.acl.authorize(principal, privilege, this.dao, roleManager);
    }
}
