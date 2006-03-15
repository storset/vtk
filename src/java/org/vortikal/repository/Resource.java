package org.vortikal.repository;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalStore;
import org.vortikal.util.repository.URIUtil;

public interface Resource extends PropertySet {

    public Property createProperty(String namespace, String name);
    
    public void deleteProperty(Property property);
    
    public void removeProperty(String namespace, String name);
    
    // XXX: What to do about this?! Old client code operates with a divison of know/unkown props.
    public List getOtherProperties();
    
    // Old stuff from dto
    
    /**
     * Gets a resource's serial string.  A serial string is a unique
     * string which changes on each write-operation on the resource,
     * and may be used for instance as an ETag on that resource.
     *
     */
    public String getSerial();

    /**
     * Returns the uri of the parent.
     *
     * @return the URI
     */
    public String getParent();

    /**
     * Gets the creation time for this resource.
     *
     * @return the <code>Date</code> object representing the creation
     * time
     */
    public Date getCreationTime();

    /**
     * Gets the date of this resource's last modification. The date
     * returned is either that of the
     * <code>getContentLastModified()</code> or the
     * <code>getPropertiesLastModified()</code> method, depending on
     * which one is the most recent.
     *
     * @return the time of last modification
     */
    public Date getLastModified();

    /**
     * Gets the date of the last property modification.
     */
    public Date getContentLastModified();

    /**
     * Gets the date of the last content modification.
     */
    public Date getPropertiesLastModified();

    /**
     * Gets this resource's name.
     *
     * @return the name
     */
    public String getName();
    
    /**
     * Gets a resource's owner.
     *
     */
    public Principal getOwner();
    
    /**
     * Gets the name of the principal that last modified either the
     * content or the properties of this resource.
     *
     * @return the name of the principal
     */
    public Principal getModifiedBy();
    
    /**
     * Gets the name of the principal that last modified the
     * resource's content.
     */
    public Principal getContentModifiedBy();

    /**
     * Gets the name of the principal that last modified the
     * resource's properties.
     */
    public Principal getPropertiesModifiedBy();
    
    /**
     * Gets the size of a resource.
     *
     * @return the size of the resource's content (in bytes)
     */
    public long getContentLength();
    
    /**
     * Gets the list of children. These are "soft" references, listing
     * the URIs of the children.
     *
     * @return the children's URIs, or <code>null</code> if the
     * resource is not a collection
     */
    public String[] getChildURIs();

    /**
     * Gets a resource's content locale.
     *
     * @return the locale (if it has one, <code>null</code>
     * otherwise)
     */
    public Locale getContentLocale();

    /**
     * Gets this resource's display name.
     *
     * @return the display name
     */
    public String getDisplayName();

    /**
     * Gets a resource's content (MIME) type.
     *
     * @return the content type
     */
    public String getContentType();
    
    /**
     * Returns a listing of lock information for the resource.
     *
     * @return an array of <code>Lock</code> objects representing
     * the active locks that are set on the resource
     */
    public Lock getActiveLock();
    
    /**
     * Determines whether this resource is a collection.
     *
     * @return a <code>true</code> if this resource is a collection,
     * <code>false</code> otherwise
     */
    public boolean isCollection();
    
    /**
     * Gets this resource's set of supported privileges.
     *
     * @return a <code>PrivilegeDefinition</code> tree structure
     * representing the supported privilege set.
     *
     */
    public PrivilegeDefinition getSupportedPrivileges();
    
    /**
     * Gets the ACL restrictions of a resource.
     *
     * @return the ACL restrictions object.
     * @see AclRestrictions
     */
    public AclRestrictions getAclRestrictions();

    public Acl getAcl();
    
    /**
     * Gets the character encoding. This value is only relevant if the
     * resource type is 'textResource'.
     *
     * @return the value of characterEncoding
     */
    public String getCharacterEncoding();
    
    
    public void setCharacterEncoding(String characterEncoding);

    public void setContentLocale(Locale locale);

    public void setContentType(String string);

    public void setOwner(Principal principal);

    public void setDisplayName(String text);
    
}
