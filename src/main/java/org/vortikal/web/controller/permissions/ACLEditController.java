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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.vortikal.repository.Acl;
import org.vortikal.repository.Privilege;
import org.vortikal.repository.Repository;
import org.vortikal.repository.RepositoryAction;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalFactory;
import org.vortikal.security.PseudoPrincipal;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;



public class ACLEditController extends SimpleFormController implements InitializingBean {

    private Repository repository;
    private PrincipalFactory principalFactory;
    private RepositoryAction privilege;
    
    private Map privilegePrincipalMap;

    private Principal groupingPrincipal = PseudoPrincipal.ALL;
    
    public ACLEditController() {
        setSessionForm(true);
    }
    
    protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) throws Exception {
        binder.registerCustomEditor(java.lang.String[].class, new StringArrayPropertyEditor());
    }



    public void setRepository(Repository repository) {
        this.repository = repository;
    }


    public void setPrivilegePrincipalMap(Map privilegePrincipalMap) {
        this.privilegePrincipalMap = privilegePrincipalMap;
    }
    

    public void afterPropertiesSet() {
        if (this.repository == null) {
            throw new BeanInitializationException(
                "Bean property 'repository' must be set");
        }
        if (this.principalFactory == null) {
            throw new BeanInitializationException(
                "Bean property 'principalManager' must be set");
        }
        if (this.groupingPrincipal == null) {
            throw new BeanInitializationException(
                "Bean property 'groupingPrincipal' must be set");
        }

        if (!Privilege.PRIVILEGES.contains(this.privilege)) {
            throw new BeanInitializationException(
                "Legal values for bean property 'privilege' are defined by " +
                "Privilege.PRIVILEGES. Value is '" + this.privilege + "'.");
        }
        if (this.privilegePrincipalMap == null) {
            throw new BeanInitializationException(
                "Bean property 'privilegePrincipalMap' must be set");
        }
        Principal p = (Principal) this.privilegePrincipalMap.get(this.privilege);
        if (p != null) this.groupingPrincipal = p;
    }
    

    public void setPrivilege(RepositoryAction privilege) {
        this.privilege = privilege;
    }
    

    protected Object formBackingObject(HttpServletRequest request)
        throws Exception {
        
        String uri = RequestContext.getRequestContext().getResourceURI();
        String token = SecurityContext.getSecurityContext().getToken();
        
        Resource resource = this.repository.retrieve(token, uri, false);
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
        
        command.setGrouped(acl.containsEntry(this.privilege, this.groupingPrincipal));
        command.setOwner(resource.getOwner().getName());

        List authorizedUsers = new ArrayList(
            java.util.Arrays.asList(acl.listPrivilegedUsers(this.privilege)));
        authorizedUsers.addAll(java.util.Arrays.asList(acl.listPrivilegedPseudoPrincipals(this.privilege)));
//         if (!authorizedUsers.contains(PseudoPrincipal.OWNER)) {
//             authorizedUsers.add(0, PseudoPrincipal.OWNER);
//         }

        List authorizedGroups = new ArrayList(
            java.util.Arrays.asList(acl.listPrivilegedGroups(this.privilege)));
        command.setUsers(authorizedUsers);
        command.setGroups(authorizedGroups);

        Map removeUserURLs = new HashMap();

        for (int i = 0; i < authorizedUsers.size(); i++) {
            Principal authorizedUser = (Principal) authorizedUsers.get(i);
            Map parameters = new HashMap();
            parameters.put("removeUserAction", "true");
            parameters.put("userNames", authorizedUser.getName());
            // Switch grouping when removing individual groups:
            parameters.put("grouped", "false");

            if (!(PseudoPrincipal.OWNER.equals(authorizedUser)
                  || this.groupingPrincipal.equals(authorizedUser))) {

                String url = service.constructLink(
                    resource, securityContext.getPrincipal(), parameters);
                removeUserURLs.put(authorizedUser.getName(), url);
            }
        }
        command.setRemoveUserURLs(removeUserURLs);

        Map removeGroupURLs = new HashMap();
        for (int i = 0; i < authorizedGroups.size(); i++) {
            Principal authorizedGroup = (Principal) authorizedGroups.get(i);
            Map parameters = new HashMap();
            parameters.put("removeGroupAction", "true");
            parameters.put("groupNames", authorizedGroup.getName());
            // Switch grouping when removing individual groups:
            parameters.put("grouped", "false");
            String url = service.constructLink(
                resource, securityContext.getPrincipal(), parameters);
            removeGroupURLs.put(authorizedGroup.getName(), url);
        }
        command.setRemoveGroupURLs(removeGroupURLs);
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
        
        // Setting or unsetting the grouping principal:
        if (editCommand.isGrouped()) {
            acl.addEntry(this.privilege, this.groupingPrincipal);
        } else if (acl.containsEntry(this.privilege, this.groupingPrincipal)) {
            acl.removeEntry(this.privilege, this.groupingPrincipal);
        }
        
        // Has the user asked to save?
        if (editCommand.getSaveAction() != null) {
            this.repository.storeACL(token, resource);
            return new ModelAndView(getSuccessView());
        }

        // doing remove or add actions
        if (editCommand.getRemoveUserAction() != null) {
            String[] userNames = editCommand.getUserNames();

            for (int i = 0; i < userNames.length; i++) {
                Principal principal = null;
                if (userNames[i].startsWith("pseudo")) {
                    principal = PseudoPrincipal.getPrincipal(userNames[i]);
                } else {
                    principal = this.principalFactory.getUserPrincipal(userNames[i]);
                }
                if (!PseudoPrincipal.OWNER.equals(principal)) {
                    acl.removeEntry(this.privilege, principal);
                }
            }
            return showForm(request, response, new BindException(
                                getACLEditCommand(editCommand.getResource()),
                                this.getCommandName()));

        } else if (editCommand.getRemoveGroupAction() != null) {
            String[] groupNames = editCommand.getGroupNames();
            
            for (int i = 0; i < groupNames.length; i++) {
                Principal group = this.principalFactory.getGroupPrincipal(groupNames[i]);
                acl.removeEntry(this.privilege, group);
            }
            return showForm(request, response, new BindException(
                    getACLEditCommand(resource), this.getCommandName()));
            
        } else if (editCommand.getAddUserAction() != null) {
            String[] userNames = editCommand.getUserNames();
            for (int i = 0; i < userNames.length; i++) {
                Principal principal = this.principalFactory.getUserPrincipal(userNames[i]);
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
                Principal group = this.principalFactory.getGroupPrincipal(groupNames[i]);

                acl.addEntry(this.privilege, group);
            }
            return showForm(request, response, new BindException(
                                getACLEditCommand(resource),
                                this.getCommandName()));

        } else {
            return new ModelAndView(getSuccessView());
        }
    }


    public void setPrincipalFactory(PrincipalFactory principalFactory) {
        this.principalFactory = principalFactory;
    }
}

