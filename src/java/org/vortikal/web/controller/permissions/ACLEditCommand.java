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

import org.vortikal.repository.Acl;
import org.vortikal.security.Principal;
import org.vortikal.web.controller.AbstractSaveCancelCommand;



public class ACLEditCommand extends AbstractSaveCancelCommand {

    private String addUserAction = null;
    private String removeUserAction = null;
    private String addGroupAction = null;
    private String removeGroupAction = null;
    private boolean everyone;
    private String owner;
    private Principal[] users;
    private Principal[] groups;
    private String[] withdrawUserURLs;
    private String[] withdrawGroupURLs;
    private String userNames[];
    private String groupNames[];
    private Acl editedACL;
    

    public ACLEditCommand(String submitURL) {
        super(submitURL);
    }
    

    /**
     * Gets the value of addUserAction
     *
     * @return the value of addUserAction
     */
    public String getAddUserAction() {
        return this.addUserAction;
    }

    /**
     * Sets the value of addUserAction
     *
     * @param addUserAction Value to assign to this.addUserAction
     */
    public void setAddUserAction(String addUserAction) {
        this.addUserAction = addUserAction;
    }

    /**
     * Gets the value of removeUserAction
     *
     * @return the value of removeUserAction
     */
    public String getRemoveUserAction() {
        return this.removeUserAction;
    }

    /**
     * Sets the value of removeUserAction
     *
     * @param removeUserAction Value to assign to this.removeUserAction
     */
    public void setRemoveUserAction(String removeUserAction) {
        this.removeUserAction = removeUserAction;
    }

    /**
     * Gets the value of addGroupAction
     *
     * @return the value of addGroupAction
     */
    public String getAddGroupAction() {
        return this.addGroupAction;
    }

    /**
     * Sets the value of addGroupAction
     *
     * @param addGroupAction Value to assign to this.addGroupAction
     */
    public void setAddGroupAction(String addGroupAction) {
        this.addGroupAction = addGroupAction;
    }

    /**
     * Gets the value of removeGroupAction
     *
     * @return the value of removeGroupAction
     */
    public String getRemoveGroupAction() {
        return this.removeGroupAction;
    }

    /**
     * Sets the value of removeGroupAction
     *
     * @param removeGroupAction Value to assign to this.removeGroupAction
     */
    public void setRemoveGroupAction(String removeGroupAction) {
        this.removeGroupAction = removeGroupAction;
    }
    

    /**
     * Gets the value of everyone
     *
     * @return the value of everyone
     */
    public boolean isEveryone() {
        return this.everyone;
    }

    /**
     * Sets the value of everyone
     *
     * @param everyone Value to assign to this.everyone
     */
    public void setEveryone(boolean everyone)  {
        this.everyone = everyone;
    }

    /**
     * Gets the value of users
     *
     * @return the value of users
     */
    public Principal[] getUsers() {
        return this.users;
    }

    /**
     * Sets the value of users
     *
     * @param users Value to assign to this.users
     */
    public void setUsers(Principal[] users)  {
        this.users = users;
    }

    /**
     * Gets the value of groups
     *
     * @return the value of groups
     */
    public Principal[] getGroups() {
        return this.groups;
    }

    /**
     * Sets the value of groups
     *
     * @param groups Value to assign to this.groups
     */
    public void setGroups(Principal[] groups)  {
        this.groups = groups;
    }
    
    /**
     * Gets the value of owner
     *
     * @return the value of owner
     */
    public String getOwner() {
        return this.owner;
    }

    /**
     * Sets the value of owner
     *
     * @param owner Value to assign to this.owner
     */
    public void setOwner(String owner)  {
        this.owner = owner;
    }

    /**
     * Gets the value of withdrawUserURLs
     *
     * @return the value of withdrawUserURLs
     */
    public String[] getWithdrawUserURLs() {
        return this.withdrawUserURLs;
    }

    /**
     * Sets the value of withdrawUserURLs
     *
     * @param withdrawUserURLs Value to assign to this.withdrawUserURLs
     */
    public void setWithdrawUserURLs(String[] withdrawUserURLs)  {
        this.withdrawUserURLs = withdrawUserURLs;
    }

    /**
     * Gets the value of withdrawGroupURLs
     *
     * @return the value of withdrawGroupURLs
     */
    public String[] getWithdrawGroupURLs() {
        return this.withdrawGroupURLs;
    }

    /**
     * Sets the value of withdrawGroupURLs
     *
     * @param withdrawGroupURLs Value to assign to this.withdrawGroupURLs
     */
    public void setWithdrawGroupURLs(String[] withdrawGroupURLs)  {
        this.withdrawGroupURLs = withdrawGroupURLs;
    }

    
    /**
     * Gets the value of editedACL
     *
     * @return the value of editedACL
     */
    public Acl getEditedACL() {
        return this.editedACL;
    }

    /**
     * Sets the value of editedACL
     *
     * @param editedACL Value to assign to this.editedACL
     */
    public void setEditedACL(Acl editedACL)  {
        this.editedACL = editedACL;
    }


    public String[] getGroupNames() {
        return groupNames;
    }


    public void setGroupNames(String[] groupNames) {
        this.groupNames = groupNames;
    }


    public String[] getUserNames() {
        return userNames;
    }


    public void setUserNames(String[] userNames) {
        this.userNames = userNames;
    }

}

