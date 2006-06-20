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
import org.vortikal.repository.Privilege;
import org.vortikal.repository.Repository;
import org.vortikal.repository.RepositoryAction;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalManager;
import org.vortikal.security.PseudoPrincipal;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;


public class ACLEditController extends SimpleFormController implements InitializingBean {

    private Repository repository;
    private PrincipalManager principalManager;
    private RepositoryAction privilege;
    
    
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
        if (!Privilege.PRIVILEGES.contains(this.privilege)) {
            throw new BeanInitializationException(
                "Legal values for bean property 'privilege' are defined by " +
                "Privilege.PRIVILEGES. Value is '" + privilege + "'.");
        }
    }
    

    public void setPrivilege(RepositoryAction privilege) {
        this.privilege = privilege;
    }
    

    protected Object formBackingObject(HttpServletRequest request)
        throws Exception {
        
        String uri = RequestContext.getRequestContext().getResourceURI();
        String token = SecurityContext.getSecurityContext().getToken();
        
        Resource resource = repository.retrieve(token, uri, false);
        return getACLEditCommand(resource);
    }

    

    private ACLEditCommand getACLEditCommand(Resource resource) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        Service service = requestContext.getService();
        
        Acl acl = resource.getAcl();
        
        String submitURL = service.constructLink(
            resource, securityContext.getPrincipal());
         
        ACLEditCommand command = new ACLEditCommand(submitURL);
        
        Principal auth = PseudoPrincipal.AUTHENTICATED;
        Principal pseudoOwner = PseudoPrincipal.OWNER;
        command.setEveryone(resource.isAuthorized(this.privilege, auth));
        command.setOwner(resource.getOwner().getName());

        Principal[] authorizedUsers = acl.listPrivilegedUsers(this.privilege);
        Principal[] authorizedGroups = acl.listPrivilegedGroups(this.privilege);
        command.setUsers(authorizedUsers);
        command.setGroups(authorizedGroups);

        String[] withdrawUserURLs = new String[authorizedUsers.length];

        for (int i = 0; i < authorizedUsers.length; i++) {
            Map parameters = new HashMap();
            parameters.put("removeUserAction", "true");
            parameters.put("userNames", authorizedUsers[i]);

            if (!(pseudoOwner.equals(authorizedUsers[i]) ||
                  auth.equals(authorizedUsers[i]))) {
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
        command.setResource(resource);
        return command;
    }
    

    protected boolean isFormSubmission(HttpServletRequest request) {
        return "POST".equals(request.getMethod())
            || ("GET".equals(request.getMethod())
                && (request.getParameter("removeUserAction") != null
                    || request.getParameter("removeGroupAction") != null));
    }
    
    

    /**
     * Override to reset actions in case of errors.
     */
    protected ModelAndView processFormSubmission(
        HttpServletRequest req, HttpServletResponse resp,
        Object command, BindException errors) throws Exception {
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
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        
        ACLEditCommand editCommand = (ACLEditCommand) command;

        Resource resource = editCommand.getResource();
        String uri = resource.getURI();
        Acl acl = resource.getAcl();
        
        String token = securityContext.getToken();

        // did the user cancel?
        if (editCommand.getCancelAction() != null) {
            return new ModelAndView(getSuccessView());
        }
        
        Principal auth = PseudoPrincipal.AUTHENTICATED;
        
        // Setting or unsetting pseudo:authenticated 
        if (!resource.isAuthorized(this.privilege, auth) && editCommand.isEveryone()) {
            if (Privilege.READ.equals(this.privilege))
                acl.addEntry(Privilege.READ_PROCESSED, PseudoPrincipal.ALL);
            acl.addEntry(this.privilege, auth);
        } else if (resource.isAuthorized(privilege, auth) && !editCommand.isEveryone()) {
            if (Privilege.READ.equals(this.privilege)) 
                acl.removeEntry(Privilege.READ_PROCESSED, PseudoPrincipal.ALL);
            acl.removeEntry(this.privilege, auth);
        }
        
        // Has the user asked to save?
        if (editCommand.getSaveAction() != null) {
            repository.storeACL(token, uri, acl);
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
                                getACLEditCommand(editCommand.getResource()),
                                this.getCommandName()));

        } else if (editCommand.getRemoveGroupAction() != null) {
            String[] groupNames = editCommand.getGroupNames();
            
            for (int i = 0; i < groupNames.length; i++) {
                Principal group = principalManager.getGroupPrincipal(groupNames[i]);
                acl.removeEntry(this.privilege, group);
            }
            return showForm(request, response, new BindException(
                    getACLEditCommand(resource), this.getCommandName()));
            
        } else if (editCommand.getAddUserAction() != null) {
            String[] userNames = editCommand.getUserNames();
            for (int i = 0; i < userNames.length; i++) {
                Principal principal = principalManager.getUserPrincipal(userNames[i]);
                acl.addEntry(this.privilege, principal);
            }
            ModelAndView mv =  showForm(
                request, response, new BindException(
                    getACLEditCommand(resource),
                    this.getCommandName()));
            return mv;

        } else if (editCommand.getAddGroupAction() != null) {
            String[] groupNames = editCommand.getGroupNames();
            
           
            for (int i = 0; i < groupNames.length; i++) {
                Principal group = principalManager.getGroupPrincipal(groupNames[i]);

                acl.addEntry(this.privilege, group);
            }
            return showForm(request, response, new BindException(
                                getACLEditCommand(resource),
                                this.getCommandName()));

        } else {
            return new ModelAndView(getSuccessView());
        }
    }
}

