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

import org.springframework.beans.factory.annotation.Required;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.vortikal.security.InvalidPrincipalException;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalFactory;
import org.vortikal.security.PrincipalManager;

public class ACLEditCommandValidator implements Validator {

    private PrincipalManager principalManager;
    private PrincipalFactory principalFactory;

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
            validateUserNames(editCommand.getUserNames(), errors);
            validateGroupNames(editCommand.getGroupNames(), errors);
        }

        if (editCommand.getAddUserAction() != null) {
            String userNames[] = editCommand.getUserNames();

            if (userNames.length == 0) {
                errors.rejectValue("userNames", "permissions.user.missing.value",
                        "You must type a value");
            }

            validateUserNames(userNames, errors);

        } else if (editCommand.getAddGroupAction() != null) {
            String[] groupNames = editCommand.getGroupNames();

            if (groupNames.length == 0) {
                errors.rejectValue("groupNames", "permissions.group.missing.value",
                        "You must type a value");
            }

            validateGroupNames(groupNames, errors);
        }

    }

    private void validateUserNames(String[] userNames, Errors errors) {
        for (String userName : userNames) {
            if (!userName.toLowerCase().equals(userName)) {
                errors.rejectValue("userNames", "permissions.user.uppercase.value",
                        "You must use lower case characters");
            } else {
                try {
                    Principal principal = principalFactory.getPrincipal(userName,
                            Principal.Type.USER);

                    if (!this.principalManager.validatePrincipal(principal)) {

                        errors.rejectValue("userNames", "permissions.user.wrong.value",
                                new Object[] { userName }, "User '" + userName
                                        + "' does not exist");
                    }

                } catch (InvalidPrincipalException e) {
                    errors.rejectValue("userNames", "permissions.user.wrong.value",
                            new Object[] { userName }, "User '" + userName
                                    + "' is illegal");
                }
            }
        }
    }

    private void validateGroupNames(String[] groupNames, Errors errors) {
        for (String groupName : groupNames) {
            Principal group = null;
            try {
                group = principalFactory.getPrincipal(groupName, Principal.Type.GROUP);
            } catch (InvalidPrincipalException e) {
                errors.rejectValue("groupNames", "permissions.group.illegal.value",
                        new Object[] { groupName }, "String '" + groupName
                                + "' is an illegal group name");
            }

            if (group != null && !this.principalManager.validateGroup(group))
                errors.rejectValue("groupNames", "permissions.group.wrong.value",
                        new Object[] { groupName }, "Group '" + groupName
                                + "' does not exist");
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

}
