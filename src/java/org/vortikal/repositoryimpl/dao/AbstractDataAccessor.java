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
package org.vortikal.repositoryimpl.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repositoryimpl.ACL;
import org.vortikal.repositoryimpl.PropertyManagerImpl;
import org.vortikal.repositoryimpl.Resource;
import org.vortikal.util.web.URLUtil;


public abstract class AbstractDataAccessor
  implements InitializingBean, DataAccessor {

    protected Log logger = LogFactory.getLog(this.getClass());

    protected ContentStore contentStore;
    
    protected PropertyManagerImpl propertyManager;
    
    public void setContentStore(ContentStore contentStore) {
        this.contentStore = contentStore;
    }

    public void setPropertyManager(PropertyManagerImpl propertyManager) {
        this.propertyManager = propertyManager;
    }

    public void afterPropertiesSet() throws Exception {
        // FIXME: Implement
    }
    
    protected void loadACLs(Connection conn, Resource[] resources)
        throws SQLException {

        if (resources == null || resources.length == 0) {
            return;
        }

        Map acls = new HashMap();

        for (int i = 0; i < resources.length; i++) {

            String[] path = URLUtil.splitUriIncrementally(
                resources[i].getURI());

            for (int j = path.length -1; j >= 0; j--) {

                if (!acls.containsKey(path[j])) {
                    acls.put(path[j], null);
                }
            }

            /* Initialize (empty) ACL for resource: */
            ACL acl = new ACL();
            resources[i].setACL(acl);
        
        }

        if (acls.size() == 0) {
            throw new SQLException("No ancestor path");
        }
    

        /* Populate the parent ACL map (these are all the ACLs that
         * will be needed) */
        executeACLQuery(conn, acls);

        for (int i = 0; i < resources.length; i++) {

            Resource resource = resources[i];
            ACL acl = null;

            if (!resource.isInheritedACL()) {
            
                if (!acls.containsKey(resource.getURI())) {
                    throw new SQLException(
                        "Database inconsistency: resource " +
                        resource.getURI() + " claims  ACL is inherited, " +
                        "but no ACL exists");
                }
                
                acl = (ACL) acls.get(resource.getURI());

            } else {

                String[] path = URLUtil.splitUriIncrementally(
                    resource.getURI());

                for (int j = path.length - 2; j >= 0; j--) {

                    ACL found = (ACL) acls.get(path[j]);

                    if (found != null) {
                        try {
                            /* We have to clone the ACL here, because ACLs
                             * and resources are "doubly linked". */
                            acl = (ACL) found.clone();
                        } catch (CloneNotSupportedException e) {
                            throw new SQLException(e.getMessage());
                        }

                        break;
                    }                
                }

                if (acl == null) {
                    throw new SQLException("Resource " + resource.getURI() +
                                           ": no ACL to inherit! At least root " +
                                           "resource should contain an ACL");
                }
            }

            resource.setACL(acl);
        }
    }

    protected abstract void executeACLQuery(Connection conn, Map acls) throws SQLException;

}
