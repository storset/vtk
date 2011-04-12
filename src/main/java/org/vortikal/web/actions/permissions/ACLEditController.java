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
import org.vortikal.security.Principal.Type;
import org.vortikal.security.PrincipalFactory;
import org.vortikal.security.PrincipalManager;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;

public class ACLEditController extends SimpleFormController {

    private Privilege privilege;
    private PrincipalManager principalManager; 
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

    @Override
    protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder)
            throws Exception {
        binder.registerCustomEditor(java.lang.String[].class,
                new StringArrayPropertyEditor());
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
        
        shortcuts = this.permissionShortcuts.get(this.privilege);
        if(shortcuts != null) {
          this.validShortcuts = countValidshortcuts(shortcuts);
        }
        
        return getACLEditCommand(resource, resource.getAcl(), requestContext.getPrincipal());
    }

    private ACLEditCommand getACLEditCommand(Resource resource, Acl acl, Principal principal) throws Exception {
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
        authorizedUsers.addAll(Arrays.asList(acl
                .listPrivilegedPseudoPrincipals(this.privilege)));

        if (shortcuts != null) {   
          command.setShortcuts(extractAndCheckShortcuts(authorizedUsers, authorizedGroups, shortcuts, this.validShortcuts));
        }

        command.setGroups(authorizedGroups);
        command.setUsers(authorizedUsers);

        return command;
    }

    protected boolean isFormSubmission(HttpServletRequest request) {
        return "POST".equals(request.getMethod());
    }

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

    @Override
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
        
        Principal yourself = requestContext.getPrincipal();
        
        // Remove or add shortcuts
        acl = aclShortcuts(acl, editCommand, yourself, errors);
        
        // Has the user asked to save?
        if (editCommand.getSaveAction() != null) {     
            acl = addToAcl(acl, editCommand.getGroupNames(), Type.GROUP);
            acl = addToAcl(acl, editCommand.getUserNameEntries(), Type.USER);
            if(errors.hasErrors()) {
                BindException bex =  new BindException(getACLEditCommand(resource, acl, yourself), this.getCommandName());
                bex.addAllErrors(errors);
                return showForm(request, response, errors);  
            }
            resource = repository.storeACL(token, resource.getURI(), acl);
            return new ModelAndView(getSuccessView()); 
        }

        // Doing remove or add actions
        if (editCommand.getRemoveGroupAction() != null) {
            acl = removeFromAcl(acl, editCommand.getGroupNames(), Type.GROUP, yourself, errors);
            BindException bex =  new BindException(getACLEditCommand(resource, acl, yourself), this.getCommandName());
            bex.addAllErrors(errors);
            return showForm(request, response, bex);

        } else if (editCommand.getRemoveUserAction() != null) {
            acl = removeFromAcl(acl, editCommand.getUserNames(), Type.USER, yourself, errors);
            BindException bex =  new BindException(getACLEditCommand(resource, acl, yourself), this.getCommandName());
            bex.addAllErrors(errors);
            return showForm(request, response, bex);

        } else if (editCommand.getAddGroupAction() != null) {
            acl = addToAcl(acl, editCommand.getGroupNames(), Type.GROUP);
            return showForm(request, response, new BindException(getACLEditCommand(resource, acl, yourself), this.getCommandName()));
     
        } else if (editCommand.getAddUserAction() != null) {
            acl = addToAcl(acl, editCommand.getUserNameEntries(), Type.USER);
            return showForm(request, response, new BindException(getACLEditCommand(resource, acl, yourself), this.getCommandName()));
        }
        
        return new ModelAndView(getSuccessView());
    }
    
    /**
     * Count valid shortcuts (with GROUP or USER prefix)
     * 
     * @param theShortcuts the shortcuts
     * @return number of valid shortcuts
     */
    private int countValidshortcuts(List<String> theShortcuts) {
        int valid = 0;
          for (String shortcut: theShortcuts) {
            if (shortcut.startsWith(GROUP_PREFIX) || shortcut.startsWith(USER_PREFIX)) {
              valid++;
            }
          }
          return valid;
    }
    
    /**
     * Extracts shortcuts for users and groups (if exist set to 'checked' and remove).
     * 
     * @param authorizedUsers the authorized users
     * @param authorizedGroups the authorized groups
     * @param shortcuts the configured shortcuts
     * @param pre-counted valid shortcuts
     * @return a <code>String[][]</code> object containing checked / not-checked shortcuts
     */
    private String[][] extractAndCheckShortcuts (List<Principal> authorizedUsers, 
            List<Principal> authorizedGroups, List<String> shortcuts, int validShortcuts) {

        String checkedShortcuts[][] = new String[validShortcuts][2];
        
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
     * @param editCommand the command object
     * @param yourself
     * @param errors ACL validation errors
     * @return the modified ACL
     */
    private Acl aclShortcuts(Acl acl, ACLEditCommand editCommand, Principal yourself, BindException errors) {
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
                acl = removeFromAcl(acl, groupOrUserShortcut, type, yourself, errors);
            }

            // Add
            if (uncheckedFound) {
                String[] groupOrUserShortcut = new String[1];
                Type type = unformatShortcutAndSetType(shortcut, groupOrUserShortcut);
                acl = addToAcl(acl, groupOrUserShortcut, type);
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
                if (userOrGroup.equals(yourself) || (!acl.containsEntry(this.privilege, yourself) && type.equals(Type.GROUP))) {
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
     * Check if yourself is still in privileged groups for admin after removal
     * 
     * @param acl the ACL object
     * @param potentialAcl the potential ACL object
     * @param userOrGroup the user or group
     * @param yourself
     * @param errors ACL validation errors
     * @return the modified ACL
     */
    private Acl checkIfUserIsInAdminPrivilegedGroups(Acl acl, Acl potentialAcl, Principal userOrGroup, Principal yourself, BindException errors) { 
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
            acl = acl.addEntry(this.privilege, principal);
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
            acl = acl.addEntry(this.privilege, principal);
        }
        return acl;
    }
    
    /**
     * Unformat shortcut and set type to GROUP or USER
     * 
     * @param shortcut formatted shortcut
     * @param groupOrUserShortcut unformatted group or user shortcut (return by reference)
     * @return type of ACL (GROUP or USER)
     */
    private Type unformatShortcutAndSetType(String[] shortcut, String[] groupOrUserShortcut) {
        Type type = null;
        if (shortcut[0].startsWith(GROUP_PREFIX)) {
            groupOrUserShortcut[0] = shortcut[0].substring(GROUP_PREFIX.length());
            type = Type.GROUP;
        } else if (shortcut[0].startsWith(USER_PREFIX)) {
            groupOrUserShortcut[0] = shortcut[0].substring(USER_PREFIX.length());
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

}
