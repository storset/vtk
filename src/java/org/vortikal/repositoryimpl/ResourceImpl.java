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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.vortikal.repository.Acl;
import org.vortikal.repository.AuthorizationManager;
import org.vortikal.repository.Lock;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.RepositoryAction;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.ConstraintViolationException;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import org.vortikal.security.Principal;
import org.vortikal.util.codec.MD5;
import org.vortikal.util.repository.URIUtil;


public class ResourceImpl extends PropertySetImpl implements Resource, Cloneable {
    
    private Acl acl;
    private Lock lock = null;
    private String[] childURIs = null;
    
    private PropertyManager propertyManager;
    private AuthorizationManager authorizationManager;
    
    public ResourceImpl(String uri, PropertyManager propertyManager, 
            AuthorizationManager authorizationManager) {
        super(uri);
        this.propertyManager = propertyManager;
        this.authorizationManager = authorizationManager;
    }

    public ResourceTypeDefinition getResourceTypeDefinition() {
        return this.propertyManager.getResourceTypeDefinitionByName(this.resourceType);
    }
    

    public boolean isOfType(ResourceTypeDefinition type) {
        return this.propertyManager.isContainedType(type, this.resourceType);
    }
    

    public boolean isAuthorized(RepositoryAction action, Principal principal) {
        return this.authorizationManager.authorizeAction(this.uri, action, principal);
    }

    public Property createProperty(Namespace namespace, String name) {
        Property prop = this.propertyManager.createProperty(namespace, name);
        addProperty(prop);
        return prop;
    }

    public void removeProperty(Namespace namespace, String name) {
        Map props = (Map)this.propertyMap.get(namespace);
        
        if (props == null) return;
        
        Property prop = (Property)props.get(name);
        
        if (prop == null) return;

        PropertyTypeDefinition def = prop.getDefinition();
        
        if (def != null && def.isMandatory())
            throw new ConstraintViolationException("Property is mandatory"); 

        props.remove(name);
    }

    public String getParent() {
        return URIUtil.getParentURI(super.uri);
    }

    public String getContentLanguage() {
        return getPropValue(PropertyType.CONTENTLOCALE_PROP_NAME);
    }


    public void setChildURIs(String[] childURIs) {
        this.childURIs = childURIs;
    }

    public String[] getChildURIs() {
        return this.childURIs;
    }

    public Acl getAcl() {
        return this.acl;
    }

    public void setAcl(Acl acl) {
        this.acl = acl;
    }

    public Lock getLock() {
        return this.lock;
    }

    public void setLock(Lock lock) {
        this.lock = lock;
    }

    public String getURI() {
        return super.uri;
    }

    public String getName() {
        if (this.uri.equals("/")) {
            return this.uri;
        } 
        return this.uri.substring(this.uri.lastIndexOf("/") + 1);
    }
    

    public String getSerial() {
        String serial = getURI() + getContentLastModified() + getPropertiesLastModified();
        serial = MD5.md5sum(serial);
        serial = "vortex-" + serial;
        return serial;
    }
    
    public String getEtag() {
        return "\"" + getSerial() + "\"";
    }
    
    public Principal getOwner() {
        return getPrincipalPropValue(PropertyType.OWNER_PROP_NAME);
    }

    public Principal getCreatedBy() {
        return getPrincipalPropValue(PropertyType.CREATEDBY_PROP_NAME);
    }

    public Principal getContentModifiedBy() {
        return getPrincipalPropValue(PropertyType.CONTENTMODIFIEDBY_PROP_NAME);
    }

    public Principal getPropertiesModifiedBy() {
        return getPrincipalPropValue(PropertyType.PROPERTIESMODIFIEDBY_PROP_NAME);
    }

    public Date getCreationTime() {
        return getDatePropValue(PropertyType.CREATIONTIME_PROP_NAME);
    }

    public Date getContentLastModified() {
        return getDatePropValue(PropertyType.CONTENTLASTMODIFIED_PROP_NAME);
    }

    public Date getPropertiesLastModified() {
        return getDatePropValue(PropertyType.PROPERTIESLASTMODIFIED_PROP_NAME);
    }

    public String getDisplayName() {
        return getPropValue(PropertyType.DISPLAYNAME_PROP_NAME);
    }

    public String getContentType() {
        return getPropValue(PropertyType.CONTENTTYPE_PROP_NAME);
    }

    public String getCharacterEncoding() {
        return getPropValue(PropertyType.CHARACTERENCODING_PROP_NAME);
    }

    public String getUserSpecifiedCharacterEncoding() {
        return getPropValue(PropertyType.CHARACTERENCODING_USER_SPECIFIED_PROP_NAME);
    }

    public String getGuessedCharacterEncoding() {
        return getPropValue(PropertyType.CHARACTERENCODING_GUESSED_PROP_NAME);
    }

    public boolean isCollection() {
        return getBooleanPropValue(PropertyType.COLLECTION_PROP_NAME);
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
        return getDatePropValue(PropertyType.LASTMODIFIED_PROP_NAME);
    }

