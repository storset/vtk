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
package org.vortikal.security.store;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.security.AuthenticationProcessingException;
import org.vortikal.security.GroupStore;
import org.vortikal.security.Principal;
import org.vortikal.util.cache.SimpleCache;


public class ChainedGroupStore implements InitializingBean, GroupStore {

    private Log logger = LogFactory.getLog(this.getClass());

    private List managers = null;

    // Maintain cache: principal -> item(map(groups)) for
    // fast group membership lookup
    private SimpleCache cache = null;
    

    public ChainedGroupStore() {
    }

    public ChainedGroupStore(List managers) {
        this.managers = managers;
    }

    public ChainedGroupStore(List managers, SimpleCache cache) {
        this.managers = managers;
        this.cache = cache;
    }
    
    

    public void setManagers(List managers) {
        this.managers = managers;
    }
    

    public void setCache(SimpleCache cache) {
        this.cache = cache;
    }
    

    public void afterPropertiesSet() throws Exception {
        if (this.managers == null) {
            throw new BeanInitializationException(
                "Bean property 'managers' cannot be null");
        }
    }


    public boolean validateGroup(Principal group)
        throws AuthenticationProcessingException {
        for (Iterator i = this.managers.iterator(); i.hasNext();) {
            GroupStore manager = (GroupStore) i.next();
            if (manager.validateGroup(group)) {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Validated group '" + group
                                 + "' using manager " + manager);
                }
                return true;
            }
        }
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Invalid group '" + group + "'");
        }
        return false;
    }
    

//    public String[] resolveGroup(Principal group)
//        throws AuthenticationProcessingException {
//        for (Iterator i = managers.iterator(); i.hasNext();) {
//            PrincipalStore manager = (PrincipalStore) i.next();
//            if (manager.validateGroup(group)) {
//                if (logger.isDebugEnabled()) {
//                    logger.debug("Resolved group '" + group
//                                 + "' using manager " + manager);
//                }
//                return manager.resolveGroup(group);
//            }
//        }
//        if (logger.isDebugEnabled()) {
//            logger.debug("Unable to resolve group '" + group + "'");
//        }
//        return new String[0];
//    }
    
    

    public boolean isMember(Principal principal, Principal group)
        throws AuthenticationProcessingException {

        if (this.cache == null) {
            return isMemberUncached(principal, group);
        }
        return isMemberCached(principal, group);
    }
    
    public boolean isMemberUncached(Principal principal, Principal group)
        throws AuthenticationProcessingException {

        for (Iterator i = this.managers.iterator(); i.hasNext();) {
            GroupStore manager = (GroupStore) i.next();
            if (manager.validateGroup(group)) {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Validated group membership for principal '"
                            + principal + "', group '" + group.getQualifiedName() + "' using "
                            + "manager " + manager);
                }

                boolean isMember = manager.isMember(principal, group);
                return isMember;
            }
        }
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Validating group membership failed: no principal "
                    + "manager match for principal '" + principal + "'"
                    + ", group '" + group.getQualifiedName() + "'");
        }
        return false;
    }


    public boolean isMemberCached(Principal principal, Principal group)
        throws AuthenticationProcessingException {

        String groupName = group.getQualifiedName();
            
            GroupItem item = (GroupItem) this.cache.get(principal);
            if (item == null) {

                item = new GroupItem();
                this.cache.put(principal, item);
            
            }

            Map groupsMap = item.getGroupsMap();

            if (groupsMap.containsKey(groupName)) {
                boolean isMember = (groupsMap.get(groupName) != null);
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Validated group membership for principal '"
                                 + principal + "', group '" + groupName + "'" + " to '"
                                 + isMember + "' from cache ");
                }
                return isMember;

            }


        for (Iterator i = this.managers.iterator(); i.hasNext();) {
            GroupStore manager = (GroupStore) i.next();
            // XXX: We currently have two group stores for the same domain,
            // Should both member sets be checked? (currently only the first)
            if (manager.validateGroup(group)) {
                boolean isMember = manager.isMember(principal, group);

                if (isMember) {
                    item.getGroupsMap().put(groupName, new Object());
                } else {
                    item.getGroupsMap().put(groupName, null);
                }
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Validated group membership to '" + isMember 
                            + "' for principal '" + principal + "', group '" 
                            + group + "' using " + "manager " + manager);
                }

                return isMember;
            }
        }
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Validating group membership failed: no principal "
                    + "manager match for principal '" + principal + "'"
                    + ", group '" + groupName + "'");
        }
        return false;
    }
    

    private class GroupItem {
        private Map groupsMap = new HashMap();

        public Map getGroupsMap() {
            return this.groupsMap;
        }
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer(this.getClass().getName());
        sb.append(": managers = [").append(this.managers).append("]");
        sb.append(", cache = ").append(this.cache);
        return sb.toString();
    }

    public int getOrder() {
        // XXX: DUMMY - not used, but should be refactored
        return 0;
    }

    /**
     * @see org.vortikal.security.GroupStore#getMemberGroups(org.vortikal.security.Principal)
     */
    public Set getMemberGroups(Principal principal) {
        Set groups = new HashSet();
        for (Iterator i = this.managers.iterator(); i.hasNext();) {
            GroupStore manager = (GroupStore) i.next();
            
            groups.addAll(manager.getMemberGroups(principal));
        }
        return groups;
    }
    
}
