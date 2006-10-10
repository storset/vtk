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
package org.vortikal.repositoryimpl.dao;

import java.util.Date;

import org.vortikal.repository.AbstractRepositoryTestCase;
import org.vortikal.repository.Acl;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repositoryimpl.PropertyManagerImpl;
import org.vortikal.repositoryimpl.PropertySetImpl;
import org.vortikal.repositoryimpl.ResourceImpl;
import org.vortikal.security.Principal;


public class DataAccessorTestCase extends AbstractRepositoryTestCase {


    protected void setUp() throws Exception {
        System.setProperty("org.apache.commons.logging.Log",
                           "org.apache.commons.logging.impl.Log4JLogger");
        System.setProperty("log4j.configuration", "log4j.test.xml");
        super.setUp();
    }


    private PropertyManagerImpl getPropertyManager() {
        return (PropertyManagerImpl) getApplicationContext().getBean("propertyManager");
    }
    

    private DataAccessor getDataAccessor() {
        return (DataAccessor) getApplicationContext().getBean("repository.database");
    }
    

    public void testChangeAclInheritance() throws Exception {
        PropertyManagerImpl propertyManager = getPropertyManager();
        DataAccessor dao = getDataAccessor();
        Principal rootPrincipal = getPrincipalFactory().getUserPrincipal("root@localhost");

        ResourceImpl root = dao.load("/");

        // Create /parent with inherited ACL:
        String parentURI = "/parent";
        ResourceImpl parent = propertyManager.create(rootPrincipal, parentURI, true);
        dao.store(parent);
        parent = dao.load(parentURI);
        assertEquals(root.getID(), parent.getAclInheritedFrom());
        
        // Create /parent/child with inherited ACL:
        String childURI = parentURI + "/child";
        ResourceImpl child = propertyManager.create(rootPrincipal, childURI, false);
        dao.store(child);
        child = dao.load(childURI);
        assertTrue(child.isInheritedAcl());
        assertEquals(root.getID(), child.getAclInheritedFrom());

        // Storing the child's inherited ACL should have no effect:
        child.setAcl((Acl) root.getAcl().clone());
        child.setAclInheritedFrom(root.getID());
        child.setInheritedAcl(true);
        dao.storeACL(child);
        child = dao.load(childURI);
        assertTrue(child.isInheritedAcl());
        assertEquals(root.getID(), child.getAclInheritedFrom());

        // Set parent's ACL inherited = false
        parent.setInheritedAcl(false);
        parent.setAclInheritedFrom(PropertySetImpl.NULL_RESOURCE_ID);
        dao.storeACL(parent);
        parent = dao.load(parentURI);
        child = dao.load(childURI);
        assertFalse(parent.isInheritedAcl());
        assertEquals(parent.getID(), child.getAclInheritedFrom());

        // Create /parent/second-child with inherited ACL:
        String secondChildURI = parentURI + "/second-child";
        ResourceImpl secondChild = propertyManager.create(rootPrincipal, secondChildURI, false);
        dao.store(secondChild);        
        secondChild = dao.load(secondChildURI);
        assertEquals(parent.getID(), secondChild.getAclInheritedFrom());

        // Storing the second child's inherited ACL should have no effect either:
        secondChild.setAcl((Acl) parent.getAcl().clone());
        secondChild.setAclInheritedFrom(parent.getID());
        secondChild.setInheritedAcl(true);
        dao.storeACL(secondChild);
        secondChild = dao.load(secondChildURI);
        assertEquals(parent.getID(), secondChild.getAclInheritedFrom());
    }
    
    

    public void testSetLiveProperty() throws Exception {
        PropertyManagerImpl propertyManager = getPropertyManager();
        DataAccessor dao = getDataAccessor();
        Principal rootPrincipal = getPrincipalFactory().getUserPrincipal("root@localhost");

        ResourceImpl root = dao.load("/");

        // Create /property-collection:
        String collectionURI = "/property-collection";
        ResourceImpl collection = propertyManager.create(rootPrincipal, collectionURI, true);
        Date lastModifiedBefore = collection.getLastModified();
        dao.store(collection);
        collection = dao.load(collectionURI);
        assertEquals(lastModifiedBefore, collection.getLastModified());
        
        // Set the propertiesLastModified property to a future date:
        Date futureDate = new Date(System.currentTimeMillis() + 10000000);
        Property propertiesLastModified = collection.getProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.PROPERTIESLASTMODIFIED_PROP_NAME);
        propertiesLastModified.setDateValue(futureDate);
        dao.store(collection);
        collection = dao.load(collectionURI);
        assertEquals(futureDate, collection.getPropertiesLastModified());
    }
    
}

