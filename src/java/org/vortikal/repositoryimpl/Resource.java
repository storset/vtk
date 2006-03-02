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
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.IllegalOperationException;
import org.vortikal.repositoryimpl.dao.DataAccessor;
import org.vortikal.security.PrincipalManager;
import org.vortikal.util.web.URLUtil;


public class Resource implements Cloneable {

    protected Log logger = LogFactory.getLog(this.getClass());

    /* Numeric ID, required by database */
    private int id = -1;

    protected String uri = null;
    protected String owner = null;
    protected String contentModifiedBy = null;
    protected String propertiesModifiedBy = null;
    protected ACL acl = null;
    protected boolean inheritedACL = true;
    protected LockImpl lock = null;
    protected Date creationTime = null;
    protected Date contentLastModified = null;
    protected Date propertiesLastModified = null;
    protected String name;
    protected String displayName = "";
    protected String contentType = "";
    protected String characterEncoding = null;
    protected Vector properties = new Vector();
    protected boolean dirtyACL = false;
    private String[] childURIs = null;
    private Locale contentLocale = null;
    private boolean isCollection;
    
    public Resource(String uri, String owner, String contentModifiedBy,
        String propertiesModifiedBy, ACL acl, boolean inheritedACL, 
        LockImpl lock, boolean isCollection, String[] childURIs) {

        this.uri = uri;
        this.owner = owner;
        this.contentModifiedBy = contentModifiedBy;
        this.propertiesModifiedBy = propertiesModifiedBy;
        this.acl = acl;
        this.inheritedACL = inheritedACL;
        this.lock = lock;
        this.childURIs = childURIs;
        this.isCollection = isCollection;
        
        if (isCollection) {
            this.contentType = "application/x-vortex-collection";
        }
        
        if (this.uri.equals("/")) {
            this.name = uri;
        } else {
            this.name = uri.substring(uri.lastIndexOf("/") + 1, uri.length());
        }

        this.owner = owner;
        //acl.setResource(this);
    }

    public Object clone() throws CloneNotSupportedException {
        ACL acl = (this.acl == null) ? null : (ACL) this.acl.clone();
        LockImpl lock = (this.lock == null) ? null : (LockImpl) this.lock
                .clone();

        Resource clone = new Resource(uri, owner, contentModifiedBy,
                propertiesModifiedBy, acl, inheritedACL, lock, isCollection,
                childURIs);
        clone.setContentLocale(this.contentLocale);
        return clone;
    }



    public void setACL(ACL acl) {
        this.acl = acl;
        //acl.setResource(this);
    }

    public ACL getACL() {
        return this.acl;
    }

    public LockImpl getLock() {
        return this.lock;
    }

    public void setLock(LockImpl lock) {
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

    public void setDirtyACL(boolean dirtyACL) {
        this.dirtyACL = dirtyACL;
    }

    public boolean isDirtyACL() {
        return this.dirtyACL;
    }

    public Date getCreationTime() {
        return this.creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public Date getContentLastModified() {
        return this.contentLastModified;
    }

    public void setContentLastModified(Date contentLastModified) {
        this.contentLastModified = contentLastModified;
    }

    public Date getPropertiesLastModified() {
        return this.propertiesLastModified;
    }

    public void setPropertiesLastModified(Date propertiesLastModified) {
        this.propertiesLastModified = propertiesLastModified;
    }

    public String getDisplayName() {
        return ((this.displayName == null) || this.displayName.equals("")) ? this.name
                                                                 : this.displayName;
    }

    public void setDisplayName(String displayName) {
        if (!((displayName == null) || displayName.equals(""))) {
            this.displayName = displayName;
        }
    }

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

    public void setContentType(String contentType) {
        if (isCollection()) {
            this.contentType = "application/x-vortex-collection";
        } else {
            this.contentType = contentType;
        }
    }

    public boolean isCollection() {
        return "application/x-vortex-collection".equals(getContentType());
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

    public void setChildURIs(String[] childURIs) {
        this.childURIs = childURIs;
    }

    public String[] getChildURIs() {
        return this.childURIs;
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

    /**
     * @return Returns the contentLocale.
     */
    public Locale getContentLocale() {
        return contentLocale;
    }

    /**
     * @param contentLocale The contentLocale to set.
     */
    public void setContentLocale(Locale contentLocale) {
        this.contentLocale = contentLocale;
    }


}
