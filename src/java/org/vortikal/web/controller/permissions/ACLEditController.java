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
package org.vortikal.web.controller.permissions;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

import org.vortikal.repository.Acl;
import org.vortikal.repository.PrivilegeDefinition;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalManager;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;


public class ACLEditController extends SimpleFormController implements InitializingBean {

    private Repository repository = null;
    private PrincipalManager principalManager = null;
    private String privilege = null;
    
    
    public ACLEditController() {
        setSessionForm(true);
    }
    

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setPrincipalManager(PrincipalManager principalManager) {
        this.principalManager = principalManager;
    }


    public void afterPropertiesSet() {
        if (this.repository == null) {
            throw new BeanInitializationException(
                "Bean property 'repository' must be set");
        }
        if (this.principalManager == null) {
            throw new BeanInitializationException(
                "Bean property 'principalManager' must be set");
        }
    }
    

    public void setPrivilege(String privilege) {
        if (! ("read".equals(privilege) ||
               "write".equals(privilege) ||
               "write-acl".equals(privilege))) {
            throw new IllegalArgumentException(
                "Legal values for bean property 'privilege' are 'read', 'write' and " +
                "'write-acl'. Value is '" + privilege + "'.");
        }
        this.privilege = privilege;
    }
    


    protected Object formBackingObject(HttpServletRequest request)
        throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        
        String uri = requestContext.getResourceURI();
        String token = securityContext.getToken();
        Acl acl = repository.getACL(token, uri);

