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

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.vortikal.repository.Acl;
import org.vortikal.repository.Lock;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repositoryimpl.AclImpl;
import org.vortikal.repositoryimpl.LockImpl;
import org.vortikal.repositoryimpl.ResourceImpl;
import org.vortikal.security.Principal;
import org.vortikal.util.repository.URIUtil;
import org.vortikal.util.web.URLUtil;

/**
 * This class is going to be a "generic" JDBC database accessor. Currently, only
 * PostgreSQL is supported. 
 *
 */
public class JDBCClient extends AbstractDataAccessor {

    private QueryProvider queryProvider = new QueryProvider();
    
    public void setQueryProvider(QueryProvider queryProvider) {
        if (queryProvider == null) {
            throw new IllegalArgumentException("Query provider cannot be null");
        }

        this.queryProvider = queryProvider;
    }
    


    protected boolean validate(Connection conn) throws SQLException {
        boolean exists = false;

        exists = ((tableExists(conn, "vortex_resource") || tableExists(conn,
                                                                       "VORTEX_RESOURCE"))
                  && (tableExists(conn, "lock_type") || tableExists(conn, "LOCK_TYPE"))
                  && (tableExists(conn, "vortex_lock") || tableExists(conn,
                                                                      "VORTEX_LOCK"))
                  && (tableExists(conn, "action_type") || tableExists(conn,
                                                                      "ACTION_TYPE"))
                  && (tableExists(conn, "acl_entry") || tableExists(conn, "ACL_ENTRY"))
                  && (tableExists(conn, "extra_prop_entry") || tableExists(conn, "EXTRA_PROP_ENTRY")));
        return exists;
    }


    private boolean tableExists(Connection conn, String tableName)
            throws SQLException {
        DatabaseMetaData md = conn.getMetaData();

        ResultSet rs = md.getTables(null, null, tableName, null);
        boolean exists = rs.next();
        rs.close();
        return exists;
    }



    protected ResourceImpl load(Connection conn, String uri) throws SQLException {
        
        String query = this.queryProvider.getLoadResourceByUriPreparedStatement();
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, uri);
        
        ResultSet rs = stmt.executeQuery();

        ResourceImpl resource = null;

        if (rs.next()) {
            resource = populateResource(uri, rs);
        }

        rs.close();
        stmt.close();

        if (resource == null) {
            return null;
        }
       
        Map locks = loadLocks(conn, new String[] {uri});
        LockImpl lock = null;
        if (locks.containsKey(uri)) {
            lock = (LockImpl) locks.get(uri);
        }
        resource.setLock(lock);
        resource.setChildURIs(loadChildURIs(conn, resource.getURI()));
        
        loadACLs(conn, new ResourceImpl[] {resource});
        loadProperties(conn, resource);

        return resource;
    }


    private ResourceImpl populateResource(String uri, ResultSet rs) 
        throws SQLException {

        ResourceImpl resource = new ResourceImpl(uri, propertyManager);
        resource.setID(rs.getInt("resource_id"));
        
        int aclInheritedFrom =  rs.getInt("acl_inherited_from");
        if (rs.wasNull()) {
            aclInheritedFrom = -1;
        }
        resource.setAclInheritedFrom(aclInheritedFrom);
        
        boolean collection = rs.getString("is_collection").equals("Y");
        Property prop = this.propertyManager.createProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.COLLECTION_PROP_NAME,
            new Boolean(collection));
        resource.addProperty(prop);
        
        Principal createdBy = principalManager.getUserPrincipal(rs.getString("created_by"));
        prop = this.propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, PropertyType.CREATEDBY_PROP_NAME,
                createdBy);
        resource.addProperty(prop);

        prop = this.propertyManager.createProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.CREATIONTIME_PROP_NAME,
            new Date(rs.getTimestamp("creation_time").getTime()));
        resource.addProperty(prop);

        Principal principal = principalManager.getUserPrincipal(rs.getString("resource_owner"));
        prop = this.propertyManager.createProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.OWNER_PROP_NAME,
            principal);
        resource.addProperty(prop);

        String string = rs.getString("display_name");
        if (string != null) {
            prop = this.propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, 
                PropertyType.DISPLAYNAME_PROP_NAME,
                string);
            resource.addProperty(prop);
        }
        
        string = rs.getString("content_type");
        if (string != null) {
            prop = this.propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, 
                PropertyType.CONTENTTYPE_PROP_NAME,
                string);
            resource.addProperty(prop);
        }
        
        string = rs.getString("character_encoding");
        if (string != null) {
            prop = this.propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, 
                PropertyType.CHARACTERENCODING_PROP_NAME,
                string);
            resource.addProperty(prop);
        }
        
        string = rs.getString("content_language");
        if (string != null) {
            prop = this.propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, 
                PropertyType.CONTENTLOCALE_PROP_NAME,
                string);
            resource.addProperty(prop);
        }

        prop = this.propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, PropertyType.LASTMODIFIED_PROP_NAME,
                new Date(rs.getTimestamp("last_modified").getTime()));
        resource.addProperty(prop);

        principal = principalManager.getUserPrincipal(rs.getString("modified_by"));
        prop = this.propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, PropertyType.MODIFIEDBY_PROP_NAME,
                principal);
        resource.addProperty(prop);

        prop = this.propertyManager.createProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.CONTENTLASTMODIFIED_PROP_NAME,
            new Date(rs.getTimestamp("content_last_modified").getTime()));
        resource.addProperty(prop);

        principal = principalManager.getUserPrincipal(rs.getString("content_modified_by"));
        prop = this.propertyManager.createProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.CONTENTMODIFIEDBY_PROP_NAME,
            principal);
        resource.addProperty(prop);

        prop = this.propertyManager.createProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.PROPERTIESLASTMODIFIED_PROP_NAME,
            new Date(rs.getTimestamp("properties_last_modified").getTime()));
        resource.addProperty(prop);

        principal = principalManager.getUserPrincipal(rs.getString("properties_modified_by"));
        prop = this.propertyManager.createProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.PROPERTIESMODIFIEDBY_PROP_NAME,
            principal);
        resource.addProperty(prop);

        if (!collection) {
            long contentLength = contentStore.getContentLength(resource.getURI());
            prop = this.propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, PropertyType.CONTENTLENGTH_PROP_NAME,
                new Long(contentLength));
            resource.addProperty(prop);
        }
        
        resource.setResourceType(rs.getString("resource_type"));
        
        return resource;
    }


    
    private String getURIWildcard(String uri) {
        if ("/".equals(uri)) {
            return "/%";
        }
        return uri + "/%";
    }
    

    private int getURIDepth(String uri) {
        if ("/".equals(uri)) {
            return 0;
        }
        int count = 0;
        for (int index = 0; (index = uri.indexOf('/', index)) != -1; count++, index ++);
        return count;
    }

    /**
     * @deprecated
     */
