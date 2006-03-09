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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.Property;


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
    protected String displayName;
    protected String contentType;
    protected String characterEncoding = null;
// XXX: shouldn't do it like this!:
    protected List properties = new ArrayList();
    protected boolean dirtyACL = false;
    private String[] childURIs = null;
    private String contentLocale = null;
    private boolean collection;
    
    public Resource(String uri, String owner, String contentModifiedBy,
        String propertiesModifiedBy, ACL acl, boolean inheritedACL,
        boolean collection) {

        this.uri = uri;
        this.owner = owner;
        this.contentModifiedBy = contentModifiedBy;
        this.propertiesModifiedBy = propertiesModifiedBy;
        this.acl = acl;
        this.inheritedACL = inheritedACL;
        this.collection = collection;
        this.owner = owner;
    }

    public Object clone() throws CloneNotSupportedException {
        ACL acl = (this.acl == null) ? null : (ACL) this.acl.clone();
        LockImpl lock = (this.lock == null) ? null : (LockImpl) this.lock
                .clone();

        Resource clone = new Resource(uri, owner, contentModifiedBy,
                propertiesModifiedBy, acl, inheritedACL, collection);
        clone.setLock(lock);
        clone.setChildURIs(this.childURIs);
        clone.setContentLocale(this.contentLocale);

        List props = new ArrayList();
        
        for (Iterator iter = this.properties.iterator(); iter.hasNext();) {
            Property prop = (Property) iter.next();
            props.add(prop.clone());
        }
        clone.setProperties(props);

        return clone;
    }



    public void setACL(ACL acl) {
        this.acl = acl;
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
        return this.displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getContentType() {
        return this.contentType;
    }

    public String getName() {
        if (uri.equals("/")) {
            return uri;
        } 
        
        return uri.substring(uri.lastIndexOf("/") + 1);
        
    }

    public String getCharacterEncoding() {
        return this.characterEncoding;
    }

    public void setCharacterEncoding(String characterEncoding) {
        this.characterEncoding = characterEncoding;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public boolean isCollection() {
        return this.collection;
    }

    public List getProperties() {
        return this.properties;
    }

    public void setProperties(List properties) {
        // XXX: shouldn't do it like this:
        if (properties != null)
            this.properties = properties;
    }

    public void setChildURIs(String[] childURIs) {
        this.childURIs = childURIs;
    }

    public String[] getChildURIs() {
        return this.childURIs;
    }

    public String getContentLocale() {
        return this.contentLocale;
    }

    public void setContentLocale(String contentLocale) {
        this.contentLocale = contentLocale;
    }


}
