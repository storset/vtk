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
package org.vortikal.repositoryimpl.event;

import org.vortikal.repositoryimpl.ACL;
import org.vortikal.repositoryimpl.Resource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class EventDispatcher {
    private List listeners = new ArrayList();

    public void reportResourceCreation(Resource resource) {
        for (Iterator i = listeners.iterator(); i.hasNext();) {
            EventListener listener = (EventListener) i.next();

            listener.created(resource);
        }
    }

    public void reportResourceDeletion(String uri) {
        for (Iterator i = listeners.iterator(); i.hasNext();) {
            EventListener listener = (EventListener) i.next();

            listener.deleted(uri);
        }
    }

    public void reportResourceModification(Resource resource,
        Resource originalResource) {
        for (Iterator i = listeners.iterator(); i.hasNext();) {
            EventListener listener = (EventListener) i.next();

            listener.modified(resource, originalResource);
        }
    }

    public void reportResourceContentModification(Resource resource,
        Resource originalResource) {
        for (Iterator i = listeners.iterator(); i.hasNext();) {
            EventListener listener = (EventListener) i.next();

            listener.contentModified(resource);
        }
    }

    public void reportAclModification(Resource resource,
        Resource originalResource, ACL newACL, ACL originalACL,
        boolean wasInherited) {
        for (Iterator i = listeners.iterator(); i.hasNext();) {
            EventListener listener = (EventListener) i.next();

            listener.aclModified(resource, originalResource, originalACL,
                newACL, wasInherited);
        }
    }

    /**
     * @param listeners The listeners to set.
     */
    public void setListeners(List listeners) {
        this.listeners = listeners;
    }
}
