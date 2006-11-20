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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.vortikal.security.Principal;
import org.vortikal.security.AuthenticationException;

/**
 * Manager for authorizing principals at specific authorization level.
 */
public interface AuthorizationManager {


    public boolean isReadOnly();
    

    public void setReadOnly(boolean readOnly);


    /**
     * Authorizes a principal for a root role action. Should throw an
     * AuthorizationException if the principal in question does not
     * have root privileges.
     */
    public void authorizeRootRoleAction(Principal principal) throws AuthorizationException;
    

    /**
     * Authorizes a principal for a given action on a resource
     * URI. Equivalent to calling one of the <code>authorizeYYY(uri,
     * principal)</code> methods of this interface (with
     * <code>YYY</code> mapping to one of the actions).
     *
     * @param uri a resource URI
     * @param action the action to perform. One of the action types
     * defined in {@link #ACTION_AUTHORIZATIONS}.
     * @param principal the principal performing the action
     */
    public void authorizeAction(String uri, RepositoryAction action, Principal principal)
    throws AuthorizationException, AuthenticationException, ResourceLockedException, IOException;
    
    /**
     * <ul>
     *   <li>Privilege READ_PROCESSED, READ or ALL in ACL
     *   <li>Role ROOT or READ_EVERYTHING
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizeReadProcessed(String uri, Principal principal) 
        throws AuthenticationException, AuthorizationException,
        ResourceLockedException, IOException;
    
    /**
     * <ul>
     *   <li>Privilege READ or ALL in ACL
     *   <li>Role ROOT or READ_EVERYTHING
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizeRead(String uri, Principal principal) 
        throws AuthenticationException, AuthorizationException,
        ResourceLockedException, IOException;


    /**
     * <ul>
     *   <li>Privilege BIND, WRITE or ALL on parent resource
     *   <li>Role ROOT
     *   <li>+ parent not locked by another principal
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizeCreate(String uri, Principal principal)
        throws AuthenticationException, AuthorizationException, ReadOnlyException, 
        ResourceLockedException, IOException;
    
    /**
     * <ul>
     *   <li>Privilege WRITE or ALL in ACL
     *   <li>Role ROOT
     *   <li>+ resource not locked by another principal
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizeWrite(String uri, Principal principal)
        throws AuthenticationException, AuthorizationException, ReadOnlyException,
        ResourceLockedException, IOException;
    
    /**
     * <ul>
     *   <li>Privilege ALL in ACL
     *   <li>Role ROOT
     *   <li>+ resource not locked by another principal
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizeAll(String uri, Principal principal)
        throws AuthenticationException, AuthorizationException, ReadOnlyException,
        ResourceLockedException, IOException;
    
    /**
     * <ul>
     *   <li>privilege WRITE or ALL in Acl + resource not locked by another principal
     *   <li>Role ROOT
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizeUnlock(String uri, Principal principal) 
        throws AuthenticationException, AuthorizationException, ReadOnlyException,
        ResourceLockedException, IOException;


    /**
     * <ul>
     *   <li>Privilege ALL in ACL + parent not locked
     *   <li>Action WRITE on parent
     *   <li>+ resource tree not locked by another principal
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizeDelete(String uri, Principal principal) 
        throws AuthenticationException, AuthorizationException, ReadOnlyException, 
        ResourceLockedException, IOException;


    /**
     * All of:
     * <ul>
     *   <li>Action WRITE
     *   <li>Action ALL or role ROOT
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizePropertyEditAdminRole(String uri, Principal principal) 
        throws AuthenticationException, AuthorizationException,
        ResourceLockedException, IOException;


    /**
     * All of:
     * <ul>
     *   <li>Action WRITE
     *   <li>Role ROOT
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizePropertyEditRootRole(String uri, Principal principal)
        throws AuthenticationException, AuthorizationException,
        ResourceLockedException, IOException;

    
    /**
     * All of:
     * <ul>
     *   <li>Action READ on source tree
     *   <li>Action CREATE on destination
     *   <li>if overwrite, action DELETE on dest
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizeCopy(String srcUri, String destUri, 
            Principal principal, boolean deleteDestination) 
        throws AuthenticationException, AuthorizationException, ReadOnlyException,
        ResourceLockedException, IOException;
    

    /**
     * All of:
     * <ul>
     *   <li>COPY action
     *   <li>Action DELETE on source
     * </ul>
     * @return is authorized
     * @throws IOException
     */
    public void authorizeMove(String srcUri, String destUri,
            Principal principal, boolean deleteDestination) 
        throws AuthenticationException, AuthorizationException, ReadOnlyException,
        ResourceLockedException, IOException;

}
