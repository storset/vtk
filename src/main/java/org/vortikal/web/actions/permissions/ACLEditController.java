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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

public class ACLEditController extends SimpleFormController {

    private Privilege privilege;
    private PrincipalFactory principalFactory;
    private Map<Privilege, List<String>> permissionShortcuts;
    
    private List<String> shortcuts;
    private int validShortcuts = 0;
    
    private static final String GROUP_PREFIX = "group:";
    private static final String USER_PREFIX = "user:";
    private static final String PSEUDO_PREFIX = "pseudo:";

    public ACLEditController() {
        setSessionForm(true);
    }

    protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder)
            throws Exception {
        binder.registerCustomEditor(java.lang.String[].class,
                new StringArrayPropertyEditor());
    }
    
    /**
     * Override to handle removal of permissions.
     */
    @Override
    protected ServletRequestDataBinder createBinder(HttpServletRequest request, Object command) throws Exception {
        ACLEditBinder binder = new ACLEditBinder(command, getCommandName());
        prepareBinder(binder);
        initBinder(request, binder);
        return binder; 
    }

    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        Path uri = requestContext.getResourceURI();
        Repository repository = requestContext.getRepository();
        String token = requestContext.getSecurityToken();
        Resource resource = repository.retrieve(token, uri, false);
        
        shortcuts = this.permissionShortcuts.get(this.privilege);
        if(shortcuts != null) {
          int valid = 0;
          for (String shortcut: shortcuts) {
            if (shortcut.startsWith(GROUP_PREFIX) || shortcut.startsWith(USER_PREFIX)) {
              valid++;
            }
          }
          this.validShortcuts = valid;
        }
        
        return getACLEditCommand(resource, resource.getAcl(), requestContext.getPrincipal());
    }

    private ACLEditCommand getACLEditCommand(Resource resource, Acl acl, Principal principal) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        Service service = requestContext.getService();

        String submitURL = service.constructLink(resource, principal);
        ACLEditCommand command = new ACLEditCommand(submitURL);

        command.setAcl(acl);

        List<Principal> authorizedGroups = new ArrayList<Principal>(Arrays.asList(acl
                .listPrivilegedGroups(this.privilege)));
        List<Principal> authorizedUsers = new ArrayList<Principal>(Arrays.asList(acl
                .listPrivilegedUsers(this.privilege)));
        authorizedUsers.addAll(Arrays.asList(acl
                .listPrivilegedPseudoPrincipals(this.privilege)));

        if (shortcuts != null) {   
          command.setShortcuts(extractAndCheckShortcuts(authorizedUsers, authorizedGroups, shortcuts));
        }

        command.setGroups(authorizedGroups);
        command.setUsers(authorizedUsers);

        return command;
    }

    protected boolean isFormSubmission(HttpServletRequest request) {
        return "POST".equals(request.getMethod());
    }

    /**
     * Override to reset actions in case of errors.
     */
    @Override
    protected ModelAndView processFormSubmission(HttpServletRequest req,
            HttpServletResponse resp, Object command, BindException errors)
            throws Exception {
        if (errors.hasErrors()) {
            ACLEditCommand editCommand = (ACLEditCommand) command;
            editCommand.setAddGroupAction(null);
            editCommand.setAddUserAction(null);
            editCommand.setRemoveGroupAction(null);
            editCommand.setRemoveUserAction(null);
            editCommand.setSaveAction(null);
            editCommand.getUserNameEntries().removeAll(editCommand.getUserNameEntries());
        }
        return super.processFormSubmission(req, resp, command, errors);
    }

    protected ModelAndView onSubmit(HttpServletRequest request,
            HttpServletResponse response, Object command, BindException errors)
            throws Exception {

        ACLEditCommand editCommand = (ACLEditCommand) command;

        Acl acl = editCommand.getAcl();

        RequestContext requestContext = RequestContext.getRequestContext();
        Repository repository = requestContext.getRepository();
        String token = requestContext.getSecurityToken();
        Path uri = requestContext.getResourceURI();

        Resource resource = repository.retrieve(token, uri, false);
        
        // Did the user cancel?
        if (editCommand.getCancelAction() != null) {
            return new ModelAndView(getSuccessView());
        }
        
        // Remove or add shortcuts
        acl = aclShortcuts(acl, repository, errors, editCommand);
        
        // Has the user asked to save?
        if (editCommand.getSaveAction() != null) {     
            acl = addToAcl(acl, repository, errors, editCommand.getGroupNames(), Type.GROUP);
            acl = addToAcl(acl, repository, errors, editCommand.getUserNameEntries(), Type.USER);
            
            if (acl.isEmpty()) {
                errors.rejectValue("groupNames", "permissions.no.acl", new Object[] {}, "Resource can not be without permissions");
                BindException bex = new BindException(getACLEditCommand(resource, acl, requestContext.getPrincipal()), this.getCommandName());
                bex.addAllErrors(errors); // Add error when empty ACL
                return showForm(request, response, bex); 
            }
            
            if (errors.hasErrors()) {
                BindException bex = new BindException(getACLEditCommand(resource, acl, requestContext.getPrincipal()), this.getCommandName());
                bex.addAllErrors(errors); // Add validation errors
                return showForm(request, response, bex);  
            }
            
            resource = repository.storeACL(token, resource.getURI(), acl);
            return new ModelAndView(getSuccessView()); 
        }

        // Doing remove or add actions
        if (editCommand.getRemoveGroupAction() != null) {
            acl = removeFromAcl(acl, editCommand.getGroupNames(), Type.GROUP);
            return showForm(request, response, new BindException(getACLEditCommand(resource, acl, requestContext.getPrincipal()), this.getCommandName()));

        } else if (editCommand.getRemoveUserAction() != null) {
            acl = removeFromAcl(acl, editCommand.getUserNames(), Type.USER);
            return showForm(request, response, new BindException(getACLEditCommand(resource, acl, requestContext.getPrincipal()), this.getCommandName()));

        } else if (editCommand.getAddGroupAction() != null) {
            acl = addToAcl(acl, repository, errors, editCommand.getGroupNames(), Type.GROUP);
            BindException bex = new BindException(getACLEditCommand(resource, acl, requestContext.getPrincipal()), this.getCommandName());
            bex.addAllErrors(errors); // Add validation errors
            return showForm(request, response, bex);
     
        } else if (editCommand.getAddUserAction() != null) {
            acl = addToAcl(acl, repository, errors, editCommand.getUserNameEntries(), Type.USER);
            BindException bex = new BindException(getACLEditCommand(resource, acl, requestContext.getPrincipal()), this.getCommandName());
            bex.addAllErrors(errors); // Add validation errors
            return showForm(request, response, bex);
        }
        
        return new ModelAndView(getSuccessView());
    }
    
    /**
     * Extracts shortcuts for users and groups (set to 'checked' and remove).
     * 
     * @param authorizedUsers
     *            the authorized users
     * @param authorizedGroups
     *            the authorized groups
     * @param shortcuts
     *            the configured shortcuts
     * @return a <code>String[][]</code> object containing checked / not-checked shortcuts
     */
    protected String[][] extractAndCheckShortcuts (List<Principal> authorizedUsers, 
            List<Principal> authorizedGroups, List<String> shortcuts) {

        String checkedShortcuts[][] = new String[this.validShortcuts][2];
        
        int i = 0;
        
        for (String shortcut: shortcuts) {
            boolean checked = false;
            boolean validShortcut = false;
            
            if (shortcut.startsWith(GROUP_PREFIX)) { // Check if shortcut is in authorizedGroups
                Iterator<Principal> it = authorizedGroups.iterator();
                while (it.hasNext()) {
                    String userName = it.next().getName();
                    if ((GROUP_PREFIX + userName).equals(shortcut)) {
                        checked = true;
                        it.remove();
                    }
                } 
                validShortcut = true;
            } else if (shortcut.startsWith(USER_PREFIX)) { // Check if shortcut is in authorizedUsers
                Iterator<Principal> it = authorizedUsers.iterator();
                while (it.hasNext()) {
                    String groupName = it.next().getName();
                    if ((USER_PREFIX + groupName).equals(shortcut)) {
                        checked = true;
                        it.remove();
                    }
                }
                validShortcut = true;
            }
            
            if (validShortcut) {
              checkedShortcuts[i][0] = shortcut;
              checkedShortcuts[i][1] = checked ? "checked" : "";
              i++;
            }
        }
        
        return checkedShortcuts;
    }

    /**
     * Add and remove ACL entries for updated shortcuts.
     * 
     * @param acl the ACL object
     * @param repository the repository object
     * @param errors ACL validation errors
     * @param editCommand the command object
     * @return the modified ACL
     */
    private Acl aclShortcuts(Acl acl, Repository repository, BindException errors, ACLEditCommand editCommand) {
        String[] updatedShortcuts = editCommand.getUpdatedShortcuts();
        String[][] shortcuts = editCommand.getShortcuts();

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
                String[] groupOrUserShortcut = new String[1];
                Type type = unformatShortcutAndSetType(shortcut, groupOrUserShortcut);
                acl = removeFromAcl(acl, groupOrUserShortcut, type);
            }

            // Add
            if (uncheckedFound) {
                String[] groupOrUserShortcut = new String[1];
                Type type = unformatShortcutAndSetType(shortcut, groupOrUserShortcut);
                acl = addToAcl(acl, repository, errors, groupOrUserShortcut, type);
            }
        }
        return acl;
    }

    /**
     * Remove groups or users from ACL.
     * 
     * @param acl the ACL object
     * @param values groups or users to remove
     * @param type type of ACL (GROUP or USER)
     * @return the modified ACL
     */
    private Acl removeFromAcl(Acl acl, String[] values, Type type) {
        for (String value : values) {
            Principal principal = principalFactory.getPrincipal(value, typePseudoUser(type, value));
            acl = acl.removeEntry(this.privilege, principal);
        }
        return acl;
    }
    
    /**
     * Add groups or users to ACL.
     * 
     * @param acl the ACL object         
     * @param repository the repository object
     * @param errors ACL validation errors
     * @param values groups or users to remove
     * @param type type of ACL (GROUP or USER)
     * @return the modified ACL
     */
    private Acl addToAcl(Acl acl, Repository repository, BindException errors, String[] values, Type type) {
        for (String value : values) {
            Principal principal = principalFactory.getPrincipal(value, typePseudoUser(type, value));
            if (repository.isValidAclEntry(this.privilege, principal)) {
              acl = acl.addEntry(this.privilege, principal);
            } else {
              if (type == Type.GROUP) {
                errors.rejectValue("groupNames", "permissions.group.invalid.value", new Object[] { value }, "The group '" + value + "' is not valid");                 
              } else {
                errors.rejectValue("userNames", "permissions.user.invalid.value", new Object[] { value }, "The user '" + value + "' is not valid");
              }
            }
        }
        return acl;
    }
    
    /**
     * Add groups or users to ACL (for getUserNameEntries()).
     * 
     * @param acl the ACL object         
     * @param repository the repository object
     * @param errors ACL validation errors
     * @param values groups or users to remove
     * @param type type of ACL (GROUP or USER)
     * @return the modified ACL
     */
    private Acl addToAcl(Acl acl, Repository repository, BindException errors, List<String> values, Type type) {
        for (String value : values) {
            Principal principal = principalFactory.getPrincipal(value, typePseudoUser(type, value));
            if (repository.isValidAclEntry(this.privilege, principal)) {
              acl = acl.addEntry(this.privilege, principal);
            } else {
              if (type == Type.GROUP) {
                errors.rejectValue("groupNames", "permissions.group.invalid.value", new Object[] { value }, "Group '" + value + "' is not valid");                 
              } else {
                errors.rejectValue("userNames", "permissions.user.invalid.value", new Object[] { value }, "User '" + value + "' is not valid");
              }
            }
        }
        return acl;
    }
    
    /**
     * Unformat shortcut and set type to GROUP or USER
     * 
     * @param shortcut the shortcut (formatted)
     * @param groupOrUserShortcut (unformatted)
     *        group or user
     * @return type of ACL (GROUP or USER)
     */
    private Type unformatShortcutAndSetType(String[] shortcut, String[] groupOrUserShortcut) {
        Type type = null;
        if (shortcut[0].startsWith(GROUP_PREFIX)) {
            groupOrUserShortcut[0] = shortcut[0].replace(GROUP_PREFIX, "");
            type = Type.GROUP;
        } else if (shortcut[0].startsWith(USER_PREFIX)) {
            groupOrUserShortcut[0] = shortcut[0].replace(USER_PREFIX, "");
            type = Type.USER;     
        }
        return type;
    }
    
    /**
     * Check if USER is PSEUDO and set correct type
     * 
     * @param type the type
     * @param value group or user
     * @return type type of ACL (GROUP or USER or PSEUDO)
     */
    private Type typePseudoUser(Type type, String value) { 
        if (type.equals(Type.USER)) {
          if (value.startsWith(PSEUDO_PREFIX)) {
              type = Type.PSEUDO;  
          }
        }
        return type;
    }

    @Required
    public void setPrincipalFactory(PrincipalFactory principalFactory) {
        this.principalFactory = principalFactory;
    }

    @Required
    public void setPermissionShortcuts(Map<Privilege, List<String>> permissionShortcuts) {
        this.permissionShortcuts = permissionShortcuts;
    }

    @Required
    public void setPrivilege(Privilege privilege) {
        this.privilege = privilege;
    }

}
