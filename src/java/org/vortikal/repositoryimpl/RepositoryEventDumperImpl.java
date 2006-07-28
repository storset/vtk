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
package org.vortikal.repositoryimpl;

import java.io.IOException;
import java.util.Date;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.Acl;
import org.vortikal.repository.Resource;
import org.vortikal.repositoryimpl.dao.DataAccessor;

/**
 * Dump repository resource change log events in database changelog.
 * 
 * <ul>
 *  <li>
 *  Copy and move operations result in recursive creation events for all 
 *  resources in the renamed tree.
 *  </li>
 *  <li>
 *  ACL modifications on resources result in recursive modification events
 *  for the entire tree if the affected resource is a collection and inheritance 
 *  has been altered.
 *  
 *  Otherwise, a single ACL modification event will be inserted for the affected
 *  resource.
 *  </li>
 * </ul> 
 * 
 * @author oyviste
 */
public class RepositoryEventDumperImpl extends AbstractRepositoryEventDumper 
    implements InitializingBean {

    public static final String CREATED          = "created";
    public static final String DELETED          = "deleted";
    public static final String MODIFIED_PROPS   = "modified_props";
    public static final String MODIFIED_CONTENT = "modified_content";
    public static final String ACL_MODIFIED     = "modified_acl";
    
    private DataAccessor dataAccessor;
    
    public void afterPropertiesSet() {
        super.afterPropertiesSet();

        if (this.dataAccessor == null) {
            throw new BeanInitializationException("Bean property 'dataAccessor' not set.");
        }
    }
    
    public void created(Resource resource) {
        try {

            this.dataAccessor.addChangeLogEntry(super.loggerId, super.loggerType, 
                                                resource.getURI(), CREATED,
                                                -1, resource.isCollection(),
                                                new Date(), true);
        } catch (IOException e) {
            this.logger.warn(
                "Caught IOException while reporting resource creation " +
                "for uri " + resource.getURI(), e);
        }

    }

    public void deleted(String uri, int resourceId, boolean collection) {
        
        try {
            this.dataAccessor.addChangeLogEntry(super.loggerId, super.loggerType, uri, 
                                                DELETED, resourceId, collection, 
                                                new Date(), false);
        } catch (IOException e) {
            this.logger.warn(
                "Caught IOException while reporting resource deletion " +
                "for uri " + uri, e);
        }

    }

    public void modified(Resource resource, Resource originalResource) {

        try {
            this.dataAccessor.addChangeLogEntry(super.loggerId, super.loggerType, 
                                                resource.getURI(), MODIFIED_PROPS,
                                                -1, resource.isCollection(),
                                                new Date(), false);
        } catch (IOException e) {
            this.logger.warn(
                "Caught IOException while reporting property modification " +
                "for uri " + resource.getURI(), e);
        }

    }

    public void contentModified(Resource resource) {

        try {
            this.dataAccessor.addChangeLogEntry(super.loggerId, super.loggerType, 
                                                resource.getURI(),
                                                MODIFIED_CONTENT, -1, 
                                                resource.isCollection(),
                                                new Date(), false);
        } catch (IOException e) {
            this.logger.warn(
                "Caught IOException while reporting content modification " +
                "for uri " + resource.getURI(), e);
        }

    }

    public void aclModified(Resource resource, Resource originalResource, 
                            Acl originalACL, Acl newACL) {
        
        
        if (newACL.equals(originalACL)) {
            // ACL hasn't actually changed, so we don't bother publishing an event.
            return;
        }
        
        try {
            if (resource.isCollection() && 
                    (originalResource.isInheritedAcl() != resource.isInheritedAcl())) {
                
                // ACL inheritance altered on collection resource, insert 
                // recursive ACL modification event
                this.dataAccessor.addChangeLogEntry(super.loggerId, super.loggerType, 
                                                    resource.getURI(), 
                                                    ACL_MODIFIED, -1,
                                                    resource.isCollection(), 
                                                    new Date(), true);
                
                
            } else {
                // Not a collection, insert single ACL modification event
                this.dataAccessor.addChangeLogEntry(super.loggerId, super.loggerType, 
                                                    resource.getURI(),
                                                    ACL_MODIFIED, -1,
                                                    resource.isCollection(),
                                                    new Date(), false);
            }
        } catch (IOException e) {
            this.logger.warn(
                    "Caught IOException while reporting ACL modification " +
                    "for uri " + resource.getURI(), e);
        }

    }

    public void setDataAccessor(DataAccessor dataAccessor) {
        this.dataAccessor = dataAccessor;
    }

}
