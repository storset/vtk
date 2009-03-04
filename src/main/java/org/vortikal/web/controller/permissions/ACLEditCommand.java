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

import java.util.List;
import java.util.Map;

import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.web.controller.UpdateCancelCommand;

public class ACLEditCommand extends UpdateCancelCommand {

    private String addUserAction = null;
    private String removeUserAction = null;
    private String addGroupAction = null;
    private String removeGroupAction = null;
    private boolean grouped;
    private String owner;
    private List<Principal> users;
    private List<Principal> groups;
    private Map<String, String> removeUserURLs;
    private Map<String, String> removeGroupURLs;
    private String userNames[] = new String[0];
    private String groupNames[] = new String[0];
    private Resource resource;
    private String saveAction = null;
    
    public ACLEditCommand(String submitURL) {
        super(submitURL);
    }
    
    public String getAddUserAction() {
        return this.addUserAction;
    }

    public void setAddUserAction(String addUserAction) {
        this.addUserAction = addUserAction;
    }

    public String getRemoveUserAction() {
        return this.removeUserAction;
    }

    public void setRemoveUserAction(String removeUserAction) {
        this.removeUserAction = removeUserAction;
    }

    public String getAddGroupAction() {
        return this.addGroupAction;
    }

    public void setAddGroupAction(String addGroupAction) {
        this.addGroupAction = addGroupAction;
    }

    public String getRemoveGroupAction() {
        return this.removeGroupAction;
    }

    public void setRemoveGroupAction(String removeGroupAction) {
        this.removeGroupAction = removeGroupAction;
    }
    
    public boolean isGrouped() {
        return this.grouped;
    }

    public void setGrouped(boolean grouped)  {
        this.grouped = grouped;
    }

    public List<Principal> getUsers() {
        return this.users;
    }

    public void setUsers(List<Principal> users)  {
        this.users = users;
    }

    public List<Principal> getGroups() {
        return this.groups;
    }

    public void setGroups(List<Principal> groups)  {
        this.groups = groups;
    }
    
    public String getOwner() {
        return this.owner;
    }

    public void setOwner(String owner)  {
        this.owner = owner;
    }

    public Map<String, String> getRemoveUserURLs() {
        return this.removeUserURLs;
    }

    public void setRemoveUserURLs(Map<String, String> removeUserURLs)  {
        this.removeUserURLs = removeUserURLs;
    }

    public Map<String, String> getRemoveGroupURLs() {
        return this.removeGroupURLs;
    }

    public void setRemoveGroupURLs(Map<String, String> removeGroupURLs)  {
        this.removeGroupURLs = removeGroupURLs;
    }

    public void setResource(Resource resource)  {
        this.resource = resource;
    }

    public Resource getResource() {
        return this.resource;
    }

    public String[] getGroupNames() {
        return this.groupNames;
    }

    public void setGroupNames(String[] groupNames) {
        this.groupNames = groupNames;
    }

    public String[] getUserNames() {
        return this.userNames;
    }

    public void setUserNames(String[] userNames) {
        this.userNames = userNames;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(this.getClass().getName());
        sb.append("[addUserAction=").append(this.addUserAction);
        sb.append(", removeUserAction=").append(this.removeUserAction);
        sb.append(", addGroupAction=").append(this.addGroupAction);
        sb.append(", remoceGroupAction=").append(this.removeGroupAction);
        sb.append(", grouped=").append(this.grouped);
        sb.append(", owner=").append(this.owner);
        sb.append(", users=").append(this.users);
        sb.append(", groups=").append(this.groups);
        sb.append(", removeUserURLs=").append(this.removeUserURLs);
        sb.append(", removeGroupURLs=").append(this.removeGroupURLs);
        sb.append(", userNames=").append(java.util.Arrays.asList(this.userNames));
        sb.append(", groupNames=").append(java.util.Arrays.asList(this.groupNames));
        sb.append(", resource=").append(this.resource);
        sb.append("]");
        return sb.toString();
    }

	public String getSaveAction() {
		return saveAction;
	}

	public void setSaveAction(String saveAction) {
		this.saveAction = saveAction;
	}
    

}

