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
package org.vortikal.security;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;

import org.springframework.beans.factory.InitializingBean;

import org.vortikal.util.cache.SimpleCache;


public class ChainedPrincipalStore implements InitializingBean, PrincipalStore {

    private Log logger = LogFactory.getLog(this.getClass());

    private List managers = null;

    // Maintain cache: principal -> item(map(groups)) for
    // fast group membership lookup
    private SimpleCache cache = null;
    

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
        if (this.cache == null) {
            throw new BeanInitializationException(
                "Bean property 'cache' cannot be null");
        }
    }


    public boolean validatePrincipal(String name)
        throws AuthenticationProcessingException {

        for (Iterator i = managers.iterator(); i.hasNext();) {
            PrincipalStore manager = (PrincipalStore) i.next();
            if (manager.validatePrincipal(name)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Validated principal '" + name
                                 + "' using manager " + manager);
                }
                return true;
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Invalid principal '" + name + "'");
        }
        return false;
    }


    public boolean validateGroup(String groupName)
        throws AuthenticationProcessingException {
        for (Iterator i = managers.iterator(); i.hasNext();) {
            PrincipalStore manager = (PrincipalStore) i.next();
            if (manager.validateGroup(groupName)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Validated group '" + groupName
                                 + "' using manager " + manager);
                }
                return true;
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Invalid group '" + groupName + "'");
        }
        return false;
    }
    

    public String[] resolveGroup(String groupName)
        throws AuthenticationProcessingException {
        for (Iterator i = managers.iterator(); i.hasNext();) {
            PrincipalStore manager = (PrincipalStore) i.next();
            if (manager.validateGroup(groupName)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Resolved group '" + groupName
                                 + "' using manager " + manager);
                }
                return manager.resolveGroup(groupName);
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Unable to resolve group '" + groupName + "'");
        }
        return new String[0];
    }
    

    public boolean isMember(String principal, String groupName)
        throws AuthenticationProcessingException {

        GroupItem item = (GroupItem) cache.get(principal);
        if (item == null) {

            item = new GroupItem();
            cache.put(principal, item);
            
        }

        Map groupsMap = item.getGroupsMap();

        if (groupsMap.containsKey(groupName)) {
            boolean isMember = (groupsMap.get(groupName) != null);
            if (logger.isDebugEnabled()) {
                logger.debug("Validated group membership for principal '"
                        + principal + "', group '" + groupName + "'" + " to '"
                        + isMember + "' from cache ");
            }
            return isMember;

        }

        for (Iterator i = managers.iterator(); i.hasNext();) {
            PrincipalStore manager = (PrincipalStore) i.next();
            if (manager.validateGroup(groupName)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Validated group membership for principal '"
                            + principal + "', group '" + groupName + "' using "
                            + "manager " + manager);
                }

                boolean isMember = manager.isMember(principal, groupName);
                if (isMember) {
                    item.getGroupsMap().put(groupName, new Object());
                } else {
                    item.getGroupsMap().put(groupName, null);
                }
                return isMember;
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Validating group membership failed: no principal "
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
    
}
