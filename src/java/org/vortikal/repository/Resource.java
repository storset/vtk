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
package org.vortikal.repository;

import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


/**
 * This class encapsulates meta information about a resource, as a
 * data transfer object (DTO). Instances of this class are used to
 * communicate resource information between the repository
 * implementation and clients, in order to get and set
 * properties. Note that the repository is free to ignore properties
 * set herein if it wishes. Some properties such as
 * <code>contentLength</code> are so-called "live" properties, whose
 * values are enforced by the server, and depend on the physical
 * content of the resource itself. For example, a client may set this
 * value and perform a <code>store</code> operation providing an
 * instance of this class as a parameter, but since the value of
 * <code>contentLength</code> depends on the actual size of the
 * resource, the repository will not set this property internally.
 *
 * <p>Using the method <code>setOverrideLiveProperties</code>, this
 * behavior may be controlled to a certain extent, providing a hint
 * that properties such as <code>creationTime</code> should be set to
 * the values provided in the resource. The repository implementation
 * may still choose to ignore some of these properties.
 */
public class Resource implements java.io.Serializable, Cloneable {
    private boolean overrideLiveProperties = false;
    private String uri = null;
    private Date creationTime = null;
    private Date contentLastModified = null;
    private Date propertiesLastModified = null;
    private String name = null;
    private Principal owner = null;
    private Principal contentModifiedBy = null;
    private Principal propertiesModifiedBy = null;
    private long contentLength;
    private String[] children = null;
    private Lock[] activeLocks = new Lock[] {  };
    private PrivilegeDefinition supportedPrivileges = null;
    private Ace[] acl = null;
    private Ace[] parentACL = null;
    private Principal parentOwner = null;
    private AclRestrictions aclRestrictions = null;
    private String serial = null;
    private String displayName = null;
    private String characterEncoding = null;
    private String contentLanguage = null;
    private String contentType = null;
    private Property[] properties = null;

    /**
     * Gets the value of the <code>overrideLiveProperties</code> flag.
     */
    public boolean getOverrideLiveProperties() {
        return this.overrideLiveProperties;
    }

    /**
     * Sets the <code>overrideLiveProperties</code> flag, which
     * determines whether all live properties should be stored "as is"
     * from the resource DTO or not.
     */
    public void setOverrideLiveProperties(boolean overrideLiveProperties) {
        this.overrideLiveProperties = overrideLiveProperties;
    }

    /**
     * Gets this resource's URI.
     *
     * @return the URI
     */
    public String getURI() {
        return uri;
    }

    /**
     * Sets this resource's URI,
     *
     * @param uri the URI to set
     */
    public void setURI(String uri) {
        this.uri = uri;
    }

    /**
     * Returns the url for the parent.
     *
     * @return the URI
     */
    public String getParent() {
        String uri = getURI();

        if (uri.equals("/")) {
            return null;
        }

        String parentURI = uri.substring(0, uri.lastIndexOf("/") + 1);

        if (parentURI.endsWith("/") && !parentURI.equals("/")) {
            parentURI = parentURI.substring(0, parentURI.length() - 1);
        }

        return parentURI;
    }

