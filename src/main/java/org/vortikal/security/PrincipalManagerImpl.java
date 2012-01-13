/* Copyright (c) 2004, 2007 University of Oslo, Norway
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
package org.vortikal.security;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.util.cache.SimpleCache;
import org.vortikal.util.cache.SimpleCacheImpl;


/**
 * A simple principal manager implementation.
 * 
 * Delegates API calls to a set of underlying {@link GroupStore} and {@link PrincipalStore} instances.
 * Caches aggregated group membership lookups.
 *
 */
public class PrincipalManagerImpl implements PrincipalManager, InitializingBean {
    
    private static Log logger = LogFactory.getLog(PrincipalManagerImpl.class);

    private PrincipalStore principalStore;
    private GroupStore groupStore;
    
    public void setPrincipalStore(PrincipalStore principalStore) {
        this.principalStore = principalStore;
    }

    public void setPrincipalStores(List<PrincipalStore> principalStores) {
        logger.info("Initialized with principal stores: " + principalStores);
        
        if (principalStores != null) {
            this.principalStore = new ChainedPrincipalStore(principalStores); 
        }
    }
    
    public void setGroupStores(List<GroupStore> groupStores) {
        logger.info("Initialized with group stores: " + groupStores);
        
        if (groupStores != null) {
            this.groupStore = new NewChainedGroupStore(groupStores); 
        }
    }    

    
    @Override
    public void afterPropertiesSet() throws Exception {
        if (this.principalStore == null) {
            throw new BeanInitializationException(
                "JavaBean Property 'principalStore' must be specified");
        }

        if (this.groupStore == null) {
            throw new BeanInitializationException(
                "JavaBean Property 'groupStore' must be specified");
        }
    }
    
    @Override
    public boolean validatePrincipal(Principal principal)
        throws AuthenticationProcessingException {
        return this.principalStore.validatePrincipal(principal);
    }


    @Override
    public boolean validateGroup(Principal group) throws AuthenticationProcessingException {
        return this.groupStore.validateGroup(group);
    }


    @Override
    public boolean isMember(Principal principal, Principal group) {
        return this.groupStore.isMember(principal, group);
    }


    @Override
    public Set<Principal> getMemberGroups(Principal principal) {
        return this.groupStore.getMemberGroups(principal);
    }

    private static final class ChainedPrincipalStore implements PrincipalStore {

        private List<PrincipalStore> stores = null;

        ChainedPrincipalStore(List<PrincipalStore> stores) {
            this.stores = stores;
        }

        @Override
        public boolean validatePrincipal(Principal principal)
            throws AuthenticationProcessingException {
            
            for (PrincipalStore manager: this.stores) {
                if (manager.validatePrincipal(principal)) {
                    return true;
                }
            }
            
            return false;
        }

        @Override
        public int getOrder() {
            // XXX: DUMMY - not used, but should be refactored
            return 0;
        }

    }
    
    private static final class NewChainedGroupStore implements GroupStore {
        private List<GroupStore> stores;
        private SimpleCache<Principal, Set<Principal>> groupMembershipCache;
        
        NewChainedGroupStore(List<GroupStore> stores) {
            this.stores = stores;
            SimpleCacheImpl<Principal, Set<Principal>> cache = new SimpleCacheImpl<Principal, Set<Principal>>(60);

            // Refresh cached groups periodically, regardless of user activity:
            cache.setRefreshTimestampOnGet(false);
            
            this.groupMembershipCache = cache;
        }

        @Override
        public boolean validateGroup(Principal group) throws AuthenticationProcessingException {
            for (GroupStore store: this.stores) {
                if (store.validateGroup(group)) {
                    return true;
                }
            }
            
            return false;
        }

        @Override
        public boolean isMember(Principal principal, Principal group) {
            return getMemberGroups(principal).contains(group);
        }

        @Override
        public Set<Principal> getMemberGroups(Principal principal) {
            Set<Principal> memberGroups = this.groupMembershipCache.get(principal);
            if (memberGroups == null) {
                logger.debug("Groups for principal "+ principal + " not in cache.");
                
                memberGroups = new HashSet<Principal>();
                for (GroupStore store : this.stores) {
                    Set<Principal> groups = store.getMemberGroups(principal);
                    if (groups != null) { // Extra precaution ..
                        memberGroups.addAll(groups);
                    }
                }

                // Make immutable
                memberGroups = Collections.unmodifiableSet(memberGroups);

                // Cache
                this.groupMembershipCache.put(principal, memberGroups);
            }

            return memberGroups;
        }

        @Override
        public int getOrder() {
            return 0;
        }
        
    }

    @Deprecated
    private class ChainedGroupStore implements GroupStore {

        private List<GroupStore> managers;

        // Maintain cache: principal -> item(map(groups)) for
        // fast group membership lookup
        private SimpleCache<Principal, GroupItem> cache;

        public ChainedGroupStore(List<GroupStore> managers, boolean cache) {
            this.managers = managers;
            if (cache) {
                this.cache = new SimpleCacheImpl<Principal, GroupItem>(60);
            }
        }
        
        @Override
        public boolean validateGroup(Principal group)
            throws AuthenticationProcessingException {
            for (GroupStore manager: this.managers) {
                if (manager.validateGroup(group)) {
                    return true;
                }
            }
            return false;
        }
        

        @Override
        public boolean isMember(Principal principal, Principal group)
            throws AuthenticationProcessingException {

            if (this.cache == null) {
                return isMemberUncached(principal, group);
            }
            return isMemberCached(principal, group);
        }
        
        private boolean isMemberUncached(Principal principal, Principal group)
            throws AuthenticationProcessingException {

            for (GroupStore manager: this.managers) {
                if (manager.validateGroup(group)) {
                    boolean isMember = manager.isMember(principal, group);
                    return isMember;
                }
            }
            return false;
        }


        /**
         * Method does membership lookup and caches the result.
         */
        private boolean isMemberCached(Principal principal, Principal group)
            throws AuthenticationProcessingException {

            String groupName = group.getQualifiedName();
                
            GroupItem item = this.cache.get(principal);
            if (item == null) {
                item = new GroupItem();
                this.cache.put(principal, item);
            }

            Map<String, Object> groupsMap = item.getGroupsMap();

            if (groupsMap.containsKey(groupName)) {
                boolean isMember = (groupsMap.get(groupName) != null);
                return isMember;
            }

            for (GroupStore manager: this.managers) {
                // XXX: We currently have two group stores for the same domain,
                // Should both member sets be checked? (currently only the first)
                if (manager.validateGroup(group)) {
                    boolean isMember = manager.isMember(principal, group);

                    if (isMember) {
                        item.getGroupsMap().put(groupName, new Object());
                    } else {
                        item.getGroupsMap().put(groupName, null);
                    }
                    return isMember;
                }
            }
            return false;
        }
        
        @Override
        public Set<Principal> getMemberGroups(Principal principal) {
            Set<Principal> groups = new HashSet<Principal>();
            for (GroupStore manager: this.managers) {
                Set<Principal> memberGroups = manager.getMemberGroups(principal);
                if (memberGroups != null) {
                    groups.addAll(memberGroups);
                }
            }
            
            // Cache here instead ? Should benefit all aggregated group stores, and cached
            // result can be used by isMember method.
            return groups;
        }

        private class GroupItem {
            private Map<String, Object> groupsMap = new HashMap<String, Object>();

            public Map<String, Object> getGroupsMap() {
                return this.groupsMap;
            }
        }
        
        @Override
        public int getOrder() {
            // XXX: DUMMY - not used, but should be refactored
            return 0;
        }
    }

}
