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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.Acl;
import org.vortikal.repository.Lock;
import org.vortikal.repository.Property;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalManager;
import org.vortikal.util.repository.LocaleHelper;
import org.vortikal.util.repository.URIUtil;


public class ResourceImpl implements Resource, Cloneable {
    
    protected Log logger = LogFactory.getLog(this.getClass());

    /* Numeric ID, required by database */
    private int id = -1;

    private String uri;
    private Acl acl;
    private boolean inheritedACL = true;
    private LockImpl lock = null;
    private boolean dirtyACL = false;
    private String[] childURIs = null;
    
    private Map propertyMap = new HashMap();
    
    private PrincipalManager principalManager;
    private PropertyManagerImpl propertyManager;
    
    public ResourceImpl(String uri, PrincipalManager principalManager, 
            PropertyManagerImpl propertyManager) {
        this.uri = uri;
        this.principalManager = principalManager;
        this.propertyManager = propertyManager;
    }

    public Property createProperty(String namespace, String name) {
        return propertyManager.createProperty(namespace, name);
    }

    public void deleteProperty(Property property) {
        List props = (List)propertyMap.get(property.getNamespace());
        
        if (props != null && props.contains(property)) 
            props.remove(property);
    }

    public void removeProperty(String namespace, String name) {
        List props = (List)propertyMap.get(namespace);
        
        if (props == null) return;
        
        Property prop = null;
        for (Iterator iter = props.iterator(); iter.hasNext();) {
            prop = (Property) iter.next();
            if (prop.getName().equals(name)) {
                break;
            }
            prop = null;
        }
        if (prop != null) props.remove(prop);
    }

    public String getParent() {
        return URIUtil.getParentURI(this.uri);
    }

    public Locale getContentLocale() {
        return LocaleHelper.getLocale(getPropValue(PropertyType.CONTENTLOCALE_PROP_NAME));
    }


    public void setChildURIs(String[] childURIs) {
        this.childURIs = childURIs;
    }

    public String[] getChildURIs() {
        return this.childURIs;
    }

    public void setDirtyACL(boolean dirtyACL) {
        this.dirtyACL = dirtyACL;
    }

    public boolean isDirtyACL() {
        return this.dirtyACL;
    }

    public Acl getAcl() {
        return this.acl;
    }

    public void setACL(Acl acl) {
        this.acl = acl;
    }

    public Lock getLock() {
        return this.lock;
    }