    /**
     * Gets the creation time for this resource.
     *
     * @return the <code>Date</code> object representing the creation
     * time
     */
    public Date getCreationTime() {
        return creationTime;
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
     * Gets the date of this resource's last modification. The date
     * returned is either that of the
     * <code>getContentLastModified()</code> or the
     * <code>getPropertiesLastModified()</code> method, depending on
     * which one is the most recent.
     *
     * @return the time of last modification
     */
    public Date getLastModified() {
        if (contentLastModified.compareTo(propertiesLastModified) > 0) {
            return contentLastModified;
        }

        return propertiesLastModified;
    }

    /**
     * Gets the date of the last property modification.
     */
    public Date getContentLastModified() {
        return contentLastModified;
    }

    /**
     * Sets a resource's content modification time. The default
     * mechanism is to let the repository set contentLastModified on
     * operations that alter the resource's content (such as
     * <code>storeContent(...)</code>). Using this method the default
     * mechanism for setting <code>contentLastModified</code> is explicitly
     * oeverridden.
     *
     * NOTE: This method has no effect unless
     * <code>setOverrideLiveProperties(true)</code> is invoked first.
     *
     * @param contentLastModified the date to set
     * @see #setOverrideLiveProperties
     */
    public void setContentLastModified(Date contentLastModified) {
        this.contentLastModified = contentLastModified;
    }

    /**
     * Gets the date of the last content modification.
     */
    public Date getPropertiesLastModified() {
        return propertiesLastModified;
    }

    /**
     * Sets a resource's properties modification time. The default
     * mechanism is to let the repository set propertiesLastModified
     * on operations that alter the resource's properties (such as
     * <code>store(...)</code>). Using this method the default
     * mechanism for setting <code>propertiesLastModified</code> is
     * explicitly oeverridden.
     *
     * NOTE: This method has no effect unless
     * <code>setOverrideLiveProperties(true)</code> is invoked first.
     *
     * @param propertiesLastModified the date to set
     * @see #setOverrideLiveProperties
     */
    public void setPropertiesLastModified(Date propertiesLastModified) {
        this.propertiesLastModified = propertiesLastModified;
    }

    /**
     * Gets this resource's name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets a resource's name.
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets a resource's owner.
     *
     */
    public Principal getOwner() {
        return owner;
    }

    /**
     * Sets a resource's owner.
     *
     * @param owner the name to set
     */
    public void setOwner(Principal owner) {
        this.owner = owner;
    }

    /**
     * Gets the name of the principal that last modified either the
     * content or the properties of this resource.
     *
     * @return the name of the principal
     */
    public Principal getModifiedBy() {
        if (contentLastModified.compareTo(propertiesLastModified) > 0) {
            return contentModifiedBy;
        }

        return propertiesModifiedBy;
    }

    /**
     * Gets the name of the principal that last modified the
     * resource's content.
     */
    public Principal getContentModifiedBy() {
        return contentModifiedBy;
    }

    /**
     * Sets the name of the principal that last modified the
     * resource's content.
     *
     * @param contentModifiedBy the name to set
     */
    public void setContentModifiedBy(Principal contentModifiedBy) {
        this.contentModifiedBy = contentModifiedBy;
    }

    /**
     * Gets the name of the principal that last modified the
     * resource's properties.
     */
    public Principal getPropertiesModifiedBy() {
        return propertiesModifiedBy;
    }

    /**
     * Sets the name of the principal that last modified the
     * resource's properties.
     *
     * @param propertiesModifiedBy the name to set
     */
    public void setPropertiesModifiedBy(Principal propertiesModifiedBy) {
        this.propertiesModifiedBy = propertiesModifiedBy;
    }

    /**
     * Gets the size of a resource.
     *
     * @return the size of the resource's content (in bytes)
     */
    public long getContentLength() {
        return contentLength;
    }

    /**
     * Sets the content length of a resource.
     *
     * @param contentLength the value to set
     */
    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    /**
     * Gets the list of children. These are "soft" references, listing
     * the URIs of the children.
     *
     * @return the children's URIs, or <code>null</code> if the
     * resource is not a collection
     */
    public String[] getChildURIs() {
        return this.children;
    }

    /**
     * Sets the list of child URIs.
     *
     * @param uris the children's URIs
     */
    public void setChildURIs(String[] uris) {
        this.children = uris;
    }

    /**
     * Gets this resource's display name.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets a resource's display name.
     *
     * @param displayName the name to set
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Gets a resource's content language.
     *
     * @return the language (if it has one, <code>null</code>
     * otherwise)
     */
    public String getContentLanguage() {
        return contentLanguage;
    }

    /**
     * Sets this resource's content language.
     *
     * @param contentLanguage the language to use
     */
    public void setContentLanguage(String contentLanguage) {
        this.contentLanguage = contentLanguage;
    }

    /**
     * Gets a resource's content (MIME) type.
     *
     * @return the content type
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Sets a resource's content (MIME) type.
     *
     * @param contentType the MIME type to set
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Gets a resource's serial string.  A serial string is a unique
     * string which changes on each write-operation on the resource,
     * and may be used for instance as an ETag on that resource.
     *
     */
    public String getSerial() {
        return this.serial;
    }

    /**
     * Sets a resource's serial string.
     *
     * @param serial the serial to set
     */
    public void setSerial(String serial) {
        this.serial = serial;
    }

    /**
     * Returns a listing of lock information for the resource.
     *
     * @return an array of <code>Lock</code> objects representing
     * the active locks that are set on the resource
     */
    public Lock[] getActiveLocks() {
        return activeLocks;
    }

    /**
     * Sets the set of active locks on a resource.
     *
     * @param activeLocks an array of <code>Lock[]</code> objects
     */
    public void setActiveLocks(Lock[] activeLocks) {
        this.activeLocks = activeLocks;
    }

    /**
     * Determines whether this resource is a collection.
     *
     * @return a <code>true</code> if this resource is a collection,
     * <code>false</code> otherwise
     */
    public boolean isCollection() {
        return "application/x-vortex-collection".equals(getContentType());
    }

    /**
     * Gets this resource's set of supported lock types.
     *
     * @return an array of <code>String[]</code> objects, whose values
     * are defined in <code>Lock</code>
     */
    public String[] getSupportedLocks() {
        return new String[] { Lock.LOCKTYPE_EXCLUSIVE_WRITE };
    }

    /**
     * Gets this resource's set of supported privileges.
     *
     * @return a <code>PrivilegeDefinition</code> tree structure
     * representing the supported privilege set.
     *
     */
    public PrivilegeDefinition getSupportedPrivileges() {
        return this.supportedPrivileges;
    }

    /**
     * Sets this resource's set of supported privileges.
     *
     */
    public void setSupportedPrivileges(PrivilegeDefinition supportedPrivileges) {
        this.supportedPrivileges = supportedPrivileges;
    }

    /**
     * Gets the ACL restrictions of a resource.
     *
     * @return the ACL restrictions object.
     * @see AclRestrictions
     */
    public AclRestrictions getAclRestrictions() {
        return this.aclRestrictions;
    }

    /**
     * Sets this resource's ACL restrictions.
     *
     * @param aclRestrictions the restrictions to set.
     * @see AclRestrictions
     */
    public void setAclRestrictions(AclRestrictions aclRestrictions) {
        this.aclRestrictions = aclRestrictions;
    }

    /**
     * Gets the set of privileges on this resource for a given principal.
     */
    public Privilege[] getPrivilegeSet(Principal principal,
                                       PrincipalStore principalStore) {
        Privilege[] privileges = getPrivilegeSetInternal(principal, this.acl,
                                                         this.owner,
                                                         principalStore);

        return privileges;
    }

    /**
     * Gets the set of privileges on this resource's parent for a given principal.
     */
    public Privilege[] getParentPrivilegeSet(Principal principal,
                                            PrincipalStore principalStore) {
        Privilege[] privileges = getPrivilegeSetInternal(principal,
                                                         this.parentACL,
                                                         this.parentOwner,
                                                         principalStore);

        return privileges;
    }

    /**
     * Sets the ACL of this resource. This value is ignored when
     * storing the resource.
     *
     * @param acl the ACL
     */
    public void setACL(Ace[] acl) {
        this.acl = acl;
    }

    /**
     * Sets the ACL of this resource's parent resource. This value
     * is ignored when storing the resource.
     *
     * @param parentACL the parent's ACL
     */
    public void setParentACL(Ace[] parentACL) {
        this.parentACL = parentACL;
    }

    /**
     * Sets the owner of this resource's parent resource. This value
     * is ignored when storing the resource.
     *
     * @param parentOwner a <code>String</code> value
     */
    public void setParentOwner(Principal parentOwner) {
        this.parentOwner = parentOwner;
    }

    /**
     * Gets this resource's set of custom metadata.
     *
     * @return an array of <code>Property</code> objects,
     * @see Property
     */
    public Property[] getProperties() {
        return this.properties;
    }

    /**
     * Sets this resource's set of custom metadata.
     *
     * @param properties an array of <code>Property</code> objects
     * to set
     * @see Property
     */
    public void setProperties(Property[] properties) {
        this.properties = properties;
    }

    /**
     * Utility method for retrieving a property.
     *
     * @param namespace the namespace of the property
     * @param name the name of the property
     * @return the property having the given namespace or name, or
     * <code>null</code> if no such propery exists for this resource.
     */
    public Property getProperty(String namespace, String name) {
        for (int i = 0; i < properties.length; i++) {
            if (properties[i].getNamespace().equals(namespace) &&
                    properties[i].getName().equals(name)) {
                return properties[i];
            }
        }

        return null;
    }

    /**
     * Utility method for setting a property. If the property does not
     * already exist on this resource, it is added. If a property that
     * has the same name and namespace already exists on this
     * resource, its value will be overwritten.
     *
     * @param property the property to set
     */
    public void setProperty(Property property) {
        ArrayList propertiesList = new ArrayList(Arrays.asList(getProperties()));

        Property theProperty = null;

        for (Iterator i = propertiesList.iterator(); i.hasNext();) {
            Property currProperty = (Property) i.next();

            if (currProperty.getName().equals(property.getName()) &&
                    currProperty.getNamespace().equals(property.getNamespace())) {
                theProperty = currProperty;

                break;
            }
        }

        if (theProperty == null) {
            theProperty = property;
            propertiesList.add(theProperty);
        } else {
            theProperty.setValue(property.getValue());
        }

        this.setProperties((Property[]) propertiesList.toArray(
                new Property[] {  }));
    }

    public void removeProperty(String namespace, String name) {
        ArrayList propertiesList = new ArrayList(Arrays.asList(getProperties()));

        for (Iterator i = propertiesList.iterator(); i.hasNext();) {
            Property currProperty = (Property) i.next();

            if (currProperty.getName().equals(name) &&
                    currProperty.getNamespace().equals(namespace)) {
                i.remove();

                break;
            }
        }

        this.setProperties((Property[]) propertiesList.toArray(
                new Property[] {  }));
    }

    /**
     * Gets the character encoding. This value is only relevant if the
     * content type of the resource matches 'text/*'.
     *
     * @return the value of characterEncoding
     */
    public String getCharacterEncoding() {
        return this.characterEncoding;
    }

    /**
     * Sets the character encoding. The value is ignored unless the
     * content type of the resource matches 'text/*'.
     *
     * @param characterEncoding Value to assign to this.characterEncoding
     */
    public void setCharacterEncoding(String characterEncoding) {
        this.characterEncoding = characterEncoding;
    }

    public Object clone() throws CloneNotSupportedException {
        Resource clone = (Resource) super.clone();

        Lock[] cloneLocks = new Lock[this.activeLocks.length];

        for (int i = 0; i < activeLocks.length; i++) {
            cloneLocks[i] = (Lock) activeLocks[i].clone();
        }

        clone.activeLocks = cloneLocks;

        clone.supportedPrivileges = (PrivilegeDefinition) this.supportedPrivileges.clone();

        Ace[] clonedACL = new Ace[this.acl.length];

        for (int i = 0; i < this.acl.length; i++) {
            clonedACL[i] = (Ace) this.acl[i].clone();
        }

        clone.setACL(clonedACL);

        Ace[] clonedParentACL = new Ace[this.parentACL.length];

        for (int i = 0; i < this.parentACL.length; i++) {
            clonedParentACL[i] = (Ace) this.parentACL[i].clone();
        }

        clone.setParentACL(clonedParentACL);

        clone.aclRestrictions = (AclRestrictions) this.aclRestrictions.clone();

        Property[] cloneProperties = new Property[this.properties.length];

        for (int i = 0; i < properties.length; i++) {
            cloneProperties[i] = (Property) properties[i].clone();
        }

        clone.properties = cloneProperties;

        return clone;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append(this.getClass().getName()).append(": ");
        sb.append("[uri: ").append(uri);
        sb.append("]");

        return sb.toString();
    }

    public String toStringDetailed() {
        StringBuffer sb = new StringBuffer();

        sb.append(this.getClass().getName()).append(": ");
        sb.append("[uri: ").append(uri);
        sb.append(", creationTime = ").append(creationTime);
        sb.append(", contentLastModified = ").append(contentLastModified);
        sb.append(", propertiesLastModified = ").append(propertiesLastModified);
        sb.append(", name = ").append(name);
        sb.append(", owner = ").append(owner);
        sb.append(", contentModifiedBy = ").append(contentModifiedBy);
        sb.append(", propertiesModifiedBy = ").append(propertiesModifiedBy);
        sb.append(", contentLength = ").append(contentLength);
        sb.append(", serial = ").append(serial);
        sb.append(", displayName = ").append(displayName);
        sb.append(", contentLanguage = ").append(contentLanguage);
        sb.append(", contentType = ").append(contentType);
        sb.append(", activeLocks = ").append(activeLocks);
        sb.append(", supportedPrivileges = ").append(supportedPrivileges);

        //sb.append(", currentUserPrivilegeSet = ").append(currentUserPrivilegeSet);
        sb.append(", acl = ").append(acl);
        sb.append(", parentACL").append(parentACL);
        sb.append(", aclRestrictions = ").append(aclRestrictions);
        sb.append(", properties = ").append(properties);
        sb.append("]");

        return sb.toString();
    }

    private Privilege[] getPrivilegeSetInternal(
        Principal principal, Ace[] acl, Principal owner,
        PrincipalStore principalStore) {

        Set privileges = new HashSet();

        for (int i = 0; i < acl.length; i++) {
            ACLPrincipal p = acl[i].getPrincipal();

            switch (p.getType()) {
            case ACLPrincipal.TYPE_ALL:

                if (acl[i].isGranted()) {
                    privileges.addAll(Arrays.asList(acl[i].getPrivileges()));
                }

                break;

            case ACLPrincipal.TYPE_AUTHENTICATED:

                if ((principal != null) && acl[i].isGranted()) {
                    privileges.addAll(Arrays.asList(acl[i].getPrivileges()));
                }

                break;

            case ACLPrincipal.TYPE_UNAUTHENTICATED:

                if ((principal == null) && acl[i].isGranted()) {
                    privileges.addAll(Arrays.asList(acl[i].getPrivileges()));
                }

                break;

            case ACLPrincipal.TYPE_SELF:

                if ((principal != null) && acl[i].isGranted()) {
                    privileges.addAll(Arrays.asList(acl[i].getPrivileges()));
                }

                break;

            case ACLPrincipal.TYPE_OWNER:

                if ((principal != null) && principal.equals(owner) &&
                        acl[i].isGranted()) {
                    privileges.addAll(Arrays.asList(acl[i].getPrivileges()));
                }

                break;

            case ACLPrincipal.TYPE_URL:

                
                if (p.isUser()) {
                    if ((principal != null) &&
                        principal.getQualifiedName().equals(p.getURL()) &&
                        acl[i].isGranted()) {
                        privileges.addAll(Arrays.asList(acl[i].getPrivileges()));
                    }
                } else {
                    if ((principal != null) && acl[i].isGranted() &&
                        principalStore.isMember(principal,
                                                p.getURL())) {
                        privileges.addAll(Arrays.asList(acl[i].getPrivileges()));
                    }
                }

                break;

            default:
                break;
            }
        }

        return (Privilege[]) privileges.toArray(new Privilege[0]);
    }
}
