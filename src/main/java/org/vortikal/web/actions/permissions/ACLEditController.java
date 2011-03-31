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
import java.util.Iterator;
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
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;

public class ACLEditController extends SimpleFormController implements InitializingBean {

    private Privilege privilege;
    private PrincipalFactory principalFactory;
    private Map<Privilege, List<String>> permissionShortcuts;

    public ACLEditController() {
        setSessionForm(true);
    }

    protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder)
            throws Exception {
        binder.registerCustomEditor(java.lang.String[].class,
                new StringArrayPropertyEditor());
    }

    public void afterPropertiesSet() {
        if (this.privilege == null) {
            throw new BeanInitializationException(
                    "Bean property 'privilege' must be set");
        }
    }

    public void setPrivilege(Privilege privilege) {
        this.privilege = privilege;
    }

    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        Path uri = requestContext.getResourceURI();
        Repository repository = requestContext.getRepository();
        String token = requestContext.getSecurityToken();
        Resource resource = repository.retrieve(token, uri, false);
        return getACLEditCommand(resource, requestContext.getPrincipal());
    }

    private ACLEditCommand getACLEditCommand(Resource resource, Principal principal) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        Service service = requestContext.getService();

        String submitURL = service.constructLink(resource, principal);
        ACLEditCommand command = new ACLEditCommand(submitURL);

        command.setResource(resource);

        Acl acl = resource.getAcl();

        List<Principal> authorizedUsers = new ArrayList<Principal>(Arrays.asList(acl
                .listPrivilegedUsers(this.privilege)));
        authorizedUsers.addAll(Arrays.asList(acl
                .listPrivilegedPseudoPrincipals(this.privilege)));

        List<Principal> authorizedGroups = new ArrayList<Principal>(Arrays.asList(acl
                .listPrivilegedGroups(this.privilege)));
        
        List<String> shortcuts = this.permissionShortcuts.get(this.privilege);
        if (shortcuts != null) {    
          command.setShortcuts(extractAndCheckShortcuts(authorizedUsers, authorizedGroups, shortcuts));
        }

        command.setUsers(authorizedUsers);
        command.setGroups(authorizedGroups);
        
        Map<String, String> removeUserURLs = new HashMap<String, String>();
        for (Principal authorizedUser : authorizedUsers) {
            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("removeUserAction", "true");
            parameters.put("userNames", authorizedUser.getName());
            String url = service.constructLink(resource, principal, parameters);
            removeUserURLs.put(authorizedUser.getName(), url);
        }
        command.setRemoveUserURLs(removeUserURLs);

        Map<String, String> removeGroupURLs = new HashMap<String, String>();
        for (Principal authorizedGroup : authorizedGroups) {
            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("removeGroupAction", "true");
            parameters.put("groupNames", authorizedGroup.getName());

            String url = service.constructLink(resource, principal,
                    parameters);
            removeGroupURLs.put(authorizedGroup.getName(), url);
        }
        command.setRemoveGroupURLs(removeGroupURLs);

        return command;
    }
    
    private String[][] extractAndCheckShortcuts (List<Principal> authorizedUsers, 
            List<Principal> authorizedGroups, List<String> shortcuts) {
        
        String checkedShortcuts[][] = new String[shortcuts.size()][2];
        int i = 0;
        
        for (String shortcut: shortcuts) {
            boolean checked = false;
            
            if (shortcut.startsWith("user:")) {
                Iterator<Principal> it = authorizedUsers.iterator();
                while (it.hasNext()) {
                    Principal p = it.next();
                    if (("user:" + p.getName()).equals(shortcut)) {
                        checked = true;
                        it.remove();
                    }
                }
            } else if (shortcut.startsWith("group:")) {
                Iterator<Principal> it = authorizedGroups.iterator();
                while (it.hasNext()) {
                    Principal p = it.next();
                    if (("group:" + p.getName()).equals(shortcut)) {
                        checked = true;
                        it.remove();
                    }
                }  
            }
            
            checkedShortcuts[i][0] = shortcut;
            if (checked) {
              checkedShortcuts[i][1] = "checked";
            } else {
              checkedShortcuts[i][1] = ""; 
            }
            i++;
        }
        
        return checkedShortcuts;
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

        ACLEditCommand editCommand = (ACLEditCommand) command;

        Resource resource = editCommand.getResource();
        Acl acl = resource.getAcl();

        RequestContext requestContext = RequestContext.getRequestContext();
        Repository repository = requestContext.getRepository();
        String token = requestContext.getSecurityToken();

        // Did the user cancel?
        if (editCommand.getCancelAction() != null) {
            return new ModelAndView(getSuccessView());
        }
        
        // Remove or add shortcuts
        // TODO: does not work when remove command - skip until find solution
        if(editCommand.getRemoveUserAction() == null 
           && editCommand.getRemoveGroupAction() == null) {
          aclShortcuts(editCommand, acl);
        }
        
        // Has the user asked to save?
        if (editCommand.getSaveAction() != null) {      
            addToAcl(acl, editCommand.getUserNameEntries(), Type.USER);
            addToAcl(acl, editCommand.getGroupNames(), Type.GROUP);
            repository.storeACL(token, resource);
            return new ModelAndView(getSuccessView());
        }

        // Doing remove or add actions
        if (editCommand.getRemoveUserAction() != null) {
            removeFromAcl(acl, editCommand.getUserNames(), Type.USER);
            return showForm(request, response, new BindException(
                    getACLEditCommand(resource, requestContext.getPrincipal()),
                    this.getCommandName()));

        } else if (editCommand.getRemoveGroupAction() != null) {
            removeFromAcl(acl, editCommand.getGroupNames(), Type.GROUP);
            return showForm(request, response, new BindException(
                    getACLEditCommand(resource, requestContext.getPrincipal()), 
                    this.getCommandName()));

        } else if (editCommand.getAddUserAction() != null) {
            addToAcl(acl, editCommand.getUserNameEntries(), Type.USER);
            return showForm(request, response, new BindException(
                    getACLEditCommand(resource, requestContext.getPrincipal()), 
                    this.getCommandName()));

        } else if (editCommand.getAddGroupAction() != null) {
            addToAcl(acl, editCommand.getGroupNames(), Type.GROUP);
            return showForm(request, response, new BindException(
                    getACLEditCommand(resource, requestContext.getPrincipal()), 
                    this.getCommandName()));

        } else {
            return new ModelAndView(getSuccessView());
        }
    }

    private void aclShortcuts(ACLEditCommand editCommand, Acl acl) {
        
        String[] updatedShortcuts = editCommand.getUpdatedShortcuts();
        String[][] shortcuts = editCommand.getShortcuts();
        
        int count = 0;
        for (String[] shortcut : shortcuts) {
            boolean checkedNotFound = true; // remove condition
            boolean uncheckedFound = false; // add condition
            for (String update : updatedShortcuts) {
                if (shortcut[0].equals(update) && shortcut[1].equals("checked"))  {
                    checkedNotFound = false; 
                } else if (shortcut[0].equals(update) && shortcut[1].equals("")) {
                    uncheckedFound = true;
                }
            }

            // Remove
            if (checkedNotFound) {
                shortcuts[count][1] = "";
                String[] remove = new String[1];
                Type type = null;

                if (shortcuts[count][0].startsWith("user:")) {
                    remove[0] = shortcut[0].replace("user:", "");
                    type = Type.USER;
                } else if (shortcut[0].startsWith("group:")) {
                    remove[0] = shortcut[0].replace("group:", "");
                    type = Type.GROUP;
                }
                removeFromAcl(acl, remove, type);
            }

            // Add
            if (uncheckedFound) {
                shortcuts[count][1] = "checked";
                String[] add = new String[1];
                Type type = null;
                if (shortcut[0].startsWith("user:")) {
                    add[0] = shortcut[0].replace("user:", "");
                    if (add[0].startsWith("pseudo:")) {
                        type = Type.PSEUDO;                          
                    } else {
                        type = Type.USER;                           
                    }
                } else if (shortcut[0].startsWith("group:")) {
                    add[0] = shortcut[0].replace("group:", "");
                    type = Type.GROUP;
                }  
                addToAcl(acl, add, type);
            }
            
            count++;
        }
        
        editCommand.setUpdatedShortcuts(new String[0]);
        editCommand.setShortcuts(shortcuts);
    }
    
    private void removeFromAcl(Acl acl, List<String> values, Type type) {
        for (String value : values) {
            Type tmpType = type;
            if (type.equals(Type.USER)) {
              if (value.startsWith("pseudo:")) {
                  tmpType  = Type.PSEUDO;  
              }
            }
            Principal principal = principalFactory.getPrincipal(value, tmpType);
            acl.removeEntry(this.privilege, principal);
        } 
    }
    
    private void removeFromAcl(Acl acl, String[] values, Type type) {
        for (String value : values) {
            Type tmpType = type;
            if (type.equals(Type.USER)) {
              if (value.startsWith("pseudo:")) {
                  tmpType  = Type.PSEUDO;  
              }
            }
            Principal principal = principalFactory.getPrincipal(value, tmpType);
            acl.removeEntry(this.privilege, principal);
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

    @Required
    public void setPermissionShortcuts(Map<Privilege, List<String>> permissionShortcuts) {
        this.permissionShortcuts = permissionShortcuts;
    }

}
