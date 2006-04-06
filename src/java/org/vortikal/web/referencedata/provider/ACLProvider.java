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
package org.vortikal.web.referencedata.provider;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.Acl;
import org.vortikal.repository.Privilege;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalManager;
import org.vortikal.security.PseudoPrincipal;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.referencedata.ReferenceDataProvider;
import org.vortikal.web.service.Service;

/**
 * Model builder that retrieves various Acces Control List (ACL)
 * information for the current resource. The information is made
 * available in the model as a submodel with name
 * <code>aclInfo</code>.
 * 
 * Configurable properties:
 * <ul>
 *  <li><code>repository</code> - the {@link Repository} is required
 *  <li> <code>aclInheritanceService</code> - service for editing the 'inherited
 *  property' of the ACL for a resource
 *  <li> <code>editWritePermissionsService</code> - service for editing 'write'
 *  permissions for a resource
 *  <li> <code>editReadPermissionsService</code> - service for editing 'read'
 *  permissions for a resource
 *  <li> <code>editWriteACLPermissionsService</code> - service for editing 'write ACL'
 *  permissions for a resource
 * </ul>
 * 
 * Model data provided:
 * <ul>
 *   <li><code>readAuthorizedUsers</code> - the users having read
 *   permission
 *   <li><code>readAuthorizedGroups</code> - the groups having read
 *   permission
 *   <li><code>writeAuthorizedUsers</code> - the users having write
 *   permission
 *   <li><code>writeAuthorizedGroups</code> - the groups having write
 *   permission
 *   <li><code>writeAclAuthorizedUsers</code> - the users having write
 *   ACL permission
 *   <li><code>writeAclAuthorizedGroups</code> - the groups having
 *   write ACL permission
 *   <li><code>everyoneReadAuthorized</code> - whether all
 *      authenticated users have read permission
 *   <li><code>everyoneWriteAuthorized</code> - whether all
 *       authenticated users have write permission
 *   <li><code>everyoneWriteAclAuthorized</code> - whether all
 *       authenticated users have write ACL permission
 *   <li><code>aclInheritedFrom</code> - the resource from which the
 *       current resource inherits its ACL (may be null)
 *   <li><code>aclInheritanceServiceURL</code> - the URL to the ACL
 *   inheritance editing service
 *   <li><code>editWritePermissionsServiceURL</code> - the URL to the
 *   write permissions editing service
 *   <li><code>editReadAythorizedUsersServiceURL</code> - the URL to
 *   the read permissions editing service
 *   <li><code>editWriteACLPermissionsServiceURL</code> - the URL to
 *   the write ACL permissions editing service
 * </ul>
 */
public class ACLProvider implements ReferenceDataProvider, InitializingBean {

    private Repository repository = null;
    private Service aclInheritanceService = null;
    private Service editWritePermissionsService = null;
    private Service editReadPermissionsService = null;
    private Service editWriteACLPermissionsService = null;
    

    
    public void setRepository(Repository repository) {
        this.repository = repository;
    }
    
    public void setAclInheritanceService(Service aclInheritanceService) {
        this.aclInheritanceService = aclInheritanceService;
    }
    
    public void setEditWritePermissionsService(
        Service editWritePermissionsService)  {
        this.editWritePermissionsService = editWritePermissionsService;
    }

    public void setEditReadPermissionsService(
        Service editReadPermissionsService)  {
        this.editReadPermissionsService = editReadPermissionsService;
    }

    public void setEditWriteACLPermissionsService(
        Service editWriteACLPermissionsService)  {
        this.editWriteACLPermissionsService = editWriteACLPermissionsService;
    }


    public void afterPropertiesSet() {
        if (this.repository == null) {
            throw new BeanInitializationException(
                "Bean property 'repository' must be set");
        }
    }


    public void referenceData(Map model, HttpServletRequest request)
        throws Exception {
        Map aclModel = new HashMap();

        SecurityContext securityContext = SecurityContext.getSecurityContext();
        RequestContext requestContext = RequestContext.getRequestContext();
        String uri = requestContext.getResourceURI();
        String token = securityContext.getToken();
        
        Acl acl = repository.getACL(token, uri);

        // XXX: Pseudo needs to be handled in the new way
        // XXX: groups are now principals
        Principal[] readAuthorizedUsers = acl.listPrivilegedUsers(Privilege.READ);
        aclModel.put("readAuthorizedUsers", readAuthorizedUsers);
        Principal[] readAuthorizedGroups = acl.listPrivilegedGroups(Privilege.READ);
        aclModel.put("readAuthorizedGroups", readAuthorizedGroups);

        Principal[] writeAuthorizedUsers = acl.listPrivilegedUsers(Privilege.WRITE);
        aclModel.put("writeAuthorizedUsers", writeAuthorizedUsers);
        Principal[] writeAuthorizedGroups = acl.listPrivilegedGroups(Privilege.WRITE);
        aclModel.put("writeAuthorizedGroups", writeAuthorizedGroups);

        Principal[] writeAclAuthorizedUsers = acl.listPrivilegedUsers(Privilege.ALL);
        aclModel.put("writeAclAuthorizedUsers", writeAclAuthorizedUsers);
        Principal[] writeAclAuthorizedGroups = acl.listPrivilegedGroups(Privilege.ALL);
        aclModel.put("writeAclAuthorizedGroups", writeAclAuthorizedGroups);

        Principal auth = PseudoPrincipal.AUTHENTICATED;
        aclModel.put("everyoneReadAuthorized" , 
                new Boolean(acl.hasPrivilege(Privilege.READ, auth)));
        aclModel.put("everyoneWriteAuthorized", 
                new Boolean(acl.hasPrivilege(Privilege.WRITE, auth)));
        aclModel.put("everyoneWriteAclAuthorized", 
                new Boolean(acl.hasPrivilege(Privilege.ALL, auth)));

        aclModel.put("aclInherited", new Boolean(acl.isInherited()));

        Resource resource = repository.retrieve(token, uri, false);

        try {
            if (aclInheritanceService != null) {
                String url = aclInheritanceService.constructLink(
                    resource, securityContext.getPrincipal());
                aclModel.put("aclInheritanceServiceURL", url);
            }
        } catch (Exception e) {
            // Ignore
        }

        try {
            if (editWritePermissionsService != null) {
                String url = editWritePermissionsService.constructLink(
                        resource, securityContext.getPrincipal());
                aclModel.put("editWritePermissionsServiceURL", url);
            }
        } catch (Exception e) {
            // Ignore
        }


        try {
            if (editReadPermissionsService != null) {
                String url = editReadPermissionsService.constructLink(resource,
                        securityContext.getPrincipal());
                aclModel.put("editReadPermissionsServiceURL", url);
            }
        } catch (Exception e) {
            // Ignore
        }

        try {
            if (editWriteACLPermissionsService != null) {
                String url = editWriteACLPermissionsService.constructLink(
                        resource, securityContext.getPrincipal());
                aclModel.put("editWriteACLPermissionsServiceURL", url);
            }
        } catch (Exception e) {
            // Ignore
        }

        model.put("aclInfo", aclModel);
    }

}