//    private void loadProperties(Connection conn, ResourceImpl resource)
//            throws SQLException {
//
//        String query = this.queryProvider.getLoadPropertiesByResourceIdPreparedStatement();
//        PreparedStatement propStmt = conn.prepareStatement(query);
//        propStmt.setInt(1, resource.getID());
//        ResultSet rs = propStmt.executeQuery();
//
//        while (rs.next()) {
//
//            Namespace ns = Namespace.getNamespace(rs.getString("name_space"));
//            String name = rs.getString("name");
//            String value = rs.getString("value");
//
//            Property prop = this.propertyManager.createProperty(ns, name, value);
//            resource.addProperty(prop);
//        }
//
//        rs.close();
//        propStmt.close();
//    }
    
    /**
     * Small helper-class for multi-valued property loading 
     */
    private class PropHolder {
        String namespaceUri = "";
        String name = "";
        int type;
        int resourceId;
        List values;
        
        public boolean equals(Object other) {
            if (other == null) return false;
            
            if (other == this) return true;
            
            return this.namespaceUri.equals(((PropHolder)other).namespaceUri) &&
                   this.name.equals(((PropHolder)other).name)                 &&
                   (this.resourceId == ((PropHolder)other).resourceId);
        }
        
        public int hashCode() {
            return this.namespaceUri.hashCode() 
                 + this.name.hashCode() + this.resourceId;
        }
    }
    
    /**
     * Returns an array of PropHolder objects from the ResultSet, merging
     * multi-valued props into a single PropHolder with value-list.
     * 
     * Does not close ResultSet.
     */
    private PropHolder[] getPropHoldersFromResultSet(ResultSet rs) throws SQLException {
        Map propMap = new HashMap();
        while (rs.next()) {
            PropHolder prop = new PropHolder();
            prop.namespaceUri = rs.getString("name_space");
            prop.name = rs.getString("name");
            prop.resourceId = rs.getInt("resource_id");
            
            List values = (List)propMap.get(prop);
            if (values == null) {
                values = new ArrayList();
                prop.type = rs.getInt("prop_type_id");
                prop.values = values;
                propMap.put(prop, values);
            }
            values.add(rs.getString("value"));
        }
        
        return (PropHolder[])propMap.keySet().toArray(new PropHolder[]{});
    }

    /**
     * New loadProperties with multi-value and type support.
     * @param conn
     * @param resource
     * @throws SQLException
     */
    private void loadProperties(Connection conn, ResourceImpl resource)
        throws SQLException {
        
        String query = this.queryProvider.getLoadPropertiesByResourceIdPreparedStatement();
        PreparedStatement pstmt = conn.prepareStatement(query);
        pstmt.setInt(1, resource.getID());
        ResultSet rs = pstmt.executeQuery();

        PropHolder[] propHolders = getPropHoldersFromResultSet(rs);
        
        rs.close();
        pstmt.close();
        
        for (int i=0; i<propHolders.length; i++) {
            PropHolder prop = propHolders[i];

            Property property = this.propertyManager.createProperty(
                    Namespace.getNamespace(prop.namespaceUri), prop.name, 
                    (String[])prop.values.toArray(new String[]{}), prop.type);
            resource.addProperty(property);
        }
        
    }
    
    private void loadPropertiesForChildren(Connection conn, ResourceImpl parent,
            ResourceImpl[] resources)
            throws SQLException {
        if ((resources == null) || (resources.length == 0)) {
            return;
        }

        String query = this.queryProvider.getLoadPropertiesForChildrenPreparedStatement();

        PreparedStatement propStmt = conn.prepareStatement(query);
        propStmt.setString(1, getURIWildcard(parent.getURI()));
        propStmt.setInt(2, getURIDepth(parent.getURI()) + 1);
        
        ResultSet rs = propStmt.executeQuery();
        Map resourceMap = new HashMap();

        for (int i = 0; i < resources.length; i++) {
            resourceMap.put(new Integer(resources[i].getID()), resources[i]);
        }
        
        PropHolder[] propHolders = getPropHoldersFromResultSet(rs);
        rs.close();
        propStmt.close();
        
        for (int i=0; i<propHolders.length; i++) {
            PropHolder holder = propHolders[i];
                        
            Property property = this.propertyManager.createProperty(
                    Namespace.getNamespace(holder.namespaceUri),
                    holder.name, (String[])holder.values.toArray(new String[]{}),
                    holder.type);
            
            ResourceImpl r = (ResourceImpl)resourceMap.get(
                    new Integer(holder.resourceId));
            r.addProperty(property);
            
        }
        
//        while (rs.next()) {
//            // FIXME: type conversion
//            Integer resourceID = new Integer((int) rs.getLong("resource_id"));
//
//            Property prop = this.propertyManager.createProperty(
//                    Namespace.getNamespace(rs.getString("name_space")), 
//                    rs.getString("name"), rs.getString("value"));
//            ResourceImpl r = (ResourceImpl) resourceMap.get(resourceID);
//            r.addProperty(prop);
//        }
//
//        rs.close();
//        propStmt.close();
//
    }

    protected void deleteExpiredLocks(Connection conn)
            throws SQLException {
        String query = this.queryProvider.getDeleteExpiredLocksPreparedStatement();
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setDate(1, new java.sql.Date(System.currentTimeMillis()));
        int n = stmt.executeUpdate();
        if (logger.isInfoEnabled() && n > 0) {
            logger.info("Deleted " + n + " expired locks");
        }
        stmt.close();
    }


    protected void addChangeLogEntry(Connection conn, String loggerID, String loggerType,
                                     String uri, String operation, int resourceId,
                                     boolean collection, boolean recurse) throws SQLException {
        if (collection && recurse) {

            int id = Integer.parseInt(loggerID);
            int type = Integer.parseInt(loggerType);

            String statement = this.queryProvider.getInsertRecursiveChangeLogEntryStatement(
                resourceId, uri, getURIWildcard(uri), id, type, operation);
            
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(statement);
            stmt.close();

        } else {


            try {
                int id = Integer.parseInt(loggerID);
                int type = Integer.parseInt(loggerType);

                String statement = this.queryProvider.getInsertChangeLogEntryPreparedStatement();
                PreparedStatement stmt = conn.prepareStatement(statement);

                stmt.setInt(1, id);
                stmt.setInt(2, type);
                stmt.setString(3, operation);
                stmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                stmt.setString(5, uri);

                // resourceId of -1 indicates that resourceId should be set to SQL NULL
                if (resourceId == -1) {
                    stmt.setNull(6, java.sql.Types.NUMERIC);
                } else {
                    stmt.setInt(6, resourceId);
                }
            
                stmt.setString(7, collection ? "Y" : "N");
            
                stmt.executeUpdate();
                stmt.close();
            } catch (NumberFormatException e) {
                logger.warn("No changelog entry added! Only numerical types and "
                            + "IDs are supported by this database backend.");
            }
        }
    }


    protected String[] discoverLocks(Connection conn, String uri)
            throws SQLException {
        String query = this.queryProvider.getDiscoverLocksByResourceIdPreparedStatement();
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, getURIWildcard(uri));

        ResultSet rs = stmt.executeQuery();
        List result = new ArrayList();

        while (rs.next()) {
            String lockURI = rs.getString("uri");

            result.add(lockURI);
        }

        rs.close();
        stmt.close();

        return (String[]) result.toArray(new String[result.size()]);
    }

    private LockImpl loadLock(Connection conn, String uri)
            throws SQLException {
        String query = this.queryProvider.getLoadLockByResourceUriPreparedStatement();
        PreparedStatement stmt = conn.prepareStatement(query);

        stmt.setString(1, uri);

        ResultSet rs = stmt.executeQuery();

        LockImpl lock = null;

        if (rs.next()) {
            lock = new LockImpl(rs.getString("token"), 
                    principalManager.getUserPrincipal(rs.getString("lock_owner")), 
                    rs.getString("lock_owner_info"), rs.getString("depth"), 
                    new Date(rs.getTimestamp("timeout").getTime()));
        }

        rs.close();
        stmt.close();

        return lock;
    }

    private Map loadLocks(Connection conn, String[] uris)
            throws SQLException {
        if (uris.length == 0) {
            return new HashMap();
        }

        String query = this.queryProvider.getLoadLocksByResourceUrisPreparedStatement(uris);
        PreparedStatement stmt = conn.prepareStatement(query);

        for (int i = 0; i < uris.length; i++) {
            stmt.setString(i + 1, uris[i]);
        }

        ResultSet rs = stmt.executeQuery();
        Map result = new HashMap();

        while (rs.next()) {
            LockImpl lock = new LockImpl(rs.getString("token"), 
                    principalManager.getUserPrincipal(rs.getString("lock_owner")),
                rs.getString("lock_owner_info"), rs.getString("depth"),
                new Date(rs.getTimestamp("timeout").getTime()));

            if (lock.getTimeout().getTime() > System.currentTimeMillis()) {
                result.put(rs.getString("uri"), lock);
            }
        }

        rs.close();
        stmt.close();

        return result;
    }

    protected String[] listSubTree(Connection conn, ResourceImpl parent)
            throws SQLException {
        if (parent.getURI() == null) {
            return new String[] {};
        }

        String query = this.queryProvider.getListSubTreePreparedStatement();
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, getURIWildcard(parent.getURI()));
        ResultSet rs = stmt.executeQuery();
        ArrayList l = new ArrayList();
        while (rs.next()) {
            String childURI = rs.getString("uri");

            l.add(childURI);
        }
        rs.close();
        stmt.close();

        return (String[]) l.toArray(new String[] {});
    }

    protected void store(Connection conn, ResourceImpl r)
            throws SQLException, IOException {

        String uri = r.getURI();
        String parent = r.getParent();

        Date lastModified = r.getLastModified();
        Date contentLastModified = r.getContentLastModified();
        Date propertiesLastModified = r.getPropertiesLastModified();
        Date creationTime = r.getCreationTime();

        String modifiedBy = r.getModifiedBy().getQualifiedName();
        String createdBy = r.getCreatedBy().getQualifiedName();
        String owner = r.getOwner().getQualifiedName();
        String contentModifiedBy = r.getContentModifiedBy().getQualifiedName();
        String propertiesModifiedBy = r.getPropertiesModifiedBy().getQualifiedName();
        boolean collection = r.isCollection();

        String displayName = r.getDisplayName();
        String contentType = r.getContentType();
        String characterEncoding = r.getCharacterEncoding();
        String contentLanguage = r.getContentLanguage();
        String resourceType = r.getResourceType();
        long contentLength = r.getContentLength();
        
        String query = this.queryProvider.getLoadResourceByUriPreparedStatement();
        PreparedStatement existStmt = conn.prepareStatement(query);
        existStmt.setString(1, uri);

        ResultSet rs = existStmt.executeQuery();
        boolean existed = rs.next();

        rs.close();
        existStmt.close();

        if (existed) {
            // Save the VORTEX_RESOURCE table entry:
            query = this.queryProvider.getUpdateResourcePreparedStatement();
            PreparedStatement stmt = conn.prepareStatement(query);
            
            stmt.setTimestamp(1, new Timestamp(contentLastModified.getTime()));
            stmt.setTimestamp(2, new Timestamp(propertiesLastModified.getTime()));
            stmt.setString(3, contentModifiedBy);
            stmt.setString(4, propertiesModifiedBy);
            stmt.setString(5, owner);
            stmt.setString(6, displayName);
            stmt.setString(7, contentLanguage);
            stmt.setString(8, contentType);
            stmt.setString(9, characterEncoding);
            stmt.setTimestamp(10, new Timestamp(creationTime.getTime()));
            stmt.setString(11, resourceType);

            if (collection) {
                stmt.setNull(12, Types.BIGINT);
            } else {
                stmt.setLong(12, contentLength);
            }
            
            stmt.setString(13, createdBy);
            stmt.setString(14, modifiedBy);
            stmt.setTimestamp(15, new Timestamp(lastModified.getTime()));
            
            stmt.setString(16, uri);

            stmt.executeUpdate();
            stmt.close();
        } else {
            insertResourceEntry(conn, uri, creationTime, contentLastModified,
                    propertiesLastModified, displayName, owner, contentModifiedBy,
                    propertiesModifiedBy, contentLanguage, contentType,
                                characterEncoding, collection, resourceType, contentLength, parent,
                                r.getAclInheritedFrom(), createdBy, modifiedBy, lastModified);
            
            // Temporary hack to get the new resource ID
            // Statement.getGeneratedKeys() is not implemented in the PostgreSQL JDBC driver.
            // storeACL(), storeLock() and storeProperties() depends on the ID.
            // Consequences for caching: Cache should not keep resource as is before
            // passing it on to the wrapped DAO accessor.
            String resourceIdQuery = queryProvider.getLoadResourceIdByUriPreparedStatement();
            PreparedStatement resourceIdStatement = conn.prepareStatement(resourceIdQuery);
            resourceIdStatement.setString(1, uri);
            ResultSet resourceIdResultSet = resourceIdStatement.executeQuery();
            if (resourceIdResultSet.next()) {
                r.setID(resourceIdResultSet.getInt("resource_id"));
            } else {
                throw new SQLException("Could not get generated ID for new resource");
            }
            resourceIdResultSet.close();
            resourceIdStatement.close();
            
        }

        if (r.getAcl().isDirty()) {
            // Save the ACL:
            if (existed)
                storeACL(conn, r);
            else
                insertACLEntries(conn, r);
        }
        
        // Save the Lock:
        storeLock(conn, r);

        // Save properties:
        storeProperties(conn, r);
    }

    private void insertResourceEntry(Connection conn, 
                                     String uri, 
                                     Date creationTime,
                                     Date contentLastModified, 
                                     Date propertiesLastModified, 
                                     String displayname,
                                     String owner, 
                                     String contentModifiedBy, 
                                     String propertiesModifiedBy,
                                     String contentlanguage, 
                                     String contenttype, 
                                     String characterEncoding,
                                     boolean collection, 
                                     String resourceType, 
                                     long contentLength, 
                                     String parent, 
                                     int aclInheritedFrom,
                                     String createdBy,
                                     String modifiedBy,
                                     Date lastModified)
            throws SQLException, IOException {

        int depth = getURIDepth(uri);

        String statement = this.queryProvider.getInsertResourcePreparedStatement();

        PreparedStatement stmt = conn.prepareStatement(statement);

        stmt.setString(1, uri);
        stmt.setString(2, resourceType);
        if (collection) {
            stmt.setNull(3, Types.BIGINT);
        } else {
            stmt.setLong(3, contentLength);
        }
        stmt.setInt(4, depth);
        stmt.setTimestamp(5, new Timestamp(creationTime.getTime()));
        stmt.setTimestamp(6, new Timestamp(contentLastModified.getTime()));
        stmt.setTimestamp(7, new Timestamp(propertiesLastModified.getTime()));
        stmt.setString(8, contentModifiedBy);
        stmt.setString(9, propertiesModifiedBy);
        stmt.setString(10, owner);
        stmt.setString(11, displayname);
        stmt.setString(12, contentlanguage);
        stmt.setString(13, contenttype);
        stmt.setString(14, characterEncoding);
        stmt.setString(15, collection ? "Y" : "N");
        stmt.setInt(16, aclInheritedFrom);

        stmt.setString(17, createdBy);
        stmt.setString(18, modifiedBy);
        stmt.setTimestamp(19, new Timestamp(lastModified.getTime()));

        stmt.executeUpdate();
        stmt.close();

        contentStore.createResource(uri, collection);
    }

    private void storeLock(Connection conn, ResourceImpl r)
            throws SQLException {
        Lock lock = r.getLock();

        String query = null;

        if (lock == null) {
            query = this.queryProvider.getDeleteLockByResourceIdPreparedStatement();
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, r.getID());
            stmt.executeUpdate();
            stmt.close();
        }

        if (lock != null) {
            String lockToken = lock.getLockToken();
            Date timeout = lock.getTimeout();
            String type = lock.getLockType();
            String user = lock.getPrincipal().getQualifiedName();
            String ownerInfo = lock.getOwnerInfo();
            String depth = lock.getDepth();

            query = this.queryProvider.getLoadLockIdByTokenPreparedStatement();
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, lockToken);
            ResultSet rs = stmt.executeQuery();
            boolean exists = rs.next();
            rs.close();
            stmt.close();


            query = this.queryProvider.getLoadLockTypeIdFromNamePreparedStatement();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, type);
            rs = stmt.executeQuery();

            if (!rs.next()) {
                rs.close();
                stmt.close();
                throw new SQLException("Unknown lock type: " + type);
            }

            int lockType = rs.getInt("lock_type_id");

            rs.close();
            stmt.close();

            if (exists) {
                query = this.queryProvider.getUpdateLockPreparedStatement();
                PreparedStatement updateStmt = conn.prepareStatement(query);

                updateStmt.setInt(1, lockType);
                updateStmt.setString(2, user);
                updateStmt.setString(3, ownerInfo);
                updateStmt.setString(4, depth);
                updateStmt.setTimestamp(5, new Timestamp(timeout.getTime()));
                updateStmt.setString(6, lockToken);

                updateStmt.executeUpdate();
                updateStmt.close();
            } else {
                query = this.queryProvider.getInsertLockPreparedStatement();
                PreparedStatement insertStmt = conn.prepareStatement(query);

                insertStmt.setString(1, lockToken);
                insertStmt.setInt(2, r.getID());
                insertStmt.setInt(3, lockType);
                insertStmt.setString(4, user);
                insertStmt.setString(5, ownerInfo);
                insertStmt.setString(6, depth);
                insertStmt.setTimestamp(7, new Timestamp(timeout.getTime()));

                insertStmt.executeUpdate();
                insertStmt.close();
            }
        }
    }

    private void storeACL(Connection conn, ResourceImpl r)
            throws SQLException {
        // XXX: ACL inheritance checking does not belong here!?

        ResourceImpl existingResource = load(conn, r.getURI());
        Acl newAcl = r.getAcl();

        boolean wasInherited = existingResource.isInheritedACL();

        if (wasInherited && newAcl.isInherited()) {
                return;
        } 

        if (wasInherited) {

            int oldInheritedFrom = findNearestACL(conn, r.getURI());

            insertACLEntries(conn, r);
            
            String query = this.queryProvider.getUpdateAclInheritedByResourceIdPreparedStatement();
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setNull(1, java.sql.Types.INTEGER);
            stmt.setInt(2, r.getID());
            stmt.executeUpdate();
            stmt.close();
            
            query = this.queryProvider.getUpdateAclInheritedFromByInheritedPreparedStatement();

            stmt = conn.prepareStatement(query);
            stmt.setInt(1, r.getID());
            stmt.setInt(2, oldInheritedFrom);
            stmt.executeUpdate();
            stmt.close();

            return;
        }


        // Delete previous ACL entries for resource:
        String query = this.queryProvider.getDeleteACLEntryByResourceIdPreparedStatement();
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setInt(1, r.getID());
        stmt.executeUpdate();
        stmt.close();

        if (!newAcl.isInherited()) {

            insertACLEntries(conn, r);

        } else {
            int nearest = findNearestACL(conn, r.getURI());

            query = this.queryProvider.getUpdateAclInheritedByResourceIdOrInheritedPreparedStatement();
            
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, nearest);
            stmt.setInt(2, r.getID());
            stmt.setInt(3, r.getID());
            stmt.executeUpdate();
            stmt.close();
        }
    }

    
    private void insertACLEntries(Connection conn, ResourceImpl resource)
        throws SQLException{

        String query = this.queryProvider.getLoadActionTypesPreparedStatement();
        PreparedStatement stmt = conn.prepareStatement(query);
        ResultSet rs = stmt.executeQuery();
        Map actionTypes = new HashMap();
        
        while (rs.next()) {
            String key = rs.getString("name");
            Integer value = new Integer(rs.getInt("action_type_id"));
            actionTypes.put(key, value);
        }
        rs.close();
        stmt.close();

        

        Acl newAcl = resource.getAcl();
        Set actions = newAcl.getActions();
        
        query = this.queryProvider.getInsertAclEntryPreparedStatement();
        PreparedStatement insertStmt = conn.prepareStatement(query);

        for (Iterator i = actions.iterator(); i.hasNext();) {
            String action = (String) i.next();
            for (Iterator j = newAcl.getPrincipalSet(action).iterator(); j.hasNext();) {
                Principal p = (Principal) j.next();

                Integer actionID = (Integer) actionTypes.get(action);
                if (actionID == null) {
                    throw new SQLException("Database.insertACLEntry(): Unable to "
                                           + "find id for action '" + action + "'");
                }

                insertStmt.setInt(1, actionID.intValue());
                insertStmt.setInt(2, resource.getID());
                insertStmt.setString(3, p.getQualifiedName());
                insertStmt.setString(4, p.getType() == Principal.TYPE_GROUP ? "N" : "Y");

                // XXX: Does this have any point?
                insertStmt.setString(5, resource.getOwner().getQualifiedName());
                insertStmt.setTimestamp(6, new Timestamp(System.currentTimeMillis()));

                insertStmt.addBatch();
            }
        }
        

        insertStmt.executeBatch();
        insertStmt.close();
    }
    

    private void storeProperties(Connection conn, ResourceImpl r)
            throws SQLException {

        String query = this.queryProvider.getDeletePropertiesByResourceIdPreparedStatement();
        PreparedStatement deleteStatement = conn.prepareStatement(query);
        deleteStatement.setInt(1, r.getID());
        deleteStatement.executeUpdate();
        deleteStatement.close();

        List properties = r.getProperties();
        
        if (properties != null) {

            String insertQuery = this.queryProvider.getInsertPropertyEntryPreparedStatement();
            PreparedStatement stmt = conn.prepareStatement(insertQuery);

            for (Iterator iter = properties.iterator(); iter.hasNext();) {
                Property property = (Property) iter.next();

                if (!PropertyType.SPECIAL_PROPERTIES_SET.contains(property.getName())) {
                    String namespaceUri = property.getNamespace().getUri();
                    String name = property.getName();
                    int resourceId = r.getID();
                    int type = property.getDefinition() != null ? property
                            .getDefinition().getType()
                            : PropertyType.TYPE_STRING;

                    if (property.getDefinition() != null
                            && property.getDefinition().isMultiple()) {

                        Value[] values = property.getValues();
                        for (int i = 0; i < values.length; i++) {
                            stmt.setInt(1, resourceId);
                            stmt.setInt(2, type);
                            stmt.setString(3, namespaceUri);
                            stmt.setString(4, name);
                            stmt.setString(5, values[i].getStringRepresentation());
                            stmt.addBatch();
                        }
                    } else {
                        Value value = property.getValue();
                        stmt.setInt(1, resourceId);
                        stmt.setInt(2, type);
                        stmt.setString(3, namespaceUri);
                        stmt.setString(4, name);
                        stmt.setString(5, value.getStringRepresentation());
                        stmt.addBatch();
                    }
                }
            }
            
            stmt.executeBatch();
            stmt.close();
        }
    }
    


    protected  void delete(Connection conn, ResourceImpl resource)
            throws SQLException {
        String query = this.queryProvider.getDeleteAclEntriesByUriPreparedStatement();
        PreparedStatement stmt = conn.prepareStatement(query);

        stmt.setString(1, getURIWildcard(resource.getURI()));
        stmt.setInt(2, resource.getID());
        stmt.executeUpdate();
        stmt.close();

        if (logger.isDebugEnabled()) {
            logger.debug("Deleted ACL entries for resource " + resource);
        }

        query = this.queryProvider.getDeleteLocksByUriPreparedStatement();
        stmt = conn.prepareStatement(query);
        stmt.setString(1, getURIWildcard(resource.getURI()));
        stmt.setInt(2, resource.getID());
        stmt.executeUpdate();
        stmt.close();

        if (logger.isDebugEnabled()) {
            logger.debug("Deleted locks for resource " + resource);
        }
        
        query = this.queryProvider.getDeletePropertiesByResourceIdPreparedStatement();
        stmt = conn.prepareStatement(query);
        stmt.setInt(1, resource.getID());
        stmt.executeUpdate();
        stmt.close();
        if (logger.isDebugEnabled()) {
            logger.debug("Deleted extra property entries for resource " + resource);
        }

        query = this.queryProvider.getDeleteResourcesByUriPreparedStatement();
        stmt = conn.prepareStatement(query);
        stmt.setString(1, resource.getURI());
        stmt.setString(2, getURIWildcard(resource.getURI()));
        stmt.executeUpdate();
        stmt.close();

        if (logger.isDebugEnabled()) {
            logger.debug("Deleted entry for resource " + resource);
        }

        contentStore.deleteResource(resource.getURI());

        if (logger.isDebugEnabled()) {
            logger.debug("Deleted file(s) for resource " + resource);
        }
    }
    



    /**
     * Loads children for a given resource.
     */
    private String[] loadChildURIs(Connection conn, String uri)
            throws SQLException {

        String query = this.queryProvider.getLoadChildUrisPreparedStatement();
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, getURIWildcard(uri));
        stmt.setInt(2, getURIDepth(uri) + 1);
        
        ResultSet rs = stmt.executeQuery();

        List childURIs = new ArrayList();
        while (rs.next()) {
            String childUri = rs.getString("uri");
            childURIs.add(childUri);
        }
        rs.close();
        stmt.close();

        return (String[]) childURIs.toArray(new String[childURIs.size()]);
    }

    protected ResourceImpl[] loadChildren(Connection conn, ResourceImpl parent)
        throws SQLException {

        Map locks = loadLocksForChildren(conn, parent);

        String query = this.queryProvider.getLoadChildrenPreparedStatement();
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setInt(1, getURIDepth(parent.getURI()) + 1);
        stmt.setString(2, getURIWildcard(parent.getURI()));

        ResultSet rs = stmt.executeQuery();
        List resources = new ArrayList();

        while (rs.next()) {
            String uri = rs.getString("uri");

            ResourceImpl resource = populateResource(uri, rs);
            if (locks.containsKey(uri)) {
                resource.setLock((LockImpl) locks.get(uri));
            }

            resources.add(resource);
        }
        rs.close();
        stmt.close();

        ResourceImpl[] result = (ResourceImpl[]) resources.toArray(new ResourceImpl[resources.size()]);
        loadChildUrisForChildren(conn, parent, result);
        loadACLs(conn, result);
        loadPropertiesForChildren(conn, parent, result);

        if (logger.isDebugEnabled()) {
            logger.debug("Loaded " + result.length + " resources");
        }

        return result;
    }


    private void loadChildUrisForChildren(Connection conn, ResourceImpl parent,
            ResourceImpl[] children)
            throws SQLException {
        
        // Initialize a map from child.URI to the set of grandchildren's URIs:
        Map childMap = new HashMap();
        for (int i = 0; i < children.length; i++) {
            childMap.put(children[i].getURI(), new HashSet());
        }
        String query = this.queryProvider.getLoadChildUrisPreparedStatement();
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, getURIWildcard(parent.getURI()));
        stmt.setInt(2, getURIDepth(parent.getURI()) + 2);
        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
            String uri = rs.getString("uri");
            String parentURI = URIUtil.getParentURI(uri);
            ((Set) childMap.get(parentURI)).add(uri);
        }

        rs.close();
        stmt.close();

        for (int i = 0; i < children.length; i++) {
            if (!children[i].isCollection()) continue;
            Set childURIs = (Set) childMap.get(children[i].getURI());
            children[i].setChildURIs((String[]) childURIs.toArray(new String[childURIs.size()]));
        }

    }


    private int findNearestACL(Connection conn, String uri) throws SQLException {

        String[] path = URLUtil.splitUriIncrementally(uri);

        String query = this.queryProvider.getFindAclInheritedFromResourcesPreparedStatement(path.length);
            
        int n = path.length;
        PreparedStatement stmt = conn.prepareStatement(query);
        for (int i = 0; i < path.length; i++) {
            stmt.setString(n--, path[i]);
        }
        Map uris = new HashMap();
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            uris.put(rs.getString("uri"), new Integer(rs.getInt("resource_id")));
        }
        rs.close();
        stmt.close();
            
        int nearestResourceId = -1;
        for (int i = path.length - 1; i >= 0; i--) {
            if (uris.containsKey(path[i])) {
                nearestResourceId = ((Integer) uris.get(path[i])).intValue();
                break;
            }
        }
        if (nearestResourceId == -1) {
            throw new SQLException("Database inconsistency: no acl to inherit "
                                   + "from for resource " + uri);
        }

        return nearestResourceId;
    }
    


    private void loadACLs(Connection conn, ResourceImpl[] resources)
        throws SQLException {

        if (resources.length == 0) return; 

        Set resourceIds = new HashSet();
        for (int i = 0; i < resources.length; i++) {
            if (resources[i].getAclInheritedFrom() != -1) {
                resourceIds.add(new Integer(resources[i].getAclInheritedFrom()));
            } else {
                resourceIds.add(new Integer(resources[i].getID()));
            }
        }

        Map map = loadAclMap(conn, resourceIds);

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

            try {
                acl = (AclImpl) acl.clone();
            } catch (CloneNotSupportedException e) { }

            acl.setInherited(resources[i].isInheritedACL());
            resources[i].setACL(acl);
        }
    }
    


    private Map loadAclMap(Connection conn, Set resourceIds) throws SQLException {

        Map resultMap = new HashMap();

        if (resourceIds.isEmpty()) {
            return resultMap;
        }

        String query = this.queryProvider.getLoadAclsByResourceIdsPreparedStatement(resourceIds);
        PreparedStatement stmt = conn.prepareStatement(query.toString());

        int n = 1;
        for (Iterator i = resourceIds.iterator(); i.hasNext();) {
            Integer id = (Integer)i.next();
            stmt.setInt(n++, id.intValue());
        }
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            
            Integer resourceId = new Integer(rs.getInt("resource_id"));
            String action = rs.getString("action_name");

            AclImpl acl = (AclImpl)resultMap.get(resourceId);
            
            if (acl == null) {
                acl = new AclImpl(this.principalManager);
                resultMap.put(resourceId, acl);
            }
            
            boolean isGroup = rs.getString("is_user").equals("N");
            String name = rs.getString("user_or_group_name");
            Principal p = null;

            if (isGroup)
                p = principalManager.getGroupPrincipal(name);
            else if (name.startsWith("pseudo:"))
                p = principalManager.getPseudoPrincipal(name);
            else
                p = principalManager.getUserPrincipal(name);
            acl.addEntry(action, p);
        }
        rs.close();
        stmt.close();

        return resultMap;
    }
    




    private Map loadLocksForChildren(Connection conn, ResourceImpl parent)
            throws SQLException {

        String query = this.queryProvider.getLoadLocksForChildrenPreparedStatement();
        
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, getURIWildcard(parent.getURI()));
        stmt.setInt(2, getURIDepth(parent.getURI()) + 1);
        
        ResultSet rs = stmt.executeQuery();
        Map result = new HashMap();

        while (rs.next()) {
            LockImpl lock = new LockImpl(rs.getString("token"), 
                    this.principalManager.getUserPrincipal(rs.getString("lock_owner")),
                rs.getString("lock_owner_info"), rs.getString("depth"),
                new Date(rs.getTimestamp("timeout").getTime()));

            if (lock.getTimeout().getTime() > System.currentTimeMillis()) {
                result.put(rs.getString("uri"), lock);
            }
        }

        rs.close();
        stmt.close();

        return result;
    }

    
    protected String[] discoverACLs(Connection conn, String uri)
            throws SQLException {

        String query = this.queryProvider.getDiscoverAclsPreparedStatement();
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, getURIWildcard(uri));
        ResultSet rs = stmt.executeQuery();
        
        List uris = new ArrayList();

        while (rs.next()) {
            uris.add(rs.getString("uri"));
        }

        rs.close();
        stmt.close();

        return (String[]) uris.toArray(new String[uris.size()]);

    }



    protected void copy(Connection conn, ResourceImpl resource,
                        String destURI, boolean copyACLs,
                        boolean setOwner, String owner) throws SQLException, IOException {

        long timestamp = System.currentTimeMillis();

        int depthDiff = getURIDepth(destURI) - getURIDepth(resource.getURI());
    
        String query = setOwner ?
            this.queryProvider.getCopyResourceSetOwnerPreparedStatement() :
            this.queryProvider.getCopyResourcePreserveOwnerPreparedStatement();
        PreparedStatement stmt = conn.prepareStatement(query);
        int i = 1;
        stmt.setString(i++, destURI);
        stmt.setString(i++, resource.getURI());
        stmt.setInt(i++, depthDiff);
        if (setOwner) {
            stmt.setString(i++, owner);
            
        } 
        stmt.setString(i++, resource.getURI());
        stmt.setString(i++, getURIWildcard(resource.getURI()));
        stmt.executeUpdate();
        stmt.close();


        query = this.queryProvider.getCopyPropertiesPreparedStatement();
        stmt = conn.prepareStatement(query);
        stmt.setString(1, destURI);
        stmt.setString(2, getURIWildcard(destURI));
        stmt.executeUpdate();
        stmt.close();


        if (copyACLs) {

            query = this.queryProvider.getCopyAclsPreparedStatement();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, destURI);
            stmt.setString(2, getURIWildcard(destURI));
            stmt.executeUpdate();
            stmt.close();

            // Update inheritance to nearest node:
            int srcNearestACL = findNearestACL(conn, resource.getURI());
            int destNearestACL = findNearestACL(conn, destURI);

            query = this.queryProvider.getUpdateAclInheritedByPrevIdPreparedStatement();

            stmt = conn.prepareStatement(query);
            stmt.setInt(1, destNearestACL);
            stmt.setString(2, destURI);
            stmt.setString(3, getURIWildcard(destURI));
            stmt.setInt(4, srcNearestACL);
            stmt.executeUpdate();
            stmt.close();

            query = this.queryProvider.getUpdateAclInheritedFromByPrevResourceIdPreparedStatement();
            if (query != null) {

                // Update acl_inherited_from, using prev_{acl_entry, resource}_id:

                query = this.queryProvider.getUpdateAclInheritedFromByPrevResourceIdPreparedStatement();            
                stmt = conn.prepareStatement(query);
                stmt.setString(1, destURI);
                stmt.setString(2, getURIWildcard(destURI));
                stmt.executeUpdate();
                stmt.close();
                
            } else {

                query = this.queryProvider.getMapInheritedFromByPrevResourceIdPreparedStatement();

                stmt = conn.prepareStatement(query);
                stmt.setString(1, destURI);
                stmt.setString(2, getURIWildcard(destURI));
                ResultSet rs = stmt.executeQuery();

                Map inheritedMap = new HashMap();
                while (rs.next()) {
                    inheritedMap.put(new Integer(rs.getInt("resource_id")),
                                     new Integer(rs.getInt("inherited_from")));

                }
                rs.close();
                stmt.close();

                query = this.queryProvider.getUpdateAclInheritedByResourceIdPreparedStatement();
                stmt = conn.prepareStatement(query);

                for (Iterator iter = inheritedMap.entrySet().iterator(); iter.hasNext();) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    stmt.setInt(1, ((Integer) entry.getKey()).intValue());
                    stmt.setInt(2, ((Integer) entry.getValue()).intValue());
                    stmt.addBatch();
                }
                stmt.executeBatch();
                stmt.close();
            }

        } else {
            int nearestAclNode = findNearestACL(conn, destURI);
            query = this.queryProvider.getUpdateAclInheritedByUriPreparedStatement();
            
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, nearestAclNode);
            stmt.setString(2, destURI);
            stmt.setString(3, getURIWildcard(destURI));
            stmt.executeUpdate();
            stmt.close();
        }

        query = this.queryProvider.getClearPrevResourceIdPreparedStatement();
        stmt = conn.prepareStatement(query);
        stmt.setString(1, destURI);
        stmt.setString(2, getURIWildcard(destURI));
        stmt.executeUpdate();
        stmt.close();

        contentStore.copy(resource.getURI(), destURI);
        long duration = System.currentTimeMillis() - timestamp;

        if (logger.isDebugEnabled()) {
            logger.debug("Successfully copied '" + resource.getURI() + "' to '"
                         + destURI + "' in " + duration + " ms");
        }
    }

    
}
