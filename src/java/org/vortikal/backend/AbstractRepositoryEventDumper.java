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


import org.vortikal.repository.Ace;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.event.ACLModificationEvent;
import org.vortikal.repository.event.ContentModificationEvent;
import org.vortikal.repository.event.RepositoryEvent;
import org.vortikal.repository.event.ResourceCreationEvent;
import org.vortikal.repository.event.ResourceDeletionEvent;
import org.vortikal.repository.event.ResourceModificationEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;



public abstract class AbstractRepositoryEventDumper
  implements ApplicationListener, InitializingBean {

    protected Log logger = LogFactory.getLog(this.getClass());

    protected String id = null;
    protected String loggerType = null;
    protected Repository repository;
    


    public void setRepository(Repository repository)  {
        this.repository = repository;
    }
    

    public void setId(String id) {
        this.id = id;
    }      
    

    public void setLoggerType(String loggerType) {
        this.loggerType = loggerType;
    }      
    

    public void afterPropertiesSet() {
        if (repository == null) {
            throw new BeanInitializationException("Bean property 'repository' not set.");
        }
        if (id == null) {
            throw new BeanInitializationException("Bean property 'id' not set.");
        }
    }
    
    
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
        }
        if (event instanceof ResourceDeletionEvent) {
            deleted(((ResourceDeletionEvent) event).getURI(),
                    ((ResourceDeletionEvent) event).getResourceId(),
                    ((ResourceDeletionEvent) event).isCollection());
        }
        if (event instanceof ResourceModificationEvent) {
            modified(((ResourceModificationEvent) event).getResource(),
                     ((ResourceModificationEvent) event).getOriginal());
        }
        if (event instanceof ContentModificationEvent) {
            contentModified(((ContentModificationEvent) event).getResource());
        }
        if (event instanceof ACLModificationEvent) {
            aclModified(((ACLModificationEvent) event).getResource(),
                        ((ACLModificationEvent) event).getOriginal(),
                        ((ACLModificationEvent) event).getACL(),
                        ((ACLModificationEvent) event).getOriginalACL());
        }
    }



    public abstract void created(Resource resource);

    public abstract void deleted(String uri, int resourceId, boolean collection);

    public abstract void modified(Resource resource, Resource originalResource);

    public abstract void contentModified(Resource resource);

    public abstract void aclModified(Resource resource, Resource originalResource,
                                     Ace[] originalACL, Ace[] newACL);
    
}
