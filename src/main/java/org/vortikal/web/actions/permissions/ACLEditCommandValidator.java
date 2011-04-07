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

import java.util.List;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.vortikal.repository.Repository;
import org.vortikal.security.InvalidPrincipalException;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalFactory;
import org.vortikal.security.PrincipalManager;
import org.vortikal.security.Principal.Type;

public class ACLEditCommandValidator implements Validator {

    private PrincipalManager principalManager;
    private PrincipalFactory principalFactory;
    private Repository repository;

    /**
     * @see org.springframework.validation.Validator#supports(java.lang.Class)
     */
    @SuppressWarnings("unchecked")
    public boolean supports(Class clazz) {
        return (clazz == ACLEditCommand.class);
    }
 
    public void validate(Object command, Errors errors) {
        ACLEditCommand editCommand = (ACLEditCommand) command;
        
        // Don't validate on cancel
        if (editCommand.getCancelAction() != null) {
            return;
        }

        if (editCommand.getSaveAction() != null) {
            validateUserNames(editCommand, errors);
            validateGroupNames(editCommand, errors);
        }

        if (editCommand.getAddUserAction() != null) {
            String[] userNames = editCommand.getUserNames();

            if (userNames.length == 0) {
                errors.rejectValue("userNames", "permissions.user.missing.value",
                        "You must type a username");
            }

            validateUserNames(editCommand, errors);

        } else if (editCommand.getAddGroupAction() != null) {
            String[] groupNames = editCommand.getGroupNames();

            if (groupNames.length == 0) {
                errors.rejectValue("groupNames", "permissions.group.missing.value",
                        "You must type a group name");
            }
            validateGroupNames(editCommand, errors);
        }

    }

    private void validateUserNames(ACLEditCommand editCommand, Errors errors) {
        String[] userNames = editCommand.getUserNames();

        if (userNames.length > 0) {
            for (String userName : userNames) {

                userName = userName.trim();
                String uid = userName;

                if (!userName.contains(" ")) {
                    // assume a username and validate it as such
                    if (!validateUserName(userName, errors, editCommand)) {
                        continue;
                    }
                } else {
                    // assume a full name and look for a match in ac_userNames
                    // if match found, validate corresponsding username
                    // if no match found, assume full name entered without
                    // selecting from autocomplete suggestions
                    // i.e. no username provided -> validate as full name
                    try {
                        String ac_userName = getAc_userName(userName, editCommand
                                .getAc_userNames(), editCommand.getUserNameEntries());
                        if (ac_userName != null && !"".equals(ac_userName)) {
                            // Entered name is selected from autocomplete
                            // suggestions and we have username
                            if (!validateUserName(ac_userName, errors, editCommand)) {
                                continue;
                            }
                            uid = ac_userName;
                        } else {
                            List<Principal> matches = this.principalFactory.search(
                                    userName, Type.USER);
                            if (matches == null || matches.size() == 0) {
                                errors.rejectValue("userNames",
                                        "permissions.user.wrong.value",
                                        new Object[] { userName }, "User '" + userName
                                                + "' does not exist");
                                continue;
                            } else if (matches.size() > 1) {
                                errors.rejectValue("userNames",
                                        "permissions.user.too.many.matches",
                                        new Object[] { userName }, userName
                                                + " yielded too many matches.");
                                continue;
                            }
                            uid = matches.get(0).getName();
                        }
                    } catch (Exception e) {
                        errors.rejectValue("userNames", "permissions.exception",
                                        new Object[] { userName }, "Cannot find user "
                                                + userName);
                        continue;
                    }
                }
                editCommand.addUserNameEntry(uid);
            }
        }
    }

    private boolean validateUserName(String userName, Errors errors, ACLEditCommand editCommand) {
        try {
            Principal user = principalFactory.getPrincipal(userName, Principal.Type.USER);

            if (!this.principalManager.validatePrincipal(user)) {
                errors.rejectValue("userNames", "permissions.user.wrong.value", new Object[] { userName },
                     "The user '" + userName + "' does not exist");
                return false;
            }

            if (!repository.isValidAclEntry(editCommand.getPrivilege(), user)) {
                errors.rejectValue("userNames", "permissions.user.invalid.value", new Object[] { userName },
                     "The user '" + userName + "' is not valid");
                return false;
            }

        } catch (InvalidPrincipalException e) {
            errors.rejectValue("userNames", "permissions.user.illegal.value", new Object[] { userName },
                     "The user '" + userName + "' is illegal");
            return false;
        }

        return true;
    }

    private String getAc_userName(String userName, String[] ac_userNames,
            List<String> userNameEntries) {
        for (String ac_userName : ac_userNames) {
            String[] s = ac_userName.split(";");
            String ac_fullName = s[0].trim();
            String ac_uid = s[1].trim();
            if (!userNameEntries.contains(ac_uid) && userName.equals(ac_fullName)) {
                return ac_uid;
            }
        }
        return null;
    }

    private void validateGroupNames(ACLEditCommand editCommand, Errors errors) {
        String[] groupNames = editCommand.getGroupNames();
        String noneExistingGroups = new String();
        String invalidGroups = new String();
        String illegalGroups = new String();
        
        for (String groupName : groupNames) {
            try {
                Principal group = principalFactory.getPrincipal(groupName, Principal.Type.GROUP);
                if (group != null && !this.principalManager.validateGroup(group)) {
                    if (noneExistingGroups.isEmpty()) {
                        noneExistingGroups += groupName;
                    } else {
                        noneExistingGroups += ", " + groupName;
                    }
                } else {
                    if (!repository.isValidAclEntry(editCommand.getPrivilege(), group)) {
                        if (invalidGroups.isEmpty()) {
                            invalidGroups += groupName;
                        } else {
                            invalidGroups += ", " + groupName;
                        }
                    }
                }
            } catch (InvalidPrincipalException e) {
                if (illegalGroups.isEmpty()) {
                    illegalGroups += groupName;
                } else {
                    illegalGroups += ", " + groupName;
                }
            }
        }
        
        if (!noneExistingGroups.isEmpty()) {
            if (!noneExistingGroups.contains(",")) {
                errors.rejectValue("groupNames", "permissions.group.wrong.value", new Object[] { noneExistingGroups },
                        "The group " + noneExistingGroups + " does not exist");
            } else {
                errors.rejectValue("groupNames", "permissions.group.wrong.values", new Object[] { noneExistingGroups },
                        "The groups " + noneExistingGroups + " does not exist");
            }
        }
        if (!invalidGroups.isEmpty()) {
            if (!invalidGroups.contains(",")) {
                errors.rejectValue("groupNames", "permissions.group.invalid.value", new Object[] { invalidGroups },
                        "The group " + invalidGroups + " is not valid");
            } else {
                errors.rejectValue("groupNames", "permissions.group.invalid.values", new Object[] { invalidGroups },
                        "The groups " + invalidGroups + " is not valid");
            }
        }
        if (!illegalGroups.isEmpty()) {
            if (!illegalGroups.contains(",")) {
                errors.rejectValue("groupNames", "permissions.group.illegal.value", new Object[] { illegalGroups },
                        "The group " + illegalGroups + " is illegal");
            } else {
                errors.rejectValue("groupNames", "permissions.group.illegal.values", new Object[] { illegalGroups },
                        "The groups " + illegalGroups + " is illegal");
            }
        }
        
    }

    @Required
    public void setPrincipalManager(PrincipalManager principalManager) {
        this.principalManager = principalManager;
    }

    @Required
    public void setPrincipalFactory(PrincipalFactory principalFactory) {
        this.principalFactory = principalFactory;
    }

    @Required
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

}
