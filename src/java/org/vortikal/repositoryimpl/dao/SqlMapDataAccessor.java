/* Copyright (c) 2006, University of Oslo, Norway
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

import com.ibatis.sqlmap.client.SqlMapClient;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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

import org.vortikal.repository.Acl;
import org.vortikal.repository.Lock;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repositoryimpl.AclImpl;
import org.vortikal.repositoryimpl.AuthorizationManager;
import org.vortikal.repositoryimpl.LockImpl;
import org.vortikal.repositoryimpl.PropertyManager;
import org.vortikal.repositoryimpl.PropertySetImpl;
import org.vortikal.repositoryimpl.ResourceImpl;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalManager;
import org.vortikal.security.PseudoPrincipal;
import org.vortikal.util.repository.URIUtil;
import org.vortikal.util.web.URLUtil;



/**
 * An iBATIS SQL maps implementation of the DataAccessor interface.
 */
public class SqlMapDataAccessor implements InitializingBean, DataAccessor {

    private Map sqlMaps = new HashMap();

    private Log logger = LogFactory.getLog(this.getClass());

    private ContentStore contentStore;
    private PropertyManager propertyManager;
    private PrincipalManager principalManager;
    private AuthorizationManager authorizationManager;
    private SqlMapClient sqlMapClient;
    
    private boolean optimizedAclCopySupported = false;

    public void setContentStore(ContentStore contentStore) {
        this.contentStore = contentStore;
    }

    public void setPropertyManager(PropertyManager propertyManager) {
        this.propertyManager = propertyManager;
    }

    public void setPrincipalManager(PrincipalManager principalManager) {
        this.principalManager = principalManager;
    }
    
    public void setAuthorizationManager(AuthorizationManager authorizationManager) {
        this.authorizationManager = authorizationManager;
    }

    public void setSqlMapClient(SqlMapClient sqlMapClient) {
        this.sqlMapClient = sqlMapClient;
    }
    
    public void setOptimizedAclCopySupported(boolean optimizedAclCopySupported) {
        this.optimizedAclCopySupported = optimizedAclCopySupported;
    }
    
    public void setSqlMaps(Map sqlMaps) {
        this.sqlMaps = sqlMaps;
    }
    

    public void afterPropertiesSet() throws Exception {
        if (this.contentStore == null) {
            throw new BeanInitializationException(
                "JavaBean property 'contentStore' not specified");
        }
        if (this.propertyManager == null) {
            throw new BeanInitializationException(
                "JavaBean property 'propertyManager' not specified");
        }
        if (this.authorizationManager == null) {
            throw new BeanInitializationException(
                "JavaBean property 'authorizationManager' not specified");
        }
        if (this.principalManager == null) {
            throw new BeanInitializationException(
                "JavaBean property 'principalManager' not specified");
        }
        if (this.sqlMapClient == null) {
            throw new BeanInitializationException(
                "JavaBean property 'principalManager' not specified");
        }
        if (this.sqlMaps == null) {
            throw new BeanInitializationException(
                "JavaBean property 'sqlMaps' not specified");
        }
    }



    public boolean validate() throws IOException {
        throw new IOException("Not implemented");
    }


