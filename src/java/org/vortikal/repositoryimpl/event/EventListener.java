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
import org.vortikal.repositoryimpl.RepositoryImpl;
import org.vortikal.repositoryimpl.Resource;


public interface EventListener {
    /**
     * Sets this listener's ID
     *
     */
    public void setID(String id);

    /**
     * Sets this listener's CMS object
     * FIXME: This is a special case, not every listener needs (or
     * even has a concept of) a database object. This should be set at
     * the implementation level.
     *
     */
    public void setRepository(RepositoryImpl repository);

    /**
     * Reports a resource creation event.
     */
    public void created(Resource resource);

    /**
     * Reports a resource deletion event. The URI may be that of a
     * collection resource, and in that case none of the children's
     * deletion will be reported.
     */
    public void deleted(String uri);

    /**
     * Reports a resource property modification event.
     */
    public void modified(Resource resource, Resource originalResource);

    /**
     * Reports a resource content modification event.
     */
    public void contentModified(Resource resource);

    /**
     * Reports an ACL modification event.
     */
    public void aclModified(Resource resource, Resource originalResource,
        ACL newACL, ACL originalACL, boolean wasInherited);
}
