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
package org.vortikal.backend;

import java.io.IOException;
import org.springframework.beans.factory.BeanInitializationException;
import org.vortikal.repository.Ace;
import org.vortikal.repository.Resource;
import org.vortikal.repositoryimpl.dao.AbstractDataAccessor;
import org.vortikal.util.repository.AclUtil;



public class InternalReplicationEventDumper extends AbstractRepositoryEventDumper {

    protected AbstractDataAccessor dataAccessor;

    private final static String CREATED = "created";
    private final static String DELETED = "deleted";
    private final static String MODIFIED_PROPS = "modified_props";
    private final static String MODIFIED_CONTENT = "modified_content";
    private final static String MODIFIED_ACL = "modified_acl";



    /**
     * Sets the value of dataAccessor
     *
     * @param dataAccessor Value to assign to this.dataAccessor
     */
    public void setDataAccessor(AbstractDataAccessor dataAccessor)  {
        this.dataAccessor = dataAccessor;
    }


    public void afterPropertiesSet() {

        if (dataAccessor == null) {
            throw new BeanInitializationException("Bean property 'dataAccessor' not set.");
        }
    }
    
    public void created(Resource resource) {
        try {
            dataAccessor.addChangeLogEntry(id, loggerType, resource.getURI(), CREATED, -1, resource.isCollection());
            //if (resource.isCollection()) {
            //    String[] childUris = dataAccessor.listSubTree((Collection) dataAccessor.load(resource.getURI()));
            //for (int i = 0; i < childUris.length; i++) {
            //        dataAccessor.addChangeLogEntry(id, loggerType, childUris[i], CREATED);
            //    }
            //}

            if (resource.isCollection()) {
                org.vortikal.repositoryimpl.Resource[] childResources =
                    dataAccessor.loadChildren(dataAccessor.load(resource.getURI()));
                for (int i = 0; i < childResources.length; i++) {
                    dataAccessor.addChangeLogEntry(id, loggerType, childResources[i].getURI(), CREATED, -1, childResources[i].isCollection());
                }
            }
        } catch (IOException e) {
            logger.warn(
                "Caught IOException while reporting resource creation " +
                "for uri " + resource.getURI(), e);
        }
    }
        



    public void deleted(String uri, int resourceId, boolean collection) {
        
        try {
            dataAccessor.addChangeLogEntry(id, loggerType, uri, DELETED, resourceId, collection);
        } catch (IOException e) {
            logger.warn(
                "Caught IOException while reporting resource deletion " +
                "for uri " + uri, e);
        }
    }



    public void modified(Resource resource, Resource originalResource) {

        try {
            dataAccessor.addChangeLogEntry(id, loggerType, resource.getURI(), MODIFIED_PROPS, -1, resource.isCollection());
        } catch (IOException e) {
            logger.warn(
                "Caught IOException while reporting property modification " +
                "for uri " + resource.getURI(), e);
        }
    }


    public void contentModified(Resource resource) {
        try {
            dataAccessor.addChangeLogEntry(id, loggerType, resource.getURI(),
                                 MODIFIED_CONTENT, -1, resource.isCollection());
        } catch (IOException e) {
            logger.warn(
                "Caught IOException while reporting content modification " +
                "for uri " + resource.getURI(), e);
        }
    }


    public void aclModified(Resource resource, Resource originalResource,
                            Ace[] originalACL, Ace[] newACL) {
        try {

            if (AclUtil.equal(originalACL, newACL) &&
                AclUtil.isInherited(newACL) == AclUtil.isInherited(originalACL)) {
                return;
            }
        
            dataAccessor.addChangeLogEntry(id, loggerType, resource.getURI(), MODIFIED_ACL, -1, resource.isCollection());
            
        } catch (IOException e) {
            logger.warn(
                "Caught IOException while reporting ACL modification " +
                "for uri " + resource.getURI(), e);
        }
    }
}