    public void setLock(LockImpl lock) {
        this.lock = lock;
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

    public boolean isInheritedACL() {
        return this.inheritedACL;
    }

    public void setInheritedACL(boolean inheritedACL) {
        this.inheritedACL = inheritedACL;
    }

    public String getName() {
        if (uri.equals("/")) {
            return uri;
        } 
        return uri.substring(uri.lastIndexOf("/") + 1);
    }

    public void addProperty(Property property) {
        Map map = (Map)propertyMap.get(property.getNamespace());
        if (map == null) {
            map = new HashMap();
            propertyMap.put(property.getNamespace(), map);
        }
        map.put(property.getName(), property);
    }
    
    public Property getProperty(String namespace, String name) {
        Map map = (Map)propertyMap.get(namespace);

        if (map == null) return null;
        
        return (Property)map.get(name);
    }

    public List getProperties(String namespace) {
        Map map = (Map)propertyMap.get(namespace);
        return new ArrayList(map.entrySet());
    }

    public List getProperties() {
        List props = new ArrayList();
        for (Iterator iter = propertyMap.entrySet().iterator(); iter.hasNext();) {
            Map map = (Map) iter.next();
            props.addAll(map.entrySet());
        }
        return props;
    }

    public List getOtherProperties() {
        List otherProps = new ArrayList();
        
        for (Iterator iter = this.propertyMap.entrySet().iterator(); iter.hasNext();) {
            Map.Entry element = (Map.Entry) iter.next();
            String namespace = (String)element.getKey();
            List props = (List)element.getValue();
            if (namespace.equals(PropertyType.DEFAULT_NAMESPACE_URI)) {
                List specialProps =  Arrays.asList(PropertyType.SPECIAL_PROPERTIES);
                for (Iterator iterator = props.iterator(); iterator
                        .hasNext();) {
                    Property prop = (Property) iterator.next();
                    if (!specialProps.contains(prop.getName())) {
                        otherProps.add(prop);
                    }
                }
            } else {
                otherProps.addAll(props);
            }
        }
        return otherProps;
    }

    public String getSerial() {
        // XXX: Implement me.
        return null;
    }

    

    public Principal getOwner() {
        return principalManager.getPrincipal(getPropValue(PropertyType.OWNER_PROP_NAME));
    }

    public Principal getContentModifiedBy() {
        return principalManager.getPrincipal(getPropValue(PropertyType.CONTENTMODIFIEDBY_PROP_NAME));
    }

    public Principal getPropertiesModifiedBy() {
        return principalManager.getPrincipal(getPropValue(PropertyType.PROPERTIESMODIFIEDBY_PROP_NAME));
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
        if (getContentLastModified().compareTo(getPropertiesLastModified()) > 0) {
            return getContentLastModified();
        }

        return getPropertiesLastModified();
    }

    /**
     * Gets the name of the principal that last modified either the
     * content or the properties of this resource.
     *
     * @return the name of the principal
     */
    public Principal getModifiedBy() {
        if (getContentLastModified().compareTo(getPropertiesLastModified()) > 0) {
            return getContentModifiedBy();
        }

        return getPropertiesModifiedBy();
    }

    public long getContentLength() {
        return getLongPropValue(PropertyType.CONTENTLENGTH_PROP_NAME);
    }

    public void setCharacterEncoding(String characterEncoding) {
        setProperty(PropertyType.DEFAULT_NAMESPACE_URI, 
                PropertyType.CHARACTERENCODING_PROP_NAME, characterEncoding);
    }

    public void setContentLocale(Locale locale) {
        setProperty(PropertyType.DEFAULT_NAMESPACE_URI, 
                PropertyType.CONTENTLOCALE_PROP_NAME, locale.toString());
    }

    public void setContentType(String contentType) {
        setProperty(PropertyType.DEFAULT_NAMESPACE_URI, 
                PropertyType.CONTENTTYPE_PROP_NAME, contentType);
    }

    public void setOwner(Principal principal) {
        setProperty(PropertyType.DEFAULT_NAMESPACE_URI, 
                PropertyType.OWNER_PROP_NAME, principal.getQualifiedName());
    }

    public void setDisplayName(String text) {
        setProperty(PropertyType.DEFAULT_NAMESPACE_URI, 
                PropertyType.DISPLAYNAME_PROP_NAME, text);
    }

    public Object clone() throws CloneNotSupportedException {
        AclImpl acl = (this.acl == null) ? null : (AclImpl) this.acl.clone();
        LockImpl lock = (this.lock == null) ? null : (LockImpl) this.lock
                .clone();

        ResourceImpl clone = new ResourceImpl(uri, principalManager, propertyManager);
        clone.setID(id);
        clone.setACL(acl);
        clone.setInheritedACL(inheritedACL);
        clone.setLock(lock);
        clone.setChildURIs(this.childURIs);
        for (Iterator iter = getProperties().iterator(); iter.hasNext();) {
            Property prop = (Property) iter.next();
            clone.addProperty((Property) prop.clone());
        }

        return clone;
    }

    private String getPropValue(String name) {
        Property prop = (Property)((Map)propertyMap.get(PropertyType.DEFAULT_NAMESPACE_URI)).get(name);
        if (prop == null) return null;
        return prop.getStringValue();
    }

    private Date getDatePropValue(String name) {
        Property prop = (Property)((Map)propertyMap.get(PropertyType.DEFAULT_NAMESPACE_URI)).get(name);
        if (prop == null) return null;
        return prop.getDateValue();
    }

    private long getLongPropValue(String name) {
        Property prop = (Property)((Map)propertyMap.get(PropertyType.DEFAULT_NAMESPACE_URI)).get(name);
        if (prop == null) return -1;
        return prop.getLongValue();
    }

    private boolean getBooleanPropValue(String name) {
        Property prop = (Property)((Map)propertyMap.get(PropertyType.DEFAULT_NAMESPACE_URI)).get(name);
        return prop.getBooleanValue();
    }

    private void setProperty(String namespace, String name, String value) {
        Property prop = getProperty(namespace, name);
        if (prop == null) {
            prop = createProperty(PropertyType.DEFAULT_NAMESPACE_URI,
                    PropertyType.CHARACTERENCODING_PROP_NAME);
        }
        prop.setStringValue(value);
    
    }
    

}
