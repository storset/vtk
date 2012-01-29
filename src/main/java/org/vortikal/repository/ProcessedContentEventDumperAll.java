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
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.ChangeLogEntry.Operation;
import org.vortikal.repository.store.ChangeLogDAO;
import org.vortikal.repository.store.DataAccessException;
import org.vortikal.repository.store.DataAccessor;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalFactory;


public class ProcessedContentEventDumperAll extends AbstractRepositoryEventDumper {

    private ChangeLogDAO changeLogDAO;

    @Override
    public void created(Resource resource) {
        ChangeLogEntry entry = changeLogEntry(this.loggerId, this.loggerType, resource.getURI(), 
                Operation.CREATED,
                -1, resource.isCollection(), new Date());
        
        this.changeLogDAO.addChangeLogEntry(entry, true);

    }

    @Override
    public void deleted(Path uri, int resourceId, boolean collection) {
        ChangeLogEntry entry = changeLogEntry(this.loggerId, this.loggerType, uri, 
                Operation.DELETED,
                resourceId, collection, new Date());
        
        this.changeLogDAO.addChangeLogEntry(entry, false);
    }

    @Override
    public void modified(Resource resource, Resource originalResource) {
        ChangeLogEntry entry = changeLogEntry(this.loggerId, this.loggerType, resource.getURI(), 
                Operation.MODIFIED_PROPS,
                -1, resource.isCollection(), new Date());
        
        this.changeLogDAO.addChangeLogEntry(entry, false);
    }


    @Override
    public void contentModified(Resource resource, Resource original) {
        ChangeLogEntry entry = changeLogEntry(this.loggerId, this.loggerType, resource.getURI(),
                Operation.MODIFIED_CONTENT, -1, resource.isCollection(),
                new Date());
        
        this.changeLogDAO.addChangeLogEntry(entry, false);
    }


    @Override
    public void aclModified(Resource resource, Resource originalResource,
                            Acl newACL, Acl originalACL) {
        
        // XXX: ACL inheritance concern moved into Resource class, so a change of the
        //     inheritance property should perhaps be a new log event type (ACL_INHERITANCE_MODIFIED)
        if (newACL.equals(originalACL) && 
                originalResource.isInheritedAcl() == resource.isInheritedAcl()) {
            
            // ACL specific resource data hasn't actually changed
            return;
        }
                
        final ChangeLogEntry entry = changeLogEntry(super.loggerId, super.loggerType, 
                resource.getURI(), 
                ChangeLogEntry.Operation.MODIFIED_ACL, ((ResourceImpl) resource).getID(),
                resource.isCollection(), 
                new Date());

        if (resource.isInheritedAcl()) {
            // Resource ACL inheritance has been turned ON.
            // Apply ACL modification event to:
            // 1. The resource itself.
            // 2. All descendants of the resource which used to inherit their ACL
            //    from it. 
            this.changeLogDAO.addChangeLogEntryInheritedToInheritance(entry);
        } else {
            // Resource ACL inheritance turned OFF or ACL has been modified.
            // Apply ACL modification event to:
            // 1. The resource itself.
            // 2. All descendants of the resource which inherit their ACLa
            //    from it.
            this.changeLogDAO.addChangeLogEntryInherited(entry);
        }
    }

    @Required
    public void setChangeLogDAO(ChangeLogDAO changeLogDAO)  {
        this.changeLogDAO = changeLogDAO;
    }

}
