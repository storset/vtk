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
package org.vortikal.repository;

import java.io.IOException;

import org.vortikal.repository.resourcetype.Content;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.Principal;

public interface RepositoryResourceHelper {

    public ResourceImpl create(Principal principal, ResourceImpl resource, boolean collection, Content content) throws IOException;

    /**
     * Evaluates and validates properties on a resource before storing.
     * 
     * @param resource the original resource
     * @param principal the principal performing the store operation
     * @param dto the user-supplied resource
     * @param content the resource's content
     * @return the resulting resource after property evaluation
     */
    public ResourceImpl propertiesChange(ResourceImpl resource, Principal principal, ResourceImpl dto, Content content)
            throws AuthenticationException, AuthorizationException, CloneNotSupportedException, IOException;

    public ResourceImpl contentModification(ResourceImpl resource, Principal principal, Content content) throws IOException;

    public ResourceImpl nameChange(ResourceImpl originalResource, ResourceImpl resource, Principal principal, Content content)
            throws IOException;

    public ResourceImpl commentsChange(ResourceImpl originalResource, Principal principal, ResourceImpl suppliedResource, Content content)
            throws IOException;

    public PropertySet getFixedCopyProperties(ResourceImpl resource, Principal principal, Path destUri)
            throws CloneNotSupportedException;

    public ResourceImpl systemChange(ResourceImpl resource, Principal principal, ResourceImpl dto, Content content)
            throws AuthenticationException, AuthorizationException, CloneNotSupportedException, IOException;

}