    /**
     * Gets the name of the principal that last modified either the
     * content or the properties of this resource.
     */
    public Principal getModifiedBy() {
        return getPrincipalPropValue(PropertyType.MODIFIEDBY_PROP_NAME);
    }

    public long getContentLength() {
        return getLongPropValue(PropertyType.CONTENTLENGTH_PROP_NAME);
    }

    public void setUserSpecifiedCharacterEncoding(String characterEncoding) {
        setProperty(Namespace.DEFAULT_NAMESPACE, 
                    PropertyType.CHARACTERENCODING_USER_SPECIFIED_PROP_NAME, characterEncoding);

    }
    
    public void setContentLocale(String locale) {
        setProperty(Namespace.DEFAULT_NAMESPACE, 
                PropertyType.CONTENTLOCALE_PROP_NAME, locale);
    }

    public void setContentType(String contentType) {
        setProperty(Namespace.DEFAULT_NAMESPACE, 
                PropertyType.CONTENTTYPE_PROP_NAME, contentType);
    }

    public void setOwner(Principal principal) {
        Property prop = getProperty(Namespace.DEFAULT_NAMESPACE, 
                PropertyType.OWNER_PROP_NAME);
        prop.setPrincipalValue(principal);
    }

    public void setDisplayName(String text) {
        setProperty(Namespace.DEFAULT_NAMESPACE, 
                PropertyType.DISPLAYNAME_PROP_NAME, text);
    }

    public Object clone() throws CloneNotSupportedException {

        AclImpl acl = (AclImpl) this.acl.clone();
        
        LockImpl lock = null;
        if (this.lock != null)
            lock = (LockImpl) this.lock.clone();

        ResourceImpl clone = new ResourceImpl(this.uri, this.propertyManager, this.authorizationManager);
        clone.setID(this.id);
        clone.setAcl(acl);
        clone.setInheritedAcl(this.aclInherited);
        clone.setAclInheritedFrom(this.getAclInheritedFrom());
        clone.setLock(lock);
        clone.setChildURIs(this.childURIs);
        clone.setResourceType(super.resourceType);
        for (Iterator iter = getProperties().iterator(); iter.hasNext();) {
            Property prop = (Property) iter.next();
            clone.addProperty((Property) prop.clone());
        }

        return clone;
    }

    private String getPropValue(String name) {
        Property prop = (Property)((Map)this.propertyMap.get(Namespace.DEFAULT_NAMESPACE)).get(name);
        if (prop == null) return null;
        return prop.getStringValue();
    }

    private Date getDatePropValue(String name) {
        Property prop = (Property)((Map)this.propertyMap.get(Namespace.DEFAULT_NAMESPACE)).get(name);
        if (prop == null) return null;
        return prop.getDateValue();
    }

    private long getLongPropValue(String name) {
        Property prop = (Property)((Map)this.propertyMap.get(Namespace.DEFAULT_NAMESPACE)).get(name);
        if (prop == null) return -1;
        return prop.getLongValue();
    }

    private boolean getBooleanPropValue(String name) {
        Property prop = (Property)((Map)this.propertyMap.get(Namespace.DEFAULT_NAMESPACE)).get(name);
        return prop.getBooleanValue();
    }

    private Principal getPrincipalPropValue(String name) {
        Property prop = (Property)((Map)this.propertyMap.get(Namespace.DEFAULT_NAMESPACE)).get(name);
        return prop.getPrincipalValue();
    }

    private void setProperty(Namespace namespace, String name, String value) {
        if (value == null) { 
            removeProperty(namespace, name);
            return;
        }
        Property prop = getProperty(namespace, name);
        if (prop == null) {
            prop = createProperty(namespace, name);
        }
        prop.setStringValue(value);
    
    }
    
    /**
     * Adds a URI to the child URI list.
     *
     * @param childURI a <code>String</code> value
     */
    public synchronized void addChildURI(String childURI) {
            String[] newChildren = new String[this.childURIs.length + 1];
            for (int i = 0; i < this.childURIs.length; i++) {
                newChildren[i] = this.childURIs[i];
            }

            newChildren[this.childURIs.length] = childURI;
            
            this.childURIs = newChildren;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ResourceImpl)) return false;
        if (!super.equals(obj)) return false;
        ResourceImpl other = (ResourceImpl) obj;
        if (!this.acl.equals(other.acl)) return false;
        if (this.lock == null && other.lock != null) return false;
        if (this.lock != null && other.lock == null) return false;
        if (this.lock != null && !this.lock.equals(other.lock)) return false;
        if (this.childURIs.length != other.childURIs.length) return false;
        for (int i = 0; i < this.childURIs.length; i++) {
            if (!this.childURIs[i].equals(other.childURIs[i])) return false;
        }
        return true;
    }


    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.getClass().getName());
        sb.append(": [").append(this.uri).append("]");
        return sb.toString();
    }

}
