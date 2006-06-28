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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;

import org.vortikal.repository.Acl;
import org.vortikal.repository.Privilege;
import org.vortikal.repository.Repository;
import org.vortikal.repository.RepositoryAction;
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
 *  <li> <code>aclEditServices</code> - map from privileges to editing
 *  services
 *  <li> <code>modelName</code> - name of the sub-model provided
 * </ul>
 * 
 * Model data provided in the sub-model:
 * <ul>
 *   inheritance editing service
 *   <li><code>aclEditURLsL</code> - map from {@link RepositoryAction actions} to edit URLs
 *   <li><code>privileges</code> - map from {@link Privilege#getName
 *   privilege names} to {@link Privilege privilege objects}
 *   <li><code>groupingPrivilegePrincipalMap</code> - map from
 *   privileges to the grouping pseudo principal
 *   <li><code>inherited</code> - whether or not the ACL of the
 *   current resource is inherited
 *   <li><code>privilegedPseudoPrincipals</code> - map from privileges
 *   to a list of pseudo principals (from the ACL)
 *   <li><code>privilegedUsers</code> - map from privileges to a list
 *   of user principals (from the ACL)
 *   <li><code>privilegedGroups</code> - map from privileges to a list
 *   of groups (from the ACL)
 * </ul>
 */
public class ACLProvider implements ReferenceDataProvider, InitializingBean {

    private Repository repository = null;
    private Service aclInheritanceService = null;
    
    private Map groupingPrivilegePrincipalMap;

    private Map aclEditServices;
    
    private String modelName = "aclInfo";
    
    public void setRepository(Repository repository) {
        this.repository = repository;
    }
    
    public void setAclInheritanceService(Service aclInheritanceService) {
        this.aclInheritanceService = aclInheritanceService;
    }
    
    public void setAclEditServices(Map aclEditServices) {
        this.aclEditServices = aclEditServices;
    }      

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
    
    public void setGroupingPrivilegePrincipalMap(Map groupingPrivilegePrincipalMap) {
        this.groupingPrivilegePrincipalMap = groupingPrivilegePrincipalMap;
    }
    

    public void afterPropertiesSet() {
        if (this.repository == null) {
            throw new BeanInitializationException(
                "JavaBean property 'repository' must be set");
        }
        if (this.aclInheritanceService == null) {
            throw new BeanInitializationException(
                "JavaBean property 'aclInheritanceService' must be set");
        }
        if (this.aclEditServices == null) {
            throw new BeanInitializationException(
                "JavaBean property 'aclEditServices' must be set");
        }
        if (this.modelName == null) {
            throw new BeanInitializationException(
                "JavaBean property 'modelName' must be set");
        }
        if (this.groupingPrivilegePrincipalMap == null) {
            throw new BeanInitializationException(
                "JavaBean property 'groupingPrivilegePrincipalMap' must be set");
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
        Resource resource = repository.retrieve(token, uri, false);
        Map editURLs = new HashMap();

        if (!acl.isInherited()) {
            for (Iterator i = this.aclEditServices.keySet().iterator(); i.hasNext();) {
                RepositoryAction action = (RepositoryAction) i.next();
                String privilegeName = Privilege.getActionName(action);
                Service editService = (Service) this.aclEditServices.get(action);
                try {
                    String url = editService.constructLink(
                        resource, securityContext.getPrincipal());
                    editURLs.put(privilegeName, url);
                } catch (Exception e) {System.out.println("error: " + e.getMessage()); e.printStackTrace(); }
            }
        }

        try {
            if (aclInheritanceService != null) {
                String url = aclInheritanceService.constructLink(
                    resource, securityContext.getPrincipal());
                editURLs.put("inheritance", url);
            }
        } catch (Exception e) { }
        

        Map privileges = new HashMap();
        Map privilegedUsers = new HashMap();
        Map privilegedGroups = new HashMap();
        Map privilegedPseudoPrincipals = new HashMap();

        for (Iterator i = Privilege.PRIVILEGES.iterator(); i.hasNext();) {
            RepositoryAction action = (RepositoryAction) i.next();
            String actionName = Privilege.getActionName(action);
            privileges.put(actionName, action);


            privilegedUsers.put(actionName, acl.listPrivilegedUsers(action));
            privilegedGroups.put(actionName, acl.listPrivilegedGroups(action));

            List l = new ArrayList(java.util.Arrays.asList(acl.listPrivilegedPseudoPrincipals(action)));
            if (!l.contains(PseudoPrincipal.OWNER)) {
                l.add(0, PseudoPrincipal.OWNER);
            }
            privilegedPseudoPrincipals.put(actionName, l);
        }
        
        Map pseudoPrincipalPrivilegeMap = new HashMap(this.groupingPrivilegePrincipalMap);
        for (Iterator i = this.groupingPrivilegePrincipalMap.keySet().iterator(); i.hasNext();) {
            RepositoryAction action = (RepositoryAction) i.next();
            Principal p = (Principal) this.groupingPrivilegePrincipalMap.get(action);
            pseudoPrincipalPrivilegeMap.put(Privilege.getActionName(action), p);
        }

        aclModel.put("aclEditURLs", editURLs);
        aclModel.put("privileges", privileges);
        aclModel.put("groupingPrivilegePrincipalMap", pseudoPrincipalPrivilegeMap);
        aclModel.put("inherited", new Boolean(acl.isInherited()));
        aclModel.put("privilegedPseudoPrincipals", privilegedPseudoPrincipals);
        aclModel.put("privilegedUsers", privilegedUsers);
        aclModel.put("privilegedGroups", privilegedGroups);
        model.put("aclInfo", aclModel);
    }

}
