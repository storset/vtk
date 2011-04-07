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

    private static final String VALIDATION_ERROR_NONE_EXISTING = "none.existing";
    private static final String VALIDATION_ERROR_INVALID_BLACKLISTED = "invalid.blacklisted";
    private static final String VALIDATION_ERROR_INVALID = "invalid";
    private static final String VALIDATION_ERROR_TOO_MANY_MATCHES = "too.many.matches";
    private static final String VALIDATION_OK = "ok";


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

        if (editCommand.getAddGroupAction() != null) {
            String[] groupNames = editCommand.getGroupNames();

            if (groupNames.length == 0) {
                errors.rejectValue("groupNames", "permissions.group.missing.value", "You must type a group name");
            }
            validateGroupNames(editCommand, errors);

        } else if (editCommand.getAddUserAction() != null) {
            String[] userNames = editCommand.getUserNames();

            if (userNames.length == 0) {
                errors.rejectValue("userNames", "permissions.user.missing.value", "You must type a username");
            }
            validateUserNames(editCommand, errors);
        }

    }


    private void validateGroupNames(ACLEditCommand editCommand, Errors errors) {
        String[] groupNames = editCommand.getGroupNames();

        if (groupNames.length > 0) {

            String noneExistingGroups = new String();
            String invalidBlackListedGroups = new String();
            String invalidGroups = new String();

            for (String groupName : groupNames) {

                String validation = validateGroupOrUserName(Type.GROUP, groupName, editCommand);

                if (validation.equals(VALIDATION_ERROR_NONE_EXISTING)) {
                    noneExistingGroups += noneExistingGroups.isEmpty() ? groupName : ", " + groupName;
                } else if (validation.equals(VALIDATION_ERROR_INVALID_BLACKLISTED)) {
                    invalidBlackListedGroups += invalidBlackListedGroups.isEmpty() ? groupName : ", " + groupName;
                } else if (validation.equals(VALIDATION_ERROR_INVALID)) {
                    invalidGroups += invalidGroups.isEmpty() ? groupName : ", " + groupName;
                }
            }

            rejectValues("group", noneExistingGroups, VALIDATION_ERROR_NONE_EXISTING, errors);
            rejectValues("group", invalidBlackListedGroups, VALIDATION_ERROR_INVALID_BLACKLISTED, errors);
            rejectValues("group", invalidGroups, VALIDATION_ERROR_INVALID, errors);
        }

    }


    private void validateUserNames(ACLEditCommand editCommand, Errors errors) {
        String[] userNames = editCommand.getUserNames();

        if (userNames.length > 0) {

            String noneExistingUsers = new String();
            String invalidBlacklistedUsers = new String();
            String invalidUsers = new String();
            String tooManyMatchedUsers = new String();

            for (String userName : userNames) {

                userName = userName.trim();
                String uid = userName;

                // Assume a username and validate it as such
                if (!userName.contains(" ")) {    
                    String validation = validateGroupOrUserName(Type.USER, userName, editCommand);
                    if (validation.equals(VALIDATION_ERROR_NONE_EXISTING)) {
                        noneExistingUsers += noneExistingUsers.isEmpty() ? userName : ", " + userName;
                    } else if (validation.equals(VALIDATION_ERROR_INVALID_BLACKLISTED)) {
                        invalidBlacklistedUsers += invalidBlacklistedUsers.isEmpty() ? userName : ", " + userName;
                    } else if (validation.equals(VALIDATION_ERROR_INVALID)) {
                        invalidUsers += invalidUsers.isEmpty() ? userName : ", " + userName;
                    }
                    if (!VALIDATION_OK.equals(validation)) {
                        continue;
                    }
                } else {
                    // Assume a full name and look for a match in ac_userNames
                    // If match found: validate corresponding username
                    // If no match found: assume full name entered without
                    // selecting from autocomplete suggestions
                    // i.e. no username provided -> validate as full name
                    try {
                        String ac_userName = getAc_userName(userName, editCommand.getAc_userNames(), editCommand
                                .getUserNameEntries());
                        if (ac_userName != null && !"".equals(ac_userName)) {
                            // Entered name is selected from autocomplete
                            // suggestions and we have username
                            String validation = validateGroupOrUserName(Type.USER, userName, editCommand);
                            if (validation.equals(VALIDATION_ERROR_NONE_EXISTING)) {
                                noneExistingUsers += noneExistingUsers.isEmpty() ? userName : ", " + userName;
                            } else if (validation.equals(VALIDATION_ERROR_INVALID_BLACKLISTED)) {
                                invalidBlacklistedUsers += invalidBlacklistedUsers.isEmpty() ? userName : ", "
                                        + userName;
                            } else if (validation.equals(VALIDATION_ERROR_INVALID)) {
                                invalidUsers += invalidUsers.isEmpty() ? userName : ", " + userName;
                            }
                            if (!VALIDATION_OK.equals(validation)) {
                                continue;
                            }
                            uid = ac_userName;
                        } else {
                            List<Principal> matches = this.principalFactory.search(userName, Type.USER);
                            if (matches == null || matches.isEmpty()) {
                                noneExistingUsers += noneExistingUsers.isEmpty() ? userName : ", " + userName;
                                continue;
                            } else if (matches.size() > 1) {
                                tooManyMatchedUsers += tooManyMatchedUsers.isEmpty() ? userName : ", " + userName; 
                                continue;
                            }
                            uid = matches.get(0).getName();
                        }
                    } catch (Exception e) {
                        noneExistingUsers += noneExistingUsers.isEmpty() ? userName : ", " + userName;
                        continue;
                    }
                }
                editCommand.addUserNameEntry(uid);
            }

            rejectValues("user", noneExistingUsers, VALIDATION_ERROR_NONE_EXISTING, errors);
            rejectValues("user", invalidBlacklistedUsers, VALIDATION_ERROR_INVALID_BLACKLISTED, errors);
            rejectValues("user", invalidUsers, VALIDATION_ERROR_INVALID, errors);
            rejectValues("user", tooManyMatchedUsers, VALIDATION_ERROR_TOO_MANY_MATCHES, errors);
            
        }
    }


    private String validateGroupOrUserName(Type type, String name, ACLEditCommand editCommand) {
        try {
            Principal groupOrUser = null;
            boolean exists = false;

            if (type == Type.GROUP) {
                groupOrUser = this.principalFactory.getPrincipal(name, type);
                exists = this.principalManager.validateGroup(groupOrUser);
            } else {
                groupOrUser = this.principalFactory.getPrincipal(name, type);
                exists = this.principalManager.validatePrincipal(groupOrUser);
            }

            if (groupOrUser != null && !exists) {
                return VALIDATION_ERROR_NONE_EXISTING;
            }

            if (!repository.isValidAclEntry(editCommand.getPrivilege(), groupOrUser)) {
                return VALIDATION_ERROR_INVALID_BLACKLISTED;
            }
        } catch (InvalidPrincipalException e) {
            return VALIDATION_ERROR_INVALID;
        }
        return VALIDATION_OK;
    }


    private void rejectValues(String type, String groupsOrUsers, String errorType, Errors errors) {
        if (!groupsOrUsers.isEmpty()) {
            if (!groupsOrUsers.contains(",")) {
                errors.rejectValue(type + "Names", "permissions." + type + "." + errorType + ".value",
                        new Object[] { groupsOrUsers }, "The " + type + " " + groupsOrUsers
                                + " does not exist, is not valid, is blacklisted, yielded too many matches or cannot be found");
            } else {
                errors.rejectValue(type + "Names", "permissions." + type + "." + errorType + ".values",
                        new Object[] { groupsOrUsers }, "The " + type + "s " + groupsOrUsers
                                + " does not exist, are not valid, are blacklisted, yielded too many matches or cannot be found");
            }
        }
    }


    private String getAc_userName(String userName, String[] ac_userNames, List<String> userNameEntries) {
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
