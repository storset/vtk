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

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.vortikal.security.InvalidPrincipalException;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalManager;

/**
 */
public class ACLEditCommandValidator implements Validator, InitializingBean {

    private PrincipalManager principalManager;
    
    /**
     * @param principalManager The principalManager to set.
     */
    public void setPrincipalManager(PrincipalManager principalManager) {
        this.principalManager = principalManager;
    }
    
    /**
     * @see org.springframework.validation.Validator#supports(java.lang.Class)
     */
    public boolean supports(Class clazz) {
        return (clazz == ACLEditCommand.class);

    }

    public void validate(Object command, Errors errors) {
        ACLEditCommand editCommand = (ACLEditCommand) command;

        // Don't validate on cancel/save
        if (editCommand.getCancelAction() != null 
                || editCommand.getSaveAction() != null) 
            return;
        
        if (editCommand.getAddUserAction() != null) {
            String userName = editCommand.getUserName();

            if (userName == null || userName.trim().equals("")) {
                errors.rejectValue("userName", "permissions.user.missing.value",
                                   "You must type a value");
            } else {
                try { 
                    Principal principal = principalManager.getPrincipal(userName);	

                    if (!principalManager.validatePrincipal(principal))
                        errors.rejectValue("userName", "permissions.user.wrong.value", 
                                new Object[] {userName}, "User '" + userName
                                + "' does not exist");

                } catch (InvalidPrincipalException e) {
                    errors.rejectValue("userName", "permissions.user.wrong.value", 
                            new Object[] {userName}, "User '" + userName
                                       + "' is illegal");
                }
            }
            
        } else if (editCommand.getAddGroupAction() != null) {
            String groupName = editCommand.getGroupName();

            if (groupName == null || groupName.trim().equals(""))
                errors.rejectValue("groupName",
                                   "permissions.group.missing.value",
                                   "You must type a value");

            else if (!principalManager.validateGroup(groupName))
                errors.rejectValue("groupName", "permissions.group.wrong.value",
                        new Object[] {groupName}, "Group '" + groupName
                                   + "' does not exist");
        }
    }

    /**
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {
        if (principalManager == null) {
            throw new BeanInitializationException(
                "Property 'principalManager' cannot be null");
        }
    }

}
