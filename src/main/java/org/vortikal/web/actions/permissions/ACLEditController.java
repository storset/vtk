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
package org.vortikal.web.actions.permissions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.vortikal.repository.Acl;
import org.vortikal.repository.Path;
import org.vortikal.repository.Privilege;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.Principal.Type;
import org.vortikal.security.PrincipalFactory;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;

public class ACLEditController extends SimpleFormController implements InitializingBean {

    private Repository repository;
    private Privilege privilege;
    private PrincipalFactory principalFactory;

    private Map<Privilege, Principal> privilegePrincipalMap;

    private Principal groupingPrincipal = PrincipalFactory.ALL;

    public ACLEditController() {
        setSessionForm(true);
    }

    protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder)
            throws Exception {
        binder.registerCustomEditor(java.lang.String[].class,
                new StringArrayPropertyEditor());
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setPrivilegePrincipalMap(
            Map<Privilege, Principal> privilegePrincipalMap) {
        this.privilegePrincipalMap = privilegePrincipalMap;
    }

    public void afterPropertiesSet() {
        if (this.repository == null) {
            throw new BeanInitializationException(
                    "Bean property 'repository' must be set");
        }
        if (this.groupingPrincipal == null) {
            throw new BeanInitializationException(
                    "Bean property 'groupingPrincipal' must be set");
        }
        

        if (this.privilege == null) {
            throw new BeanInitializationException(
                    "Bean property 'privilege' must be set");
        }
        if (this.privilegePrincipalMap == null) {
            throw new BeanInitializationException(
                    "Bean property 'privilegePrincipalMap' must be set");
        }
        Principal p = this.privilegePrincipalMap.get(this.privilege);
        if (p != null) {
            this.groupingPrincipal = p;
        }
    }

    public void setPrivilege(Privilege privilege) {
        this.privilege = privilege;
    }

    protected Object formBackingObject(HttpServletRequest request) throws Exception {

        Path uri = RequestContext.getRequestContext().getResourceURI();
        String token = SecurityContext.getSecurityContext().getToken();

        Resource resource = this.repository.retrieve(token, uri, false);
        return getACLEditCommand(resource);
    }

    private ACLEditCommand getACLEditCommand(Resource resource) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        Service service = requestContext.getService();

        String submitURL = service
                .constructLink(resource, securityContext.getPrincipal());
        ACLEditCommand command = new ACLEditCommand(submitURL);

        command.setResource(resource);

        Acl acl = resource.getAcl();
        command.setGrouped(acl.containsEntry(this.privilege, this.groupingPrincipal));
        command.setOwner(resource.getOwner().getName());

        List<Principal> authorizedUsers = new ArrayList<Principal>(Arrays.asList(acl
                .listPrivilegedUsers(this.privilege)));
        authorizedUsers.addAll(Arrays.asList(acl
                .listPrivilegedPseudoPrincipals(this.privilege)));

        List<Principal> authorizedGroups = new ArrayList<Principal>(Arrays.asList(acl
                .listPrivilegedGroups(this.privilege)));

        command.setUsers(authorizedUsers);
        command.setGroups(authorizedGroups);

        Map<String, String> removeUserURLs = new HashMap<String, String>();
        command.setRemoveUserURLs(removeUserURLs);

        for (Principal authorizedUser : authorizedUsers) {
            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("removeUserAction", "true");
            parameters.put("userNames", authorizedUser.getName());
            // Switch grouping when removing individual groups:
            parameters.put("grouped", "false");

            if (!(PrincipalFactory.OWNER.equals(authorizedUser) || this.groupingPrincipal
                    .equals(authorizedUser))) {

                String url = service.constructLink(resource, securityContext
                        .getPrincipal(), parameters);
                removeUserURLs.put(authorizedUser.getName(), url);
            }
        }

        Map<String, String> removeGroupURLs = new HashMap<String, String>();
        command.setRemoveGroupURLs(removeGroupURLs);

        for (Principal authorizedGroup : authorizedGroups) {
            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("removeGroupAction", "true");
            parameters.put("groupNames", authorizedGroup.getName());
            // Switch grouping when removing individual groups:
            parameters.put("grouped", "false");
            String url = service.constructLink(resource, securityContext.getPrincipal(),
                    parameters);
            removeGroupURLs.put(authorizedGroup.getName(), url);
        }

        return command;
    }

    protected boolean isFormSubmission(HttpServletRequest request) {
        return "POST".equals(request.getMethod())
                || ("GET".equals(request.getMethod()) && (request
                        .getParameter("removeUserAction") != null || request
                        .getParameter("removeGroupAction") != null));
    }

    /**
     * Override to reset actions in case of errors.
     */
    protected ModelAndView processFormSubmission(HttpServletRequest req,
            HttpServletResponse resp, Object command, BindException errors)
            throws Exception {
        if (errors.hasErrors()) {
            ACLEditCommand editCommand = (ACLEditCommand) command;
            editCommand.setAddGroupAction(null);
            editCommand.setAddUserAction(null);
            editCommand.setRemoveUserAction(null);
            editCommand.setRemoveGroupAction(null);
            editCommand.setSaveAction(null);
            editCommand.getUserNameEntries().removeAll(editCommand.getUserNameEntries());
        }
        return super.processFormSubmission(req, resp, command, errors);
    }

    protected ModelAndView onSubmit(HttpServletRequest request,
            HttpServletResponse response, Object command, BindException errors)
            throws Exception {
        SecurityContext securityContext = SecurityContext.getSecurityContext();

        ACLEditCommand editCommand = (ACLEditCommand) command;

        Resource resource = editCommand.getResource();
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
            addToAcl(acl, editCommand.getUserNameEntries(), Type.USER);
            addToAcl(acl, editCommand.getGroupNames(), Type.GROUP);
            this.repository.storeACL(token, resource);
            return new ModelAndView(getSuccessView());
        }

        // doing remove or add actions
        if (editCommand.getRemoveUserAction() != null) {

            for (String userName : editCommand.getUserNames()) {
                Principal principal = null;
                if (userName.startsWith("pseudo")) {
                    principal = principalFactory.getPrincipal(userName, Type.PSEUDO);
                } else {
                    principal = principalFactory.getPrincipal(userName, Type.USER);
                }
                if (!PrincipalFactory.OWNER.equals(principal)) {
                    acl.removeEntry(this.privilege, principal);
                }
            }

            return showForm(request, response, new BindException(
                    getACLEditCommand(editCommand.getResource()), this.getCommandName()));

        } else if (editCommand.getRemoveGroupAction() != null) {
            String[] groupNames = editCommand.getGroupNames();

            for (int i = 0; i < groupNames.length; i++) {
                Principal group = principalFactory
                        .getPrincipal(groupNames[i], Type.GROUP);
                acl.removeEntry(this.privilege, group);
            }
            return showForm(request, response, new BindException(
                    getACLEditCommand(resource), this.getCommandName()));

        } else if (editCommand.getAddUserAction() != null) {
            addToAcl(acl, editCommand.getUserNameEntries(), Type.USER);
            return showForm(request, response, new BindException(
                    getACLEditCommand(resource), this.getCommandName()));

        } else if (editCommand.getAddGroupAction() != null) {
            addToAcl(acl, editCommand.getGroupNames(), Type.GROUP);
            return showForm(request, response, new BindException(
                    getACLEditCommand(resource), this.getCommandName()));

        } else {
            return new ModelAndView(getSuccessView());
        }
    }

    private void addToAcl(Acl acl, List<String> values, Type type) {
        for (String value : values) {
            Principal p = principalFactory.getPrincipal(value, type);
            acl.addEntry(this.privilege, p);
        }
    }

    private void addToAcl(Acl acl, String[] values, Type type) {
        for (String value : values) {
            Principal principal = principalFactory.getPrincipal(value, type);
            acl.addEntry(this.privilege, principal);
        }
    }

    @Required
    public void setPrincipalFactory(PrincipalFactory principalFactory) {
        this.principalFactory = principalFactory;
    }

}
