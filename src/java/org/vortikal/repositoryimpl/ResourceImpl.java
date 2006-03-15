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

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.Acl;
import org.vortikal.repository.AclRestrictions;
import org.vortikal.repository.Lock;
import org.vortikal.repository.Privilege;
import org.vortikal.repository.PrivilegeDefinition;
import org.vortikal.repository.Property;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalManager;
import org.vortikal.security.PrincipalStore;
import org.vortikal.util.repository.LocaleHelper;


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
    
    public ResourceImpl(String uri, PrincipalManager principalManager) {
        this.uri = uri;
        this.principalManager = principalManager;
    }

    public Object clone() throws CloneNotSupportedException {
        AclImpl acl = (this.acl == null) ? null : (AclImpl) this.acl.clone();
        LockImpl lock = (this.lock == null) ? null : (LockImpl) this.lock
                .clone();

        ResourceImpl clone = new ResourceImpl(uri, principalManager);
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
        Property prop = (Property)((Map)propertyMap.get(null)).get(name);
        if (prop == null) return null;
        return prop.getStringValue();
    }

    private Date getDatePropValue(String name) {
        Property prop = (Property)((Map)propertyMap.get(null)).get(name);
        if (prop == null) return null;
        return prop.getDateValue();
    }

    private boolean getBooleanPropValue(String name) {
        Property prop = (Property)((Map)propertyMap.get(null)).get(name);
        return prop.getBooleanValue();
    }

    public Principal getOwner() {
        return principalManager.getPrincipal(getPropValue("owner"));
    }

    public Principal getContentModifiedBy() {
        return principalManager.getPrincipal(getPropValue("contentModifiedBy"));
    }

    public Principal getPropertiesModifiedBy() {
        return principalManager.getPrincipal(getPropValue("propertiesModifiedBy"));
    }

    public Date getCreationTime() {
        return getDatePropValue("creationTime");
    }

    public Date getContentLastModified() {
        return getDatePropValue("contentLastModified");
    }

    public Date getPropertiesLastModified() {
        return getDatePropValue("propertiesLastModified");
    }

    public String getDisplayName() {
        return getPropValue("displayName");
    }

    public String getContentType() {
        return getPropValue("contentType");
    }

    public String getCharacterEncoding() {
        return getPropValue("characterEncoding");
    }

    public boolean isCollection() {
        return getBooleanPropValue("collection");
    }

    public Locale getContentLocale() {
        return LocaleHelper.getLocale(getPropValue("contentLocale"));
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



    


    public void setACL(Acl acl) {
        this.acl = acl;
    }

    public Acl getACL() {
        return this.acl;
    }

    public LockImpl getLock() {
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
        // TODO Auto-generated method stub
        return null;
    }

    public List getProperties(String namespace) {
        // TODO Auto-generated method stub
        return null;
    }

    public List getProperties() {
        // TODO Auto-generated method stub
        return null;
    }

    public Property createProperty(String namespace, String name) {
        // TODO Auto-generated method stub
        return null;
    }

    public void deleteProperty(Property property) {
        // TODO Auto-generated method stub
        
    }

    public void removeProperty(String namespace, String name) {
        // TODO Auto-generated method stub
        
    }

    public List getOtherProperties() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getSerial() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getParent() {
        // TODO Auto-generated method stub
        return null;
    }

    public Date getLastModified() {
        // TODO Auto-generated method stub
        return null;
    }

    public Principal getModifiedBy() {
        // TODO Auto-generated method stub
        return null;
    }

    public long getContentLength() {
        // TODO Auto-generated method stub
        return 0;
    }

    public Lock getActiveLock() {
        // TODO Auto-generated method stub
        return null;
    }

    public PrivilegeDefinition getSupportedPrivileges() {
        // TODO Auto-generated method stub
        return null;
    }

    public AclRestrictions getAclRestrictions() {
        // TODO Auto-generated method stub
        return null;
    }

    public Privilege[] getPrivilegeSet(Principal principal, PrincipalStore principalStore) {
        // TODO Auto-generated method stub
        return null;
    }

    public Privilege[] getParentPrivilegeSet(Principal principal, PrincipalStore principalStore) {
        // TODO Auto-generated method stub
        return null;
    }

    public void setCharacterEncoding(String characterEncoding) {
        // TODO Auto-generated method stub
        
    }

    public Acl getAcl() {
        return this.acl;
    }

    public void setContentLocale(Locale locale) {
        // TODO Auto-generated method stub
        
    }

    public void setContentType(String string) {
        // TODO Auto-generated method stub
        
    }

    public void setOwner(Principal principal) {
        // TODO Auto-generated method stub
        
    }

    public void setDisplayName(String text) {
        // TODO Auto-generated method stub
        
    }

    
}
