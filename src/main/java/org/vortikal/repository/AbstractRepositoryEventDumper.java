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
package org.vortikal.repository;

import java.util.Date;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.vortikal.repository.ChangeLogEntry.Operation;
import org.vortikal.repository.event.ACLModificationEvent;
import org.vortikal.repository.event.ContentModificationEvent;
import org.vortikal.repository.event.InheritablePropertiesModificationEvent;
import org.vortikal.repository.event.RepositoryEvent;
import org.vortikal.repository.event.ResourceCreationEvent;
import org.vortikal.repository.event.ResourceDeletionEvent;
import org.vortikal.repository.event.ResourceModificationEvent;



public abstract class AbstractRepositoryEventDumper implements ApplicationListener {

    protected int loggerId = -1;
    protected int loggerType = -1;
    protected Repository repository;

    @Required
    public void setLoggerId(int loggerId) {
        this.loggerId = loggerId;
    }      

    @Required
    public void setLoggerType(int loggerType) {
        this.loggerType = loggerType;
    }      
    
    @Required
    public void setRepository(Repository repository)  {
        this.repository = repository;
    }
    
    @Override
    public void onApplicationEvent(ApplicationEvent event) {

        if (! (event instanceof RepositoryEvent)) {
            return;
        }

        Repository rep = ((RepositoryEvent) event).getRepository();

        if (! rep.getId().equals(this.repository.getId())) {
            return;
        }

        if (event instanceof ResourceCreationEvent) {
            created(((ResourceCreationEvent) event).getResource());
        } else if (event instanceof ResourceDeletionEvent) {
            deleted(((ResourceDeletionEvent) event).getURI(),
                    ((ResourceDeletionEvent) event).getResourceId(),
                    ((ResourceDeletionEvent) event).isCollection());
        } else if (event instanceof ResourceModificationEvent) {
            final Resource resource = ((ResourceModificationEvent)event).getResource();
            final Resource original = ((ResourceModificationEvent)event).getOriginal();
            if (event instanceof InheritablePropertiesModificationEvent) {
                modifiedInheritableProperties(resource, original);
            } else {
                modified(resource, original);
            }
        } else if (event instanceof ContentModificationEvent) {
            contentModified(((ContentModificationEvent) event).getResource(),
                            ((ContentModificationEvent) event).getOriginal());
        } else if (event instanceof ACLModificationEvent) {
            aclModified(((ACLModificationEvent)event).getResource(),
                        ((ACLModificationEvent)event).getOriginalResource(),
                        ((ACLModificationEvent)event).getACL(),
                        ((ACLModificationEvent)event).getOriginalACL());
        }
        
    }

    public abstract void created(Resource resource);

    public abstract void deleted(Path uri, int resourceId, boolean collection);

    public abstract void modified(Resource resource, Resource originalResource);
    
    public abstract void modifiedInheritableProperties(Resource resource, Resource originalResource);

    public abstract void contentModified(Resource resource, Resource original);

    public abstract void aclModified(Resource resource, Resource originalResource,
                                     Acl acl, Acl originalAcl);

    protected ChangeLogEntry changeLogEntry(int loggerId, int loggerType,
            Path uri, Operation operation, int resourceId,
            boolean collection, Date timestamp) {

        ChangeLogEntry entry = new ChangeLogEntry();
        entry.setLoggerId(loggerId);
        entry.setLoggerType(loggerType);
        entry.setUri(uri);
        entry.setOperation(operation);
        entry.setResourceId(resourceId);
        entry.setCollection(collection);
        entry.setTimestamp(timestamp);

        return entry;
    }
    
}
