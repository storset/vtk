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
package org.vortikal.web.service;


import org.vortikal.repository.Privilege;
import org.vortikal.repository.Resource;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalStore;
import org.vortikal.security.SecurityContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;


/**
 * Assert that the current user has the permission 'permission' on the current resource
 * 
 * Properties:
 * 
 * <ul>
 *   <li>permission - what permission to check, one of
 *     <ul>
 *       <li>read</li>
 *       <li>read-processed (includes read)</li>
 *       <li>write</li>
 *       <li>write-acl</li>
 *       <li>parent-write - write permission on the resource's parent</li>
 *       <li>unlock - will be true only if a resource is locked and
 *           lock owner is the current principal</li>
 *       <li>lock - true if the resource is not locked and the current
 *           principal has write permission</li>
 *     </ul>
 *   </li>
 *   <li>requiresAuthentication - whether authentication is explicitly
 *       required. An AuthenticationException will be thrown on matching
 *       if there is no principal.
 *   </li>
 *   <li>principalStore (required)
 *   </li>
 * </ul>
 */
public class ResourcePrincipalPermissionAssertion
  implements ResourceAssertion, InitializingBean {

    private static Log logger = LogFactory.getLog(
            ResourcePrincipalPermissionAssertion.class);
    
    private String permission;
    private boolean requiresAuthentication = false;
    private PrincipalStore principalStore;
    
    
    /**
     * @param requiresAuthentication The requiresAuthentication to set.
     */
    public void setRequiresAuthentication(boolean requiresAuthentication) {
        this.requiresAuthentication = requiresAuthentication;
    }
    

    /**
     * @param permission The permission to set.
     */
    public void setPermission(String permission) {
        this.permission = permission;
    }


    public void setPrincipalStore(PrincipalStore principalStore) {
        this.principalStore = principalStore;
    }
    

    /**
     * @see org.vortikal.web.service.ResourceAssertion#matches(org.vortikal.repository.Resource)
     */
    public boolean matches(Resource resource) {


        SecurityContext securityContext = SecurityContext.getSecurityContext();
        String token = securityContext.getToken();

        if (requiresAuthentication && token == null)
            throw new AuthenticationException();
        
        if (resource == null) return false;

        if (permission.equals("parent-write")) {
                
            Privilege[] parentPrivilegeSet = resource.getParentPrivilegeSet(
                securityContext.getPrincipal(), this.principalStore);
                
            for (int i = 0; i < parentPrivilegeSet.length; i++) {

                if (parentPrivilegeSet[i].getName().equals("write")) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Testing parent-write permission for principal "
                                     + securityContext.getPrincipal() + ": "
                                     + parentPrivilegeSet[i].getName() + ": true");
                    }
                    return true;
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Testing parent-write permission for principal "
                                 + securityContext.getPrincipal() + ": "
                                 + parentPrivilegeSet[i].getName() + ": false");
                }

            }

        } else if (permission.equals("unlock")) {
                
            if (resource.getActiveLocks().length > 0) {

                Principal owner = resource.getActiveLocks()[0].getUser();
                Principal p = securityContext.getPrincipal();

                if (p != null && p.equals(owner)) {
                    // FIXME: move role concept out of
                    // repositoryimpl and check for root role
                    // here.
                    return true;
                }
            }

        } else if (permission.equals("lock")) {
                
            if (resource.getActiveLocks().length == 0) {
                Privilege[] privilegeSet = resource.getPrivilegeSet(
                    securityContext.getPrincipal(), this.principalStore);
                for (int i = 0; i < privilegeSet.length; i++) {
                    if (privilegeSet[i].getName().equals("write"))
                        return true;
                }
            }

        } else {
                    
            Privilege[] privileges = resource.getPrivilegeSet(securityContext.getPrincipal(),
                                                             this.principalStore);
                    
            for (int i = 0; i < privileges.length; i++) {
                boolean match = false;

                if (privileges[i].getName().equals(permission)) {
                    match = true;
                }
                
                if (permission.equals("read-processed") &&
                    privileges[i].equals("read")) {
                    match = true;
                }

                if (logger.isDebugEnabled()) {
                    logger.debug("Matching permissions for resource " + resource
                                 + ": [" + permission + " against "
                                 + privileges[i].getName() + " = " + match + "]");
                }
                
                if (match) {
                    return true;
                }

            }
        }
        return false;
    }

    /**
     * @see org.vortikal.web.service.Assertion#conflicts(org.vortikal.web.service.Assertion)
     */
    public boolean conflicts(Assertion assertion) {
        return false;
    }


    /**
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {
        if (this.permission == null || !(
                this.permission.equals("read") ||
                this.permission.equals("write") ||
                this.permission.equals("read-processed") ||
                this.permission.equals("write-acl") ||
                this.permission.equals("parent-write") ||
                this.permission.equals("unlock") ||
                this.permission.equals("lock"))) {
            throw new BeanInitializationException(
                "Property 'permission' must " +
                "be set to one of; 'read', read-processed', 'write', " +
                "'write-acl', 'parent-write', 'unlock' or 'lock'");
        }
        if (this.principalStore == null) {
            throw new BeanInitializationException(
                "Property 'principalStore' must be set");
        }
    }


    public String toString() {
        StringBuffer sb = new StringBuffer();
		
        sb.append(super.toString());
        sb.append("; permission = ").append(this.permission);
        sb.append("; requiresAuthentication = ").append(this.requiresAuthentication);

        return sb.toString();
    }
}
