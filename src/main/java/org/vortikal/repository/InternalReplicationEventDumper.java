/* Copyright (c) 2004, 2007, University of Oslo, Norway
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

import java.util.Date;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.ChangeLogEntry.Operation;
import org.vortikal.repository.store.ChangeLogDAO;
import org.vortikal.repository.store.DataAccessor;


/**
 * 
 *
 */
public class InternalReplicationEventDumper extends AbstractRepositoryEventDumper {

    private ChangeLogDAO changeLogDAO;
    private DataAccessor dataAccessor;

    @Required
    public void setChangeLogDAO(ChangeLogDAO changeLogDAO)  {
        this.changeLogDAO = changeLogDAO;
    }

    @Required
    public void setDataAccessor(DataAccessor dataAccessor)  {
        this.dataAccessor = dataAccessor;
    }

    public void created(Resource resource) {
        
        ChangeLogEntry entry = changeLogEntry(super.loggerId, super.loggerType, resource.getURI(),
                Operation.CREATED, -1, resource.isCollection(),
                new Date());
        
        this.changeLogDAO.addChangeLogEntry(entry, false);

        if (resource.isCollection()) {
            org.vortikal.repository.ResourceImpl[] childResources =
                this.dataAccessor.loadChildren(this.dataAccessor.load(resource.getURI()));
            for (int i = 0; i < childResources.length; i++) {
                entry = changeLogEntry(super.loggerId, super.loggerType, childResources[i].getURI(),
                                                    Operation.CREATED, -1, childResources[i].isCollection(),
                                                    new Date());
                
                this.changeLogDAO.addChangeLogEntry(entry, false);
            }
        }
    }


    public void deleted(Path uri, int resourceId, boolean collection) {
        ChangeLogEntry entry = changeLogEntry(super.loggerId, super.loggerType, uri, 
                Operation.DELETED, resourceId,
                collection, new Date());
        
        this.changeLogDAO.addChangeLogEntry(entry, false);
    }



    public void modified(Resource resource, Resource originalResource) {
        ChangeLogEntry entry = changeLogEntry(super.loggerId, super.loggerType, resource.getURI(), 
                Operation.MODIFIED_PROPS,
                -1, resource.isCollection(), new Date());
        
        this.changeLogDAO.addChangeLogEntry(entry, false);
    }


    public void contentModified(Resource resource) {
        ChangeLogEntry entry = changeLogEntry(super.loggerId, super.loggerType, resource.getURI(), 
                Operation.MODIFIED_CONTENT,
                -1, resource.isCollection(), new Date());
        
        this.changeLogDAO.addChangeLogEntry(entry, false);
    }


    public void aclModified(Resource resource, Resource originalResource,
                            Acl newACL, Acl originalACL) {
        
        ChangeLogEntry entry = changeLogEntry(super.loggerId, super.loggerType, resource.getURI(), 
                Operation.MODIFIED_ACL,
                -1, resource.isCollection(), new Date());
        
        this.changeLogDAO.addChangeLogEntry(entry, false);
    }
}