    public ResourceImpl load(String uri) throws IOException {

        try {
            this.sqlMapClient.startTransaction();
            String sqlMap = getSqlMap("loadResourceByUri");
            Map resourceMap = (Map)
                this.sqlMapClient.queryForObject(sqlMap, uri);
            if (resourceMap == null) {
                return null;
            }
            ResourceImpl resource = new ResourceImpl(uri, this.propertyManager,
                                                     this.authorizationManager);

            loadChildUris(resource);

            Map locks = loadLocks(new String[] {resource.getURI()});
            if (locks.containsKey(resource.getURI())) {
                resource.setLock((Lock) locks.get(resource.getURI()));
            }

            populateStandardProperties(this.propertyManager, this.principalManager,
                                       resource, resourceMap);
            Integer resourceId = new Integer(resource.getID());
            sqlMap = getSqlMap("loadPropertiesForResource");
            List propertyList = this.sqlMapClient.queryForList(sqlMap, resourceId);
            populateCustomProperties(new ResourceImpl[] {resource}, propertyList);

            loadACLs(new ResourceImpl[] {resource});

            this.sqlMapClient.commitTransaction();
            return resource;

        } catch (SQLException e) {
            logger.warn("Error occurred while loading resource: " + uri, e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                this.sqlMapClient.endTransaction();
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }
    }



    public InputStream getInputStream(String uri) throws IOException {
        return this.contentStore.getInputStream(uri);
    }


    public void storeContent(String uri, InputStream inputStream)
            throws IOException {
        this.contentStore.storeContent(uri, inputStream);
    }


    public void deleteExpiredLocks() throws IOException {

        try {
            this.sqlMapClient.startTransaction();
            String sqlMap = getSqlMap("deleteExpiredLocks");
            this.sqlMapClient.update(sqlMap, new Date());
            this.sqlMapClient.commitTransaction();

        } catch (SQLException e) {
            logger.warn("Error occurred while deleting expired locks", e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                this.sqlMapClient.endTransaction();
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }

    }

    


    public void addChangeLogEntry(String loggerID, String loggerType, String uri,
                                  String operation, int resourceId, boolean collection,
                                  boolean recurse) throws IOException {
        try {
            this.sqlMapClient.startTransaction();

            // XXX: convert to numerical values:
            Integer id = new Integer(Integer.parseInt(loggerID));
            Integer type = new Integer(Integer.parseInt(loggerType));

            Map parameters = new HashMap();
            parameters.put("loggerId", id);
            parameters.put("loggerType", type);
            parameters.put("uri", uri);
            parameters.put("operation", operation);
            parameters.put("resourceId", resourceId == -1 ? null : new Integer(resourceId));
            parameters.put("collection", collection ? "Y" : "N");
            parameters.put("uri", uri);
            parameters.put("uriWildcard", SqlDaoUtils.getUriSqlWildcard(uri));

            String sqlMap = null;
            if (collection && recurse) {
                sqlMap = getSqlMap("insertChangelogEntriesRecursively");
            } else {
                sqlMap = getSqlMap("insertChangelogEntry");
            }
            this.sqlMapClient.update(sqlMap, parameters);
            this.sqlMapClient.commitTransaction();

        } catch (NumberFormatException e) {
            logger.warn("No changelog entry added! Only numerical types and " +
                "IDs are supported by this database backend.");
        } catch (SQLException e) {
            logger.warn("Error occurred while adding changelog entry: " + operation
                        + " for resource: " + uri, e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                this.sqlMapClient.endTransaction();
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }
    }


    public String[] discoverLocks(String uri) throws IOException {
        try {
            this.sqlMapClient.startTransaction();

            Map parameters = new HashMap();
            parameters.put("uriWildcard", SqlDaoUtils.getUriSqlWildcard(uri));
            parameters.put("timestamp", new Date());

            String sqlMap = getSqlMap("discoverLocks");
            List list = this.sqlMapClient.queryForList(sqlMap, parameters);

            String[] locks = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                locks[i] = (String) ((Map) list.get(i)).get("uri");
            }
            this.sqlMapClient.commitTransaction();
            return locks;

        } catch (SQLException e) {
            logger.warn("Error occurred while discovering locks below resource: "
                        + uri, e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                this.sqlMapClient.endTransaction();
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }
    }


    public String[] listSubTree(ResourceImpl parent) throws IOException {

        try {
            this.sqlMapClient.startTransaction();

            Map parameters = new HashMap();
            parameters.put("uriWildcard", SqlDaoUtils.getUriSqlWildcard(parent.getURI()));
            String sqlMap = getSqlMap("listSubTree");
            List list = this.sqlMapClient.queryForList(sqlMap, parameters);
            String[] uris = new String[list.size()];
            int n = 0;
            for (Iterator i = list.iterator(); i.hasNext();) {
                Map map = (Map) i.next();
                uris[n++] = (String) map.get("uri");
            }
            this.sqlMapClient.commitTransaction();
            return uris;

        } catch (SQLException e) {
            logger.warn("Error occurred while listing sub tree of resource: "
                        + parent.getURI(), e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                this.sqlMapClient.endTransaction();
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }

    }


    public void store(ResourceImpl r) throws IOException {
        try {
            this.sqlMapClient.startTransaction();

            String sqlMap = getSqlMap("loadResourceByUri");
            boolean existed = this.sqlMapClient.queryForObject(sqlMap, r.getURI()) != null;

            Map parameters = getResourceAsMap(r);
            parameters.put("depth", new Integer(
                               SqlDaoUtils.getUriDepth(r.getURI())));

            sqlMap = existed ? getSqlMap("updateResource") : getSqlMap("insertResource");
            this.sqlMapClient.update(sqlMap, parameters);

            if (!existed) {
                sqlMap = getSqlMap("loadResourceIdByUri");
                Map map = (Map) this.sqlMapClient.queryForObject(sqlMap, r.getURI());
                Integer id = (Integer) map.get("resourceId");
                r.setID(id.intValue());

                this.contentStore.createResource(r.getURI(), r.isCollection());
            } 

            if (r.getAcl().isDirty()) {
                if (existed) {
                    // Save the ACL:
                    updateAcl(r);
                } else {
                    insertAcl(r);
                }
            }

            storeLock(r);
            storeProperties(r);

            this.sqlMapClient.commitTransaction();

        } catch (SQLException e) {
            logger.warn("Error occurred while storing resource: " + r.getURI(), e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                this.sqlMapClient.endTransaction();
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }
    }



    public void delete(ResourceImpl resource) throws IOException {
        try {
            this.sqlMapClient.startTransaction();

            Map parameters = new HashMap();
            parameters.put("uri", resource.getURI());
            parameters.put("uriWildcard",
                           SqlDaoUtils.getUriSqlWildcard(resource.getURI()));
            
            String sqlMap = getSqlMap("deleteAclEntriesByUri");
            this.sqlMapClient.update(sqlMap, parameters);

            sqlMap = getSqlMap("deleteLocksByUri");
            this.sqlMapClient.update(sqlMap, parameters);

            sqlMap = getSqlMap("deletePropertiesByUri");
            this.sqlMapClient.update(sqlMap, parameters);

            sqlMap = getSqlMap("deleteResourceByUri");
            this.sqlMapClient.update(sqlMap, parameters);

            contentStore.deleteResource(resource.getURI());
            
            this.sqlMapClient.commitTransaction();

        } catch (SQLException e) {
            logger.warn("Error occurred while deleting resource: "
                        + resource.getURI(), e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                this.sqlMapClient.endTransaction();
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }
    }


    public ResourceImpl[] loadChildren(ResourceImpl parent) throws IOException {
        try {
            this.sqlMapClient.startTransaction();

            Map parameters = new HashMap();
            parameters.put("uriWildcard", SqlDaoUtils.getUriSqlWildcard(
                               parent.getURI()));
            parameters.put("depth", new Integer(SqlDaoUtils.getUriDepth(
                                                    parent.getURI()) + 1));

            List children = new ArrayList();
            String sqlMap = getSqlMap("loadChildren");
            List resources = (List) this.sqlMapClient.queryForList(sqlMap, parameters);
            Map locks = loadLocksForChildren(parent);

            for (Iterator i = resources.iterator(); i.hasNext();) {
                Map resourceMap = (Map) i.next();
                String uri = (String) resourceMap.get("uri");

                ResourceImpl resource = new ResourceImpl(uri, this.propertyManager,
                                                         this.authorizationManager);

                populateStandardProperties(this.propertyManager, this.principalManager,
                                           resource, resourceMap);
            
                if (locks.containsKey(uri)) {
                    resource.setLock((LockImpl) locks.get(uri));
                }

                children.add(resource);
            }

            ResourceImpl[] result = (ResourceImpl[]) children.toArray(
                new ResourceImpl[children.size()]);
            loadChildUrisForChildren(parent, result);
            loadACLs(result);
            loadPropertiesForChildren(parent, result);

            this.sqlMapClient.commitTransaction();
            return result;
        
        } catch (SQLException e) {
            logger.warn("Error occurred while loading children of resource: "
                        + parent.getURI(), e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                this.sqlMapClient.endTransaction();
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }
    }


    public String[] discoverACLs(String uri) throws IOException {
        try {
            this.sqlMapClient.startTransaction();

            Map parameters = new HashMap();
            parameters.put("uriWildcard", SqlDaoUtils.getUriSqlWildcard(uri));

            String sqlMap = getSqlMap("discoverAcls");
            List uris = this.sqlMapClient.queryForList(sqlMap, parameters);
            
            String[] result = new String[uris.size()];
            int n = 0;
            for (Iterator i = uris.iterator(); i.hasNext();) {
                Map map = (Map) i.next();
                result[n++] = (String) map.get("uri");
            }
            this.sqlMapClient.commitTransaction();
            return result;

        } catch (SQLException e) {
            logger.warn("Error occurred while discovering ACLs below resource: " + uri, e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                this.sqlMapClient.endTransaction();
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }

    }
    
    private void supplyFixedProperties(Map parameters, PropertySet properties) {
        List propertyList = properties.getProperties(Namespace.DEFAULT_NAMESPACE);
        for (Iterator i = propertyList.iterator(); i.hasNext();) {
            Property property = (Property) i.next();
            if (PropertyType.SPECIAL_PROPERTIES_SET.contains(property.getName())) {
                Object value = property.getValue().getObjectValue();
                if (property.getValue().getType() == PropertyType.TYPE_PRINCIPAL) {
                    value = ((Principal) value).getQualifiedName();
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Copy: fixed property: " + property.getName() + ": " + value);
                }
                parameters.put(property.getName(), value);
            }
        }
    }
    
    
    public void copy(ResourceImpl resource, String destURI, boolean copyACLs,
                     PropertySet fixedProperties) throws IOException {
        try {
            this.sqlMapClient.startTransaction();

            int depthDiff = SqlDaoUtils.getUriDepth(destURI)
                - SqlDaoUtils.getUriDepth(resource.getURI());
    
            Map parameters = new HashMap();
            parameters.put("uri", resource.getURI());
            parameters.put("uriWildcard", SqlDaoUtils.getUriSqlWildcard(resource.getURI()));
            parameters.put("destUri", destURI);
            parameters.put("destUriWildcard", SqlDaoUtils.getUriSqlWildcard(destURI));
            parameters.put("depthDiff", new Integer(depthDiff));

            if (fixedProperties != null) {
                supplyFixedProperties(parameters, fixedProperties);
            }

            String sqlMap = getSqlMap("copyResource");
            this.sqlMapClient.update(sqlMap, parameters);

            sqlMap = getSqlMap("copyProperties");
            this.sqlMapClient.update(sqlMap, parameters);

            if (copyACLs) {

                sqlMap = getSqlMap("copyAclEntries");
                this.sqlMapClient.update(sqlMap, parameters);
            
                // Update inheritance to nearest node:
                int srcNearestACL = findNearestACL(resource.getURI());
                int destNearestACL = findNearestACL(destURI);

                parameters = new HashMap();
                parameters.put("uri", destURI);
                parameters.put("uriWildcard", SqlDaoUtils.getUriSqlWildcard(destURI));
                parameters.put("inheritedFrom", new Integer(destNearestACL));
                parameters.put("previouslyInheritedFrom", new Integer(srcNearestACL));

                sqlMap = getSqlMap("updateAclInheritedFromByPreviousInheritedFromAndUri");
                int n = this.sqlMapClient.update(sqlMap, parameters);

                if (this.optimizedAclCopySupported) {
                    sqlMap = getSqlMap("updateAclInheritedFromByPreviousResourceId");
                    n = this.sqlMapClient.update(sqlMap, parameters);
                } else {
                    sqlMap = getSqlMap("loadPreviousInheritedFromMap");
                    List list = this.sqlMapClient.queryForList(sqlMap, parameters);
                    this.sqlMapClient.startBatch();
                    for (Iterator i = list.iterator(); i.hasNext();) {
                        Map map = (Map) i.next();
                        sqlMap = getSqlMap("updateAclInheritedFromByResourceId");
                        this.sqlMapClient.update(sqlMap, map);
                    }
                    this.sqlMapClient.executeBatch();
                }

            } else {
                Integer nearestAclNode = new Integer(findNearestACL(destURI));
                parameters = new HashMap();
                parameters.put("uri", destURI);
                parameters.put("uriWildcard", SqlDaoUtils.getUriSqlWildcard(destURI));
                parameters.put("inheritedFrom", nearestAclNode);

                sqlMap = getSqlMap("updateAclInheritedFromByUri");
                this.sqlMapClient.update(sqlMap, parameters);
            }

            parameters = new HashMap();
            parameters.put("uri", destURI);
            parameters.put("uriWildcard", SqlDaoUtils.getUriSqlWildcard(destURI));
            sqlMap = getSqlMap("clearPrevResourceIdByUri");
            this.sqlMapClient.update(sqlMap, parameters);

            contentStore.copy(resource.getURI(), destURI);

            this.sqlMapClient.commitTransaction();
        } catch (SQLException e) {
            logger.warn("Error occurred while copying resource: " + resource.getURI()
                        + " to: " + destURI, e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                this.sqlMapClient.endTransaction();
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }
    }



    private void loadChildUris(ResourceImpl parent) throws SQLException {
        Map parameters = new HashMap();
        parameters.put("uriWildcard",
                       SqlDaoUtils.getUriSqlWildcard(parent.getURI()));
        parameters.put("depth", new Integer(SqlDaoUtils.getUriDepth(
                                                parent.getURI()) + 1));

        String sqlMap = getSqlMap("loadChildUrisForChildren");
        List resourceList = this.sqlMapClient.queryForList(sqlMap, parameters);
        
        String[] childUris = new String[resourceList.size()];
        int n = 0;
        for (Iterator i = resourceList.iterator(); i.hasNext();) {
            Map map = (Map) i.next();
            childUris[n++] = (String) map.get("uri");
        }

        parent.setChildURIs(childUris);
    }
    

    private void loadChildUrisForChildren(ResourceImpl parent, ResourceImpl[] children)
        throws SQLException {
        
        // Initialize a map from child.URI to the set of grandchildren's URIs:
        Map childMap = new HashMap();
        for (int i = 0; i < children.length; i++) {
            childMap.put(children[i].getURI(), new HashSet());
        }

        Map parameters = new HashMap();
        parameters.put("uriWildcard",
                       SqlDaoUtils.getUriSqlWildcard(parent.getURI()));
        parameters.put("depth", new Integer(SqlDaoUtils.getUriDepth(
                                                parent.getURI()) + 2));

        String sqlMap = getSqlMap("loadChildUrisForChildren");
        List resourceUris = this.sqlMapClient.queryForList(sqlMap, parameters);

        for (Iterator i = resourceUris.iterator(); i.hasNext();) {
            Map map = (Map) i.next();
            String uri = (String) map.get("uri");
            String parentUri = URIUtil.getParentURI(uri);
            ((Set) childMap.get(parentUri)).add(uri);
        }

        for (int i = 0; i < children.length; i++) {
            if (!children[i].isCollection()) continue;
            Set childURIs = (Set) childMap.get(children[i].getURI());
            children[i].setChildURIs((String[]) childURIs.toArray(
                                         new String[childURIs.size()]));
        }
    }
    

    private void loadPropertiesForChildren(ResourceImpl parent, ResourceImpl[] resources)
            throws SQLException {
        if ((resources == null) || (resources.length == 0)) {
            return;
        }

        Map resourceMap = new HashMap();

        for (int i = 0; i < resources.length; i++) {
            resourceMap.put(new Integer(resources[i].getID()), resources[i]);
        }
        
        Map parameters = new HashMap();
        parameters.put("uriWildcard",
                       SqlDaoUtils.getUriSqlWildcard(parent.getURI()));
        parameters.put("depth", new Integer(SqlDaoUtils.getUriDepth(
                                                parent.getURI()) + 1));

        String sqlMap = getSqlMap("loadPropertiesForChildren");
        List propertyList = this.sqlMapClient.queryForList(sqlMap, parameters);

        populateCustomProperties(resources, propertyList);
    }



    private Map loadLocks(String[] uris) throws SQLException {
        if (uris.length == 0) return new HashMap();
        Map parameters = new HashMap();
        parameters.put("uris", java.util.Arrays.asList(uris));
        parameters.put("timestamp", new Date());
        String sqlMap = getSqlMap("loadLocksByUris");
        List locks = this.sqlMapClient.queryForList(sqlMap, parameters);
        Map result = new HashMap();

        for (Iterator i = locks.iterator(); i.hasNext();) {
            Map map = (Map) i.next();
            LockImpl lock = new LockImpl(
                (String) map.get("token"),
                this.principalManager.getUserPrincipal((String) map.get("owner")),
                (String) map.get("ownerInfo"),
                (String) map.get("depth"),
                (Date) map.get("timeout"));
            
            result.put(map.get("uri"), lock);
        }
        return result;
    }
    

    private Map loadLocksForChildren(ResourceImpl parent) throws SQLException {

        Map parameters = new HashMap();
        parameters.put("timestamp", new Date());
        parameters.put("uriWildcard", SqlDaoUtils.getUriSqlWildcard(parent.getURI()));
        parameters.put("depth", new Integer(SqlDaoUtils.getUriDepth(
                                                parent.getURI()) + 1));
        
        String sqlMap = getSqlMap("loadLocksForChildren");
        List locks = this.sqlMapClient.queryForList(sqlMap, parameters);
        Map result = new HashMap();

        for (Iterator i = locks.iterator(); i.hasNext();) {
            Map map = (Map) i.next();
            LockImpl lock = new LockImpl(
                (String) map.get("token"),
                this.principalManager.getUserPrincipal((String) map.get("owner")),
                (String) map.get("ownerInfo"),
                (String) map.get("depth"),
                (Date) map.get("timeout"));
            
            result.put(map.get("uri"), lock);
        }
        return result;
    }
    


    private void insertAcl(ResourceImpl r) throws SQLException {
        Map actionTypes = loadActionTypes();

        Acl newAcl = r.getAcl();
        Set actions = newAcl.getActions();
        
        String sqlMap = getSqlMap("insertAclEntry");

        this.sqlMapClient.startBatch();
        for (Iterator i = actions.iterator(); i.hasNext();) {
            String action = (String) i.next();
            
            for (Iterator j = newAcl.getPrincipalSet(action).iterator(); j.hasNext();) {
                Principal p = (Principal) j.next();

                Integer actionID = (Integer) actionTypes.get(action);
                if (actionID == null) {
                    throw new SQLException("insertAcl(): Unable to "
                                           + "find id for action '" + action + "'");
                }

                Map parameters = new HashMap();
                parameters.put("actionId", actionID);
                parameters.put("resourceId", new Integer(r.getID()));
                parameters.put("principal", p.getQualifiedName());
                parameters.put("isUser", p.getType() == Principal.TYPE_GROUP ? "N" : "Y");
                parameters.put("grantedBy", r.getOwner().getQualifiedName());
                parameters.put("grantedDate", new Date());

                this.sqlMapClient.update(sqlMap, parameters);
            }

        }

        this.sqlMapClient.executeBatch();
    }
    


    private void updateAcl(ResourceImpl r) throws SQLException {
        // XXX: ACL inheritance checking does not belong here!?
        Acl newAcl = r.getAcl();
        boolean wasInherited = isInheritedAcl(r);
        if (wasInherited && newAcl.isInherited()) {
                return;
        } 

        if (wasInherited) {

            // ACL was inherited, new ACL is not inherited:
            int oldInheritedFrom = findNearestACL(r.getURI());

            insertAcl(r);
            
            Map parameters = new HashMap();
            parameters.put("resourceId", new Integer(r.getID()));
            parameters.put("inheritedFrom", null);

            String sqlMap = getSqlMap("updateAclInheritedFromByResourceId");
            this.sqlMapClient.update(sqlMap, parameters);

            parameters = new HashMap();
            parameters.put("previouslyInheritedFrom", new Integer(oldInheritedFrom));
            parameters.put("inheritedFrom", new Integer(r.getID()));
            parameters.put("uri", r.getURI());
            parameters.put("uriWildcard", SqlDaoUtils.getUriSqlWildcard(r.getURI()));
            
            sqlMap = getSqlMap("updateAclInheritedFromByPreviousInheritedFromAndUri");
            this.sqlMapClient.update(sqlMap, parameters);
            return;
        }

        // ACL was not inherited
        // Delete previous ACL entries for resource:
        String sqlMap = getSqlMap("deleteAclEntriesByResourceId");
        this.sqlMapClient.delete(sqlMap, new Integer(r.getID()));

        if (!newAcl.isInherited()) {
            insertAcl(r);

        } else {

            // The new ACL is inherited, update pointers to the
            // previously "nearest" ACL node:
            int nearest = findNearestACL(r.getURI());
            
            Map parameters = new HashMap();
            parameters.put("inheritedFrom", new Integer(nearest));
            parameters.put("resourceId", new Integer(r.getID()));
            parameters.put("previouslyInheritedFrom", new Integer(r.getID()));
            
            sqlMap = getSqlMap("updateAclInheritedFromByResourceIdOrPreviousInheritedFrom");
            this.sqlMapClient.update(sqlMap, parameters);
        }
    }
    

    private Map loadActionTypes() throws SQLException {
        Map actionTypes = new HashMap();

        String sqlMap = getSqlMap("loadActionTypes");
        List list = this.sqlMapClient.queryForList(sqlMap, null);
        for (Iterator i = list.iterator(); i.hasNext();) {
            Map map = (Map) i.next();
            actionTypes.put(map.get("name"), map.get("id"));
        }
        return actionTypes;
    }
    
    private boolean isInheritedAcl(ResourceImpl r) throws SQLException {

        String sqlMap = getSqlMap("isInheritedAcl");
        Map map = (Map) this.sqlMapClient.queryForObject(
            sqlMap, new Integer(r.getID()));
        
        Integer inheritedFrom = (Integer) map.get("inheritedFrom");
        return inheritedFrom != null;
    }       
    


    private int findNearestACL(String uri) throws SQLException {
        List path = java.util.Arrays.asList(URLUtil.splitUriIncrementally(uri));
        Map parameters = new HashMap();
        parameters.put("path", path);
        String sqlMap = getSqlMap("findNearestAclResourceId");
        List list = this.sqlMapClient.queryForList(sqlMap, parameters);
        Map uris = new HashMap();
        for (Iterator i = list.iterator(); i.hasNext();) {
             Map map = (Map) i.next();
             uris.put(map.get("uri"), map.get("resourceId"));
        }

        int nearestResourceId = -1;
        for (Iterator i = path.iterator(); i.hasNext();) {
            String candidateUri = (String) i.next();
            if (uris.containsKey(candidateUri)) {
                nearestResourceId = ((Integer) uris.get(candidateUri)).intValue();
                break;
            }
        }
        if (nearestResourceId == -1) {
            throw new SQLException("Database inconsistency: no acl to inherit "
                                   + "from for resource " + uri);
        }
        return nearestResourceId;
    }
    

    private void loadACLs(ResourceImpl[] resources) throws SQLException {

        if (resources.length == 0) return; 

        List resourceIds = new ArrayList();
        for (int i = 0; i < resources.length; i++) {
            if (resources[i].getAclInheritedFrom() != -1) {
                resourceIds.add(new Integer(resources[i].getAclInheritedFrom()));
            } else {
                resourceIds.add(new Integer(resources[i].getID()));
            }
        }

        Map map = loadAclMap(resourceIds);

        if (map.isEmpty()) {
            throw new SQLException(
                "Database inconsistency: no ACL entries exist for "
                + "resources " + java.util.Arrays.asList(resources));
        }

        for (int i = 0; i < resources.length; i++) {
            AclImpl acl = null;

            if (resources[i].getAclInheritedFrom() != -1) {
                acl = (AclImpl) map.get(new Integer(resources[i].getAclInheritedFrom()));
            } else {
                acl = (AclImpl) map.get(new Integer(resources[i].getID()));
            }

            if (acl == null) {
                throw new SQLException(
                    "Resource " + resources[i] + " has no ACL entry (ac_inherited_from = "
                    + resources[i].getAclInheritedFrom() + ")");
            }

            acl = (AclImpl) acl.clone();
            acl.setInherited(resources[i].isInheritedACL());
            resources[i].setACL(acl);
        }
    }
    


    private Map loadAclMap(List resourceIds) throws SQLException {

        Map resultMap = new HashMap();
        if (resourceIds.isEmpty()) {
            return resultMap;
        }

        Map parameterMap = new HashMap();
        parameterMap.put("resourceIds", resourceIds);

        String sqlMap = getSqlMap("loadAclEntriesByResourceIds");
        List aclEntries = this.sqlMapClient.queryForList(sqlMap, parameterMap);
            

        for (Iterator i = aclEntries.iterator(); i.hasNext();) {
            Map map = (Map) i.next();

            Integer resourceId = (Integer) map.get("resourceId");
            String action = (String) map.get("action");

            AclImpl acl = (AclImpl) resultMap.get(resourceId);
            
            if (acl == null) {
                acl = new AclImpl();
                resultMap.put(resourceId, acl);
            }
            
            boolean isGroup = "N".equals(map.get("isUser"));
            String name = (String) map.get("principal");
            Principal p = null;

            if (isGroup)
                p = principalManager.getGroupPrincipal(name);
            else if (name.startsWith("pseudo:"))
                p = PseudoPrincipal.getPrincipal(name);
            else
                p = principalManager.getUserPrincipal(name);
            acl.addEntry(action, p);
        }
        return resultMap;
    }
    

    private void storeLock(ResourceImpl r) throws SQLException {
        Lock lock = r.getLock();
        if (lock == null) {
            // Delete any old persistent locks
            String sqlMap = getSqlMap("deleteLockByResourceId");
            this.sqlMapClient.delete(sqlMap, new Integer(r.getID()));
        }
        if (lock != null) {
            String sqlMap = getSqlMap("loadLockByLockToken");
            boolean exists = this.sqlMapClient.queryForObject(
                sqlMap, lock.getLockToken()) != null;

            Map parameters = new HashMap();
            parameters.put("lockToken", lock.getLockToken());
            parameters.put("timeout", lock.getTimeout());
            parameters.put("owner", lock.getPrincipal().getQualifiedName());
            parameters.put("ownerInfo", lock.getOwnerInfo());
            parameters.put("depth", lock.getDepth());
            parameters.put("resourceId", new Integer(r.getID()));

            sqlMap = exists ? getSqlMap("updateLock") : getSqlMap("insertLock");
            this.sqlMapClient.update(sqlMap, parameters);
        }
    }
    


    private void storeProperties(ResourceImpl r) throws SQLException {
        
        String sqlMap = getSqlMap("deletePropertiesByResourceId");
        this.sqlMapClient.update(sqlMap, new Integer(r.getID()));

        List properties = r.getProperties();
        
        if (properties != null) {

            sqlMap = getSqlMap("insertPropertyEntry");

            this.sqlMapClient.startBatch();

            for (Iterator iter = properties.iterator(); iter.hasNext();) {
                Property property = (Property) iter.next();

                if (!PropertyType.SPECIAL_PROPERTIES_SET.contains(property.getName())) {
                    Map parameters = new HashMap();
                    parameters.put("namespaceUri", property.getNamespace().getUri());
                    parameters.put("name", property.getName());
                    parameters.put("resourceId", new Integer(r.getID()));
                    parameters.put("type", new Integer(
                                       property.getDefinition() != null
                                       ? property.getDefinition().getType()
                                       : PropertyType.TYPE_STRING));
                    
                    if (property.getDefinition() != null
                            && property.getDefinition().isMultiple()) {

                        Value[] values = property.getValues();
                        for (int i = 0; i < values.length; i++) {
                            parameters.put("value",
                                           values[i].getNativeStringRepresentation());
                            
                            this.sqlMapClient.update(sqlMap, parameters);
                        }
                    } else {
                        Value value = property.getValue();
                        parameters.put("value", value.getNativeStringRepresentation());
                        this.sqlMapClient.update(sqlMap, parameters);
                    }
                }
            }
            this.sqlMapClient.executeBatch();
        }
    }
    


    private void populateCustomProperties(ResourceImpl[] resources, List propertyList) {

        Map resourceMap = new HashMap();
        for (int i = 0; i < resources.length; i++) {
            resourceMap.put(new Integer(resources[i].getID()), resources[i]);
        }

        Map propMap = new HashMap();
        for (Iterator i = propertyList.iterator(); i.hasNext();) {
            Map propEntry = (Map) i.next();

            SqlDaoUtils.PropHolder prop = new SqlDaoUtils.PropHolder();
            prop.namespaceUri = (String) propEntry.get("namespaceUri");
            prop.name = (String) propEntry.get("name");
            prop.resourceId = ((Integer) propEntry.get("resourceId")).intValue();
            
            List values = (List) propMap.get(prop);
            if (values == null) {
                values = new ArrayList();
                prop.type = ((Integer) propEntry.get("typeId")).intValue();
                prop.values = values;
                propMap.put(prop, values);
            }
            values.add(propEntry.get("value"));
        }

        for (Iterator i = propMap.keySet().iterator(); i.hasNext();) {
            SqlDaoUtils.PropHolder prop = (SqlDaoUtils.PropHolder) i.next();
            
            Property property = this.propertyManager.createProperty(
                prop.namespaceUri,
                prop.name, (String[]) prop.values.toArray(new String[]{}),
                prop.type);

            ResourceImpl r = (ResourceImpl) resourceMap.get(
                    new Integer(prop.resourceId));
            r.addProperty(property);
        }
    }
    

    public static void populateStandardProperties(
        PropertyManager propertyManager, PrincipalManager principalManager,
        PropertySetImpl propertySet, Map resourceMap) {

        propertySet.setID(((Number)resourceMap.get("id")).intValue());
        
        boolean collection = "Y".equals( resourceMap.get("isCollection"));
        Property prop = propertyManager.createProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.COLLECTION_PROP_NAME,
            new Boolean(collection));
        propertySet.addProperty(prop);
        
        Principal createdBy = principalManager.getUserPrincipal(
            (String) resourceMap.get("createdBy"));
        prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, PropertyType.CREATEDBY_PROP_NAME,
                createdBy);
        propertySet.addProperty(prop);

        prop = propertyManager.createProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.CREATIONTIME_PROP_NAME,
            resourceMap.get("creationTime"));
        propertySet.addProperty(prop);

        Principal principal = principalManager.getUserPrincipal(
            (String) resourceMap.get("owner"));
        prop = propertyManager.createProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.OWNER_PROP_NAME,
            principal);
        propertySet.addProperty(prop);

        String string = (String) resourceMap.get("displayName");
        if (string != null) {
            prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, 
                PropertyType.DISPLAYNAME_PROP_NAME,
                string);
            propertySet.addProperty(prop);
        }
        
        string = (String) resourceMap.get("contentType");
        if (string != null) {
            prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, 
                PropertyType.CONTENTTYPE_PROP_NAME,
                string);
            propertySet.addProperty(prop);
        }
        
        string = (String) resourceMap.get("characterEncoding");
        if (string != null) {
            prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, 
                PropertyType.CHARACTERENCODING_PROP_NAME,
                string);
            propertySet.addProperty(prop);
        }
        
        string = (String) resourceMap.get("guessedCharacterEncoding");
        if (string != null) {
            prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, 
                PropertyType.CHARACTERENCODING_GUESSED_PROP_NAME,
                string);
            propertySet.addProperty(prop);
        }
        
        string = (String) resourceMap.get("userSpecifiedCharacterEncoding");
        if (string != null) {
            prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, 
                PropertyType.CHARACTERENCODING_USER_SPECIFIED_PROP_NAME,
                string);
            propertySet.addProperty(prop);
        }
        
        string = (String) resourceMap.get("contentLanguage");
        if (string != null) {
            prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, 
                PropertyType.CONTENTLOCALE_PROP_NAME,
                string);
            propertySet.addProperty(prop);
        }

        prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, PropertyType.LASTMODIFIED_PROP_NAME,
                resourceMap.get("lastModified"));
        propertySet.addProperty(prop);

        principal = principalManager.getUserPrincipal((String) resourceMap.get("modifiedBy"));
        prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, PropertyType.MODIFIEDBY_PROP_NAME,
                principal);
        propertySet.addProperty(prop);

        prop = propertyManager.createProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.CONTENTLASTMODIFIED_PROP_NAME,
            resourceMap.get("contentLastModified"));
        propertySet.addProperty(prop);

        principal = principalManager.getUserPrincipal(
            (String) resourceMap.get("contentModifiedBy"));
        prop = propertyManager.createProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.CONTENTMODIFIEDBY_PROP_NAME,
            principal);
        propertySet.addProperty(prop);

        prop = propertyManager.createProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.PROPERTIESLASTMODIFIED_PROP_NAME,
            resourceMap.get("propertiesLastModified"));
        propertySet.addProperty(prop);

        principal = principalManager.getUserPrincipal(
            (String) resourceMap.get("propertiesModifiedBy"));
        prop = propertyManager.createProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.PROPERTIESMODIFIEDBY_PROP_NAME,
            principal);
        propertySet.addProperty(prop);

        if (!collection) {
            //long contentLength = contentStore.getContentLength(propertySet.getURI());
//             long contentLength = rs.getLong("content_length");
            long contentLength = ((Number) resourceMap.get("contentLength")).longValue();
            prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, PropertyType.CONTENTLENGTH_PROP_NAME,
                new Long(contentLength));
            propertySet.addProperty(prop);
        }
        
        propertySet.setResourceType((String) resourceMap.get("resourceType"));

        Integer aclInheritedFrom = (Integer) resourceMap.get("aclInheritedFrom");
        if (aclInheritedFrom == null) {
            aclInheritedFrom = new Integer(-1);
        }

        propertySet.setAclInheritedFrom(aclInheritedFrom.intValue());
    }


    

    private Map getResourceAsMap(ResourceImpl r) {
        Map map = new HashMap();
        map.put("parent", r.getParent());
        map.put("uri", r.getURI());
        map.put("lastModified", r.getLastModified());
        map.put("contentLastModified", r.getContentLastModified());
        map.put("propertiesLastModified", r.getPropertiesLastModified());
        map.put("creationTime", r.getCreationTime());
        map.put("modifiedBy", r.getModifiedBy().getQualifiedName());
        map.put("createdBy", r.getCreatedBy().getQualifiedName());
        map.put("owner", r.getOwner().getQualifiedName());
        map.put("contentModifiedBy", r.getContentModifiedBy().getQualifiedName());
        map.put("propertiesModifiedBy", r.getPropertiesModifiedBy().getQualifiedName());
        map.put("collection", r.isCollection() ? "Y" : "N");
        map.put("displayName", r.getDisplayName());
        map.put("contentType", r.getContentType());
        map.put("characterEncoding", r.getCharacterEncoding());
        map.put("guessedCharacterEncoding", r.getGuessedCharacterEncoding());
        map.put("userSpecifiedCharacterEncoding", r.getUserSpecifiedCharacterEncoding());
        map.put("contentLanguage", r.getContentLanguage());
        map.put("resourceType", r.getResourceType());
        map.put("contentLength", new Long(r.getContentLength()));
        return map;
    }
    

    private String getSqlMap(String statementId) {
        if (this.sqlMaps.containsKey(statementId)) {
            return (String) this.sqlMaps.get(statementId);
        }
        return statementId;
    }
    
}

