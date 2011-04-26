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
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.vortikal.security.PrincipalFactory;
import org.vortikal.security.PrincipalManager;
import org.vortikal.security.Principal.Type;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;

public class ACLEditController extends SimpleFormController {

    private Privilege privilege;
    private PrincipalManager principalManager;
    private PrincipalFactory principalFactory;

    private Map<Privilege, List<String>> permissionShortcuts;
    private Map<String, List<String>> permissionShortcutsConfig;
    
    private List<String> shortcuts;
    private int validShortcuts = 0;
    
    private static final String GROUP_PREFIX = "group:";
    private static final String USER_PREFIX = "user:";
    private static final String PSEUDO_PREFIX = "pseudo:";


    public ACLEditController() {
        setSessionForm(true);
    }


    @Override
    protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) throws Exception {
        binder.registerCustomEditor(java.lang.String[].class, new StringArrayPropertyEditor());
    }


    @Override
    protected ServletRequestDataBinder createBinder(HttpServletRequest request, Object command) throws Exception {
        ACLEditBinder binder = new ACLEditBinder(command, getCommandName());
        prepareBinder(binder);
        initBinder(request, binder);
        return binder;
    }


    @Override
    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        Path uri = requestContext.getResourceURI();
        Repository repository = requestContext.getRepository();
        String token = requestContext.getSecurityToken();
        Resource resource = repository.retrieve(token, uri, false);

        this.shortcuts = this.permissionShortcuts.get(this.privilege);
        if (this.shortcuts != null) {
           this.validShortcuts = countValidshortcuts(this.shortcuts, this.permissionShortcutsConfig);
        }

        return getACLEditCommand(resource, resource.getAcl(), requestContext.getPrincipal(), false);
    }


    private ACLEditCommand getACLEditCommand(Resource resource, Acl acl, Principal principal, boolean isCustomPermissions) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        Service service = requestContext.getService();

        String submitURL = service.constructLink(resource, principal);
        ACLEditCommand command = new ACLEditCommand(submitURL);

        command.setAcl(acl);
        command.setPrivilege(this.privilege);

        List<Principal> authorizedGroups = new ArrayList<Principal>(Arrays.asList(acl
                .listPrivilegedGroups(this.privilege)));
        List<Principal> authorizedUsers = new ArrayList<Principal>(Arrays.asList(acl
                .listPrivilegedUsers(this.privilege)));
        authorizedUsers.addAll(Arrays.asList(acl.listPrivilegedPseudoPrincipals(this.privilege)));

        if (this.shortcuts != null) {
            command.setShortcuts(extractAndCheckShortcuts(authorizedGroups, authorizedUsers, this.validShortcuts,
                    this.shortcuts, this.permissionShortcutsConfig, isCustomPermissions));
        }

        command.setGroups(authorizedGroups);
        command.setUsers(authorizedUsers);

        return command;
    }


    protected boolean isFormSubmission(HttpServletRequest request) {
        return "POST".equals(request.getMethod());
    }


    @Override
    protected ModelAndView processFormSubmission(HttpServletRequest req, HttpServletResponse resp, Object command,
            BindException errors) throws Exception {
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


    @Override
    protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors) throws Exception {

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

        Principal yourself = requestContext.getPrincipal();
        
        // Has the user asked to save?
        if (editCommand.getSaveAction() != null) {
            acl = addToAcl(acl, editCommand.getGroupNames(), Type.GROUP);
            acl = addToAcl(acl, editCommand.getUserNameEntries(), Type.USER);
            acl = updateAclIfShortcut(acl, editCommand, yourself, errors);
            if (errors.hasErrors()) {
                BindException bex = new BindException(getACLEditCommand(resource, acl, yourself, true), this.getCommandName());
                bex.addAllErrors(errors);
                return showForm(request, response, errors);
            }
            resource = repository.storeACL(token, resource.getURI(), acl);
            return new ModelAndView(getSuccessView());
        }

        // Doing remove or add actions
        if (editCommand.getRemoveGroupAction() != null) {
            acl = removeFromAcl(acl, editCommand.getGroupNames(), Type.GROUP, yourself, errors);
            BindException bex = new BindException(getACLEditCommand(resource, acl, yourself, true), this.getCommandName());
            bex.addAllErrors(errors);
            return showForm(request, response, bex);

        } else if (editCommand.getRemoveUserAction() != null) {
            acl = removeFromAcl(acl, editCommand.getUserNames(), Type.USER, yourself, errors);
            BindException bex = new BindException(getACLEditCommand(resource, acl, yourself, true), this.getCommandName());
            bex.addAllErrors(errors);
            return showForm(request, response, bex);

        } else if (editCommand.getAddGroupAction() != null) {
            acl = addToAcl(acl, editCommand.getGroupNames(), Type.GROUP);
            return showForm(request, response, new BindException(getACLEditCommand(resource, acl, yourself, true), this
                    .getCommandName()));

        } else if (editCommand.getAddUserAction() != null) {
            acl = addToAcl(acl, editCommand.getUserNameEntries(), Type.USER);
            return showForm(request, response, new BindException(getACLEditCommand(resource, acl, yourself, true), this
                    .getCommandName()));
        }

        return new ModelAndView(getSuccessView());
    }
    
    /**
     * Count valid shortcuts (all users and groups should have GROUP or USER prefix)
     *
     * @param shortcuts the configured shortcuts for the privilege
     * @param permissionShortcutsConfig the users and groups for the shortcuts
     * @return number of valid shortcuts
     */
    protected int countValidshortcuts(List<String> shortcuts, Map<String, List<String>> permissionShortcutsConfig) {
        int valid = 0;
        for (String shortcut : shortcuts) {
            int validGroupsUsers = 0;
            List<String> groupsUsersPrShortcut = permissionShortcutsConfig.get(shortcut);
            for (String groupOrUser : groupsUsersPrShortcut) {
                if (groupOrUser.startsWith(GROUP_PREFIX) || groupOrUser.startsWith(USER_PREFIX)) {
                    validGroupsUsers++;
                }
            }
            if (groupsUsersPrShortcut.size() == validGroupsUsers) {
                valid++;
            }
        }
        return valid;
    }
    
    
    /**
     * Extracts shortcuts from authorized users and groups
     * 
     * TODO: ponder out some smarter shorter code
     *
     * @param authorizedUsers the authorized users
     * @param authorizedGroups the authorized groups
     * @param precounted valid shortcuts
     * @param shortcuts the configured shortcuts for the privilege
     * @param permissionShortcutsConfig the users and groups for the shortcuts
     * @return a <code>String[][]</code> object containing checked / not-checked shortcuts
     */
    protected String[][] extractAndCheckShortcuts(List<Principal> authorizedGroups, List<Principal> authorizedUsers,
            int validShortcuts, List<String> shortcuts, Map<String, List<String>> permissionShortcutsConfig, boolean isCustomPermissions) {
        
         String checkedShortcuts[][] = new String[validShortcuts][2];

        // Iterate shortcuts on privilege
        int i = 0;
        for (String shortcut : shortcuts) {
            List<String> shortcutACEs = permissionShortcutsConfig.get(shortcut);
            int numberOfShortcutACEs = shortcutACEs.size();
            int matchedACEs = 0;
            int totalACEs = authorizedGroups.size() + authorizedUsers.size();

            // Find matches in shortcut ACEs
            for (String aceWithPrefix : shortcutACEs) {
                for (Principal group : authorizedGroups) {
                    if ((GROUP_PREFIX + group.getName()).equals(aceWithPrefix)) {
                        matchedACEs++;
                    }
                }
                for (Principal user : authorizedUsers) {
                    if ((USER_PREFIX + user.getName()).equals(aceWithPrefix)) {
                        matchedACEs++;
                    }
                }
            }
            
            checkedShortcuts[i][0] = shortcut;
            
            // If not custom is choosen
            if(!isCustomPermissions) {
                // If matches are exactly the number of authorized groups / users and size of shortcut
                if (matchedACEs == totalACEs && matchedACEs == numberOfShortcutACEs) {
                    checkedShortcuts[i][1] = "checked";
                    // Remove ACEs for shortcut on view
                    authorizedUsers.clear();
                    authorizedGroups.clear();
                } else {
                    checkedShortcuts[i][1] = "";
                }
            } else {
                checkedShortcuts[i][1] = "";
            }
            i++;
        }

        return checkedShortcuts;
    }


    /**
     * Add and remove ACL entries for updated shortcut
     *
     * @param acl the ACL object
     * @param editCommand the command object
     * @param yourself
     * @param errors ACL validation errors
     * @return the modified ACL
     */
    private Acl updateAclIfShortcut(Acl acl, ACLEditCommand editCommand, Principal yourself, BindException errors) {
        
        String updatedShortcut = editCommand.getUpdatedShortcut();

        if (this.permissionShortcutsConfig.get(updatedShortcut) != null) {

            // First: remove all ACEs on privilege
            acl = acl.emptyPrivilige(this.privilege);
            
            // Then: add ACEs from updated shortcut
            List<String> shortcutACEs = this.permissionShortcutsConfig.get(updatedShortcut);
            for (String aceWithPrefix : shortcutACEs) {
                String groupOrUserUnformatted[] = new String[1];
                Type type = unformatGroupOrUserAndSetType(aceWithPrefix, groupOrUserUnformatted);
                acl = addToAcl(acl, groupOrUserUnformatted, type);
            }
        } else {
          // If not a shortcut and no groups / users in admin then remove groups / users (typical when comoing from shortcuts)
          if (editCommand.getGroups().size() == 0 && editCommand.getUsers().size() == 0) {
            acl = acl.emptyPrivilige(this.privilege); 
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
     * @param yourself
     * @param errors ACL validation errors
     * @return the modified ACL
     */
    private Acl removeFromAcl(Acl acl, String[] values, Type type, Principal yourself, BindException errors) {
        for (String value : values) {
            Principal userOrGroup = principalFactory.getPrincipal(value, typePseudoUser(type, value));
            Acl potentialAcl = acl.removeEntry(this.privilege, userOrGroup);
            if (this.privilege.equals(Privilege.ALL)) {
                if (userOrGroup.equals(yourself)
                        || (!acl.containsEntry(this.privilege, yourself) && type.equals(Type.GROUP))) {
                    // Trying to remove yourself or group (with yourself not as admin)
                    acl = checkIfUserIsInAdminPrivilegedGroups(acl, potentialAcl, userOrGroup, yourself, errors);
                } else {
                    // Trying to remove user (not yourself) or group (with yourself still as admin)
                    acl = checkIfNotEmptyAdminAcl(acl, potentialAcl, userOrGroup, errors);
                }
            } else {
                acl = potentialAcl;
            }
        }
        return acl;
    }
    
    /**
     * Remove groups or users from ACL.
     *
     * @param acl the ACL object
     * @param values groups or users to remove
     * @return the modified ACL
     */
    private Acl removeFromAcl(Acl acl, List<Principal> values) {
        for (Principal principal : values) {
            acl = acl.removeEntry(this.privilege, principal);
        }
        return acl;
    }


    /**
     * Check if yourself is still in privileged groups for admin after removal
     *
     * @param acl the ACL object
     * @param potentialAcl the potential ACL object
     * @param userOrGroup the user or group
     * @param yourself
     * @param errors ACL validation errors
     * @return the modified ACL
     */
    private Acl checkIfUserIsInAdminPrivilegedGroups(Acl acl, Acl potentialAcl, Principal userOrGroup,
            Principal yourself, BindException errors) {
        Set<Principal> groups = principalManager.getMemberGroups(yourself);
        Principal[] privilegedGroups = potentialAcl.listPrivilegedGroups(Privilege.ALL);
        boolean stillAdmin = false;
        if (privilegedGroups.length > 0) {
            int i = 0;
            for (Principal group : groups) {
                if (privilegedGroups[i] == group) {
                    stillAdmin = true;
                    break;
                }
                i++;
            }
        }
        if (!stillAdmin) {

            String prefixType = (userOrGroup.getType().equals(Type.GROUP)) ? "group" : "user";
            errors.rejectValue(prefixType + "Names", "permissions.all.yourself.not.empty",
                    "Not possible to remove all admin permissions for yourself");
            return acl;
        } else {
            return checkIfNotEmptyAdminAcl(acl, potentialAcl, userOrGroup, errors);
        }
    }

    /**
     * Check if not empty admin Acl
     *
     * @param acl the ACL object
     * @param potentialAcl the potential ACL object
     * @param userOrGroup the user or group
     * @param errors ACL validation errors
     * @return the modified ACL
     */
    private Acl checkIfNotEmptyAdminAcl(Acl acl, Acl potentialAcl, Principal userOrGroup, BindException errors) {
        if (potentialAcl.getPrincipalSet(Privilege.ALL).size() == 0) {
            String prefixType = (userOrGroup.getType().equals(Type.GROUP)) ? "group" : "user";
            errors.rejectValue(prefixType + "Names", "permissions.all.not.empty",
                    "Not possible to remove all admin permissions");
            return acl;
        } else {
            return potentialAcl;
        }
    }


    /**
     * Add groups or users to ACL.
     *
     * @param acl the ACL object
     * @param values groups or users to remove
     * @param type type of ACL (GROUP or USER)
     * @return the modified ACL
     */
    private Acl addToAcl(Acl acl, String[] values, Type type) {
        for (String value : values) {
            Principal principal = principalFactory.getPrincipal(value, typePseudoUser(type, value));
            if (!acl.containsEntry(this.privilege, principal)) {
                acl = acl.addEntry(this.privilege, principal);
            }
        }
        return acl;
    }


    /**
     * Add groups or users to ACL (for getUserNameEntries()).
     *
     * @param acl the ACL object
     * @param values groups or users to remove
     * @param type type of ACL (GROUP or USER)
     * @return the modified ACL
     */
    private Acl addToAcl(Acl acl, List<String> values, Type type) {
        for (String value : values) {
            Principal principal = principalFactory.getPrincipal(value, typePseudoUser(type, value));
            if (!acl.containsEntry(this.privilege, principal)) {
                acl = acl.addEntry(this.privilege, principal);
            }
        }
        return acl;
    }


    /**
     * Unformat group or user in shortcut and set type to GROUP or USER
     *
     * @param groupOrUser formatted shortcut
     * @param groupOrUserShortcut unformatted shortcut (return by reference)
     * @return type of ACL (GROUP or USER)
     */
    private Type unformatGroupOrUserAndSetType(String groupOrUser, String[] groupOrUserUnformatted) {
        Type type = null;
        if (groupOrUser.startsWith(GROUP_PREFIX)) {
            groupOrUserUnformatted[0] = groupOrUser.substring(GROUP_PREFIX.length());
            type = Type.GROUP;
        } else if (groupOrUser.startsWith(USER_PREFIX)) {
            groupOrUserUnformatted[0] = groupOrUser.substring(USER_PREFIX.length());
            type = Type.USER;
        }
        return type;
    }


    /**
     * Check if USER is PSEUDO and set correct type
     *
     * @param type type of ACL (GROUP or USER)
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
    public void setPrivilege(Privilege privilege) {
        this.privilege = privilege;
    }


    @Required
    public void setPrincipalFactory(PrincipalFactory principalFactory) {
        this.principalFactory = principalFactory;
    }


    @Required
    public void setPrincipalManager(PrincipalManager principalManager) {
        this.principalManager = principalManager;
    }


    @Required
    public void setPermissionShortcuts(Map<Privilege, List<String>> permissionShortcuts) {
        this.permissionShortcuts = permissionShortcuts;
    }


    @Required
    public void setPermissionShortcutsConfig(Map<String, List<String>> permissionShortcutsConfig) {
        this.permissionShortcutsConfig = permissionShortcutsConfig;
    }

}
