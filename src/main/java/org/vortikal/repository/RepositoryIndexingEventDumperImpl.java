/* Copyright (c) 2006, 2007, University of Oslo, Norway
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
import org.vortikal.security.PrincipalFactory;

/**
 * Dump repository resource change log events in database changelog suitable
 * for incremental updates of system index.
 * 
 * <ul>
 *  <li>
 *  Copy and move operations result in recursive creation events for all 
 *  resources in the renamed tree.
 *  </li>
 *  <li>
 *  ACL modifications on resources result in recursive modification events
 *  for the entire tree if any of the following modifications are done:
 *  * ACL inheritance is switched on or off
 *  * ACL does not allow read-for-all
 *  * ACL is set to allow read-for-all
 *   
 *   the affected resource is a collection and inheritance 
 *  has been altered.
 *  
 *  Otherwise, a single ACL modification event will be inserted for the affected
 *  resource.
 *  </li>
 * </ul> 
 * 
 * @author oyviste
 */
public class RepositoryIndexingEventDumperImpl extends AbstractRepositoryEventDumper {

    private ChangeLogDAO changeLogDAO;
    
    @Override
    public void created(Resource resource) {

        ChangeLogEntry entry = changeLogEntry(super.loggerId, super.loggerType, 
                                            resource.getURI(), Operation.CREATED,
                                            -1, resource.isCollection(),
                                            new Date());

        this.changeLogDAO.addChangeLogEntry(entry, true);
    }

    @Override
    public void deleted(Path uri, int resourceId, boolean collection) {
        
        ChangeLogEntry entry = changeLogEntry(super.loggerId, super.loggerType, uri, 
                                            Operation.DELETED, resourceId, collection, 
                                            new Date());
        
        this.changeLogDAO.addChangeLogEntry(entry, false);

    }

    @Override
    public void modified(Resource resource, Resource originalResource) {
        
        ChangeLogEntry entry = changeLogEntry(super.loggerId, super.loggerType, 
                resource.getURI(), Operation.MODIFIED_PROPS,
                -1, resource.isCollection(),
                new Date());

        this.changeLogDAO.addChangeLogEntry(entry, false);

    }

    @Override
    public void contentModified(Resource resource, Resource original) {

        ChangeLogEntry entry = changeLogEntry(super.loggerId, super.loggerType, 
                resource.getURI(),
                ChangeLogEntry.Operation.MODIFIED_CONTENT, -1, 
                resource.isCollection(),
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
        
        ChangeLogEntry entry = changeLogEntry(super.loggerId, super.loggerType, 
                resource.getURI(), 
                ChangeLogEntry.Operation.MODIFIED_ACL, -1,
                resource.isCollection(), 
                new Date());

        boolean recurse = false;
        
        if (resource.isCollection()) {
            // Determine if we need to dump changelog entries recursively and
            // update the whole tree in the system index.
            if (originalResource.isInheritedAcl() != resource.isInheritedAcl()) {
                recurse = true;
            } else {
                Privilege[] privsForAllOrig = originalACL.getPrivilegeSet(PrincipalFactory.ALL);
                Privilege[] privsForAllNew = newACL.getPrivilegeSet(PrincipalFactory.ALL);
                
                boolean origAllowsReadForAll = false;
                boolean newAllowsReadForAll = false;
                
                for (Privilege action: privsForAllOrig) {
                    if (action == Privilege.READ 
                            || action == Privilege.READ_PROCESSED
                            || action == Privilege.ALL) {
                        origAllowsReadForAll = true;
                        break;
                    }
                }
                for (Privilege action: privsForAllNew) {
                    if (action == Privilege.READ 
                            || action == Privilege.READ_PROCESSED
                            || action == Privilege.ALL) {
                        newAllowsReadForAll = true;
                        break;
                    }
                }
                
                if (! (origAllowsReadForAll && newAllowsReadForAll)) {
                    recurse = true;
                }
            }
        }
        
        this.changeLogDAO.addChangeLogEntry(entry, recurse);
    }
    
    @Required
    public void setChangeLogDAO(ChangeLogDAO changeLogDAO)  {
        this.changeLogDAO = changeLogDAO;
    }

}