        return getACLEditCommand(acl);
    }

    

    private ACLEditCommand getACLEditCommand(Acl acl) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        Service service = requestContext.getService();
        
        Resource resource = repository.retrieve(securityContext.getToken(),
                                                requestContext.getResourceURI(), false);
        String submitURL = service.constructLink(
            resource, securityContext.getPrincipal());
         
        ACLEditCommand command = new ACLEditCommand(submitURL);
        
        Principal auth = principalManager.getPseudoPrincipal(Principal.NAME_PSEUDO_AUTHENTICATED);
        command.setEveryone(acl.hasPrivilege(auth, privilege));
        command.setOwner(resource.getOwner().getQualifiedName());

        Principal[] authorizedUsers = acl.listPrivilegedUsers(privilege);
        Principal[] authorizedGroups = acl.listPrivilegedGroups(privilege);
        command.setUsers(authorizedUsers);
        command.setGroups(authorizedGroups);

        String[] withdrawUserURLs = new String[authorizedUsers.length];

        for (int i = 0; i < authorizedUsers.length; i++) {
            Map parameters = new HashMap();
            parameters.put("removeUserAction", "true");
            parameters.put("userNames", authorizedUsers[i]);

            if (!("dav:owner".equals(authorizedUsers[i]) ||
                  "dav:authenticated".equals(authorizedUsers[i]))) {
                String url = service.constructLink(
                    resource, securityContext.getPrincipal(), parameters);
                withdrawUserURLs[i] = url;
            }
        }
        command.setWithdrawUserURLs(withdrawUserURLs);
        
        String[] withdrawGroupURLs = new String[authorizedGroups.length];

        for (int i = 0; i < authorizedGroups.length; i++) {
            Map parameters = new HashMap();
            parameters.put("removeGroupAction", "true");
            parameters.put("groupNames", authorizedGroups[i]);
            String url = service.constructLink(
                resource, securityContext.getPrincipal(), parameters);
            withdrawGroupURLs[i] = url;
        }
        command.setWithdrawGroupURLs(withdrawGroupURLs);
        command.setEditedACL(acl);
        return command;
    }
    

    protected boolean isFormSubmission(HttpServletRequest request) {
        return "POST".equals(request.getMethod())
            || ("GET".equals(request.getMethod())
                && (request.getParameter("removeUserAction") != null
                    || request.getParameter("removeGroupAction") != null));
    }
    
    

    /** Override to reset actions in case of errors.
     * 
     * @see org.springframework.web.servlet.mvc.AbstractFormController#processFormSubmission(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
     */
    protected ModelAndView processFormSubmission(HttpServletRequest req, HttpServletResponse resp, Object command, BindException errors) throws Exception {
        if (errors.hasErrors()) {
            ACLEditCommand editCommand = (ACLEditCommand) command;
            editCommand.setAddGroupAction(null);
            editCommand.setAddUserAction(null);
            editCommand.setRemoveUserAction(null);
            editCommand.setRemoveGroupAction(null);
        }
        return super.processFormSubmission(req, resp, command, errors);
    }


    protected ModelAndView onSubmit(HttpServletRequest request,
                                    HttpServletResponse response,
                                    Object command, BindException errors) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        
        ACLEditCommand editCommand = (ACLEditCommand) command;

        Acl acl = editCommand.getEditedACL();
        
        String uri = requestContext.getResourceURI();
        String token = securityContext.getToken();
        Resource resource = repository.retrieve(token, uri, false);

        // did the user cancel?
        if (editCommand.getCancelAction() != null) {
            return new ModelAndView(getSuccessView());
        }
        
        Principal auth = principalManager.getPseudoPrincipal(Principal.NAME_PSEUDO_AUTHENTICATED);
        Principal all =  principalManager.getPseudoPrincipal(Principal.NAME_PSEUDO_ALL);
        
        // Setting or unsetting dav:authenticated 
        if (!acl.hasPrivilege(auth, privilege)) {

            if (editCommand.isEveryone()) {
                if ("read".equals(privilege)) {
                    acl.addEntry(PrivilegeDefinition.READ_PROCESSED, all);
                }
                acl.addEntry(privilege, auth);
            }
        } else {

            if (!editCommand.isEveryone()) {
                if (PrivilegeDefinition.READ.equals(privilege)) 
                    acl.removeEntry(PrivilegeDefinition.READ_PROCESSED, all);
                
                acl.removeEntry(privilege, auth);
            }
        }
        
        // Has the user asked to save?
        if (editCommand.getSaveAction() != null) {
            repository.storeACL(token, uri, editCommand.getEditedACL());
            return new ModelAndView(getSuccessView());
        }

        // doing remove or add actions
        if (editCommand.getRemoveUserAction() != null) {
            String[] userNames = editCommand.getUserNames();

            for (int i = 0; i < userNames.length; i++) {
                Principal principal = principalManager.getUserPrincipal(userNames[i]);

                acl.removeEntry(this.privilege, principal);
            }
            return showForm(request, response, new BindException(
                                getACLEditCommand(editCommand.getEditedACL()),
                                this.getCommandName()));

        } else if (editCommand.getRemoveGroupAction() != null) {
            String[] groupNames = editCommand.getGroupNames();
            
            for (int i = 0; i < groupNames.length; i++) {
                Principal group = principalManager.getGroupPrincipal(groupNames[i]);
                acl.removeEntry(this.privilege, group);
            }
            return showForm(request, response, new BindException(
                    getACLEditCommand(acl), this.getCommandName()));
            
        } else if (editCommand.getAddUserAction() != null) {
            String[] userNames = editCommand.getUserNames();
            for (int i = 0; i < userNames.length; i++) {
                Principal principal = principalManager.getUserPrincipal(userNames[i]);
                acl.addEntry(this.privilege, principal);
            }
            ModelAndView mv =  showForm(
                request, response, new BindException(
                    getACLEditCommand(acl),
                    this.getCommandName()));
            return mv;

        } else if (editCommand.getAddGroupAction() != null) {
            String[] groupNames = editCommand.getGroupNames();
            
           
            for (int i = 0; i < groupNames.length; i++) {
                Principal group = principalManager.getGroupPrincipal(groupNames[i]);

                acl.addEntry(this.privilege, group);
            }
            return showForm(request, response, new BindException(
                                getACLEditCommand(editCommand.getEditedACL()),
                                this.getCommandName()));

        } else {
            return new ModelAndView(getSuccessView());
        }
    }
}

