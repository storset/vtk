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

    private static final String VALIDATION_ERROR_NOT_FOUND = "not.found";
    private static final String VALIDATION_ERROR_ILLEGAL_BLACKLISTED = "illegal.blacklisted";
    private static final String VALIDATION_ERROR_ILLEGAL = "illegal";
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
                errors.rejectValue("groupNames", "permissions.group.missing.value",
                        "You must type a group name");
            }
            validateGroupNames(editCommand, errors);

        } else if (editCommand.getAddUserAction() != null) {
            String[] userNames = editCommand.getUserNames();

            if (userNames.length == 0) {
                errors.rejectValue("userNames", "permissions.user.missing.value",
                        "You must type a username");
            }
            validateUserNames(editCommand, errors);
        }
    }


    private void validateGroupNames(ACLEditCommand editCommand, Errors errors) {
        String[] groupNames = editCommand.getGroupNames();

        if (groupNames.length > 0) {

            String notFound = new String();
            String illegalBlacklisted = new String();
            String illegal = new String();

            for (String groupName : groupNames) {
                String validation = validateGroupOrUserName(Type.GROUP, groupName, editCommand);

                if (!VALIDATION_OK.equals(validation)) {
                    // *** TODO: is it possible to refactor this seven lines in a sensible way? ***
                    if (VALIDATION_ERROR_NOT_FOUND.equals(validation)) {
                        notFound += toCSV(notFound, groupName);
                    } else if (VALIDATION_ERROR_ILLEGAL_BLACKLISTED.equals(validation)) {
                        illegalBlacklisted += toCSV(illegalBlacklisted, groupName);
                    } else if (VALIDATION_ERROR_ILLEGAL.equals(validation)) {
                        illegal += toCSV(illegal, groupName);
                    }
                    // *** ^ ***
                    continue;
                }
            }

            rejectValues("group", notFound, VALIDATION_ERROR_NOT_FOUND, errors);
            rejectValues("group", illegalBlacklisted, VALIDATION_ERROR_ILLEGAL_BLACKLISTED, errors);
            rejectValues("group", illegal, VALIDATION_ERROR_ILLEGAL, errors);
        }

    }


    private void validateUserNames(ACLEditCommand editCommand, Errors errors) {
        String[] userNames = editCommand.getUserNames();

        if (userNames.length > 0) {

            String notFound = new String();
            String illegalBlacklisted = new String();
            String illegal = new String();
            String tooManyMatchedUsers = new String();

            for (String userName : userNames) {

                userName = userName.trim();
                String uid = userName;

                // Assume a username and validate it as such
                if (!userName.contains(" ")) {
                    
                    String validation = validateGroupOrUserName(Type.USER, userName, editCommand);
                    if (!VALIDATION_OK.equals(validation)) {
                        // *** TODO: is it possible to refactor this seven lines in a sensible way? ***
                        if (VALIDATION_ERROR_NOT_FOUND.equals(validation)) {
                            notFound += toCSV(notFound, userName);
                        } else if (VALIDATION_ERROR_ILLEGAL_BLACKLISTED.equals(validation)) {
                            illegalBlacklisted += toCSV(illegalBlacklisted, userName);
                        } else if (VALIDATION_ERROR_ILLEGAL.equals(validation)) {
                            illegal += toCSV(illegal, userName);
                        }
                        // *** ^ ***
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

                        // Entered name is selected from autocomplete
                        // suggestions and we have username
                        if (ac_userName != null && !"".equals(ac_userName)) {
                             
                            String validation = validateGroupOrUserName(Type.USER, ac_userName, editCommand);
                            if (!VALIDATION_OK.equals(validation)) {
                                // *** TODO: is it possible to refactor this seven lines in a sensible way? ***y?
                                if (VALIDATION_ERROR_NOT_FOUND.equals(validation)) {
                                    notFound += toCSV(notFound, userName);
                                } else if (VALIDATION_ERROR_ILLEGAL_BLACKLISTED.equals(validation)) {
                                    illegalBlacklisted += toCSV(illegalBlacklisted, userName);
                                } else if (VALIDATION_ERROR_ILLEGAL.equals(validation)) {
                                    illegal += toCSV(illegal, userName);
                                }
                                // *** ^ ***
                                continue;
                            }

                            uid = ac_userName;
                        } else {
                            List<Principal> matches = this.principalFactory.search(userName, Type.USER);
                            if (matches == null || matches.isEmpty()) {
                                notFound += toCSV(notFound, userName);
                                continue;
                            } else if (matches.size() > 1) {
                                tooManyMatchedUsers += toCSV(tooManyMatchedUsers, userName);
                                continue;
                            }
                            uid = matches.get(0).getName();
                        }
                    } catch (Exception e) {
                        // TODO: is it ok with 'not exist' error-message here?
                        notFound += notFound.isEmpty() ? userName : ", " + userName;
                        continue;
                    }
                }
                editCommand.addUserNameEntry(uid);
            }

            rejectValues("user", notFound, VALIDATION_ERROR_NOT_FOUND, errors);
            rejectValues("user", illegalBlacklisted, VALIDATION_ERROR_ILLEGAL_BLACKLISTED, errors);
            rejectValues("user", illegal, VALIDATION_ERROR_ILLEGAL, errors);
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
                return VALIDATION_ERROR_NOT_FOUND;
            }

            if (!repository.isValidAclEntry(editCommand.getPrivilege(), groupOrUser)) {
                return VALIDATION_ERROR_ILLEGAL_BLACKLISTED;
            }
        } catch (InvalidPrincipalException e) {
            return VALIDATION_ERROR_ILLEGAL;
        }
        return VALIDATION_OK;
    }


    private void rejectValues(String type, String groupsOrUsers, String errorType, Errors errors) {
        if (!groupsOrUsers.isEmpty()) {
            if (!groupsOrUsers.contains(",")) {
                errors.rejectValue(type + "Names", "permissions." + type + "." + errorType + ".value",
                        new Object[] { groupsOrUsers }, "The " + type + " " + groupsOrUsers
                                + " does not exist, is illegal (possibly blacklisted) or yielded too many matches");
            } else {
                errors.rejectValue(type + "Names", "permissions." + type + "." + errorType + ".values",
                        new Object[] { groupsOrUsers }, "The " + type + "s " + groupsOrUsers
                                + " does not exist, are illegal (possibly blacklisted) or yielded too many matches");
            }
        }
    }


    protected String getAc_userName(String userName, String[] ac_userNames, List<String> userNameEntries) {
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


    protected String toCSV(String csv, String name) {
        String trimmedName = name.trim();
        if (csv.isEmpty()) {
            return "'" + trimmedName + "'";
        } else {
            if(!csv.contains(trimmedName)) {
              return ", '" + trimmedName + "'";
            } else {
              return "";
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
