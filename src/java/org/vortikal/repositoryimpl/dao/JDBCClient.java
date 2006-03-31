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
        resource.setInheritedACL(rs.getString("acl_inherited").equals("Y"));
        
        boolean collection = rs.getString("is_collection").equals("Y");
        Property prop = this.propertyManager.createProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.COLLECTION_PROP_NAME,
            new Boolean(collection));
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
     * Returns a list of PropHolder objects from the ResultSet, merging
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


    protected String[] discoverLocks(Connection conn, ResourceImpl resource)
            throws SQLException {
        if (!resource.isCollection()) {
            LockImpl lock = loadLock(conn, resource.getURI());

            return (lock != null) ? new String[] {resource.getURI()} : new String[0];
        }

        String uri = resource.getURI();

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

        return (String[]) result.toArray(new String[] {});
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

        Date contentLastModified = r.getContentLastModified();
        Date propertiesLastModified = r.getPropertiesLastModified();
        Date creationTime = r.getCreationTime();

        String owner = r.getOwner().getQualifiedName();
        String contentModifiedBy = r.getContentModifiedBy().getQualifiedName();
        String propertiesModifiedBy = r.getPropertiesModifiedBy().getQualifiedName();
        boolean collection = r.isCollection();

        String displayName = r.getDisplayName();
        String contentType = r.getContentType();
        String characterEncoding = r.getCharacterEncoding();
        String contentLanguage = r.getContentLanguage();

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
            stmt.setString(11, uri);

            stmt.executeUpdate();
            stmt.close();
        } else {
            insertResourceEntry(conn, uri, creationTime, contentLastModified,
                    propertiesLastModified, displayName, owner, contentModifiedBy,
                    propertiesModifiedBy, contentLanguage, contentType,
                    characterEncoding, collection, parent);
        }

        // Save the ACL:
        storeACL(conn, r);

        // Save the Lock:
        storeLock(conn, r);

        // Save properties:
        storeProperties(conn, r);
    }

    private void insertResourceEntry(Connection conn, String uri, Date creationTime,
            Date contentLastModified, Date propertiesLastModified, String displayname,
            String owner, String contentModifiedBy, String propertiesModifiedBy,
            String contentlanguage, String contenttype, String characterEncoding,
            boolean isCollection, String parent)
            throws SQLException, IOException {
        int depth = getURIDepth(uri);

        String statement = this.queryProvider.getInsertResourcePreparedStatement();

        PreparedStatement stmt = conn.prepareStatement(statement);

        stmt.setString(1, uri);
        stmt.setInt(2, depth);
        stmt.setTimestamp(3, new Timestamp(creationTime.getTime()));
        stmt.setTimestamp(4, new Timestamp(contentLastModified.getTime()));
        stmt.setTimestamp(5, new Timestamp(propertiesLastModified.getTime()));
        stmt.setString(6, contentModifiedBy);
        stmt.setString(7, propertiesModifiedBy);
        stmt.setString(8, owner);
        stmt.setString(9, displayname);
        stmt.setString(10, contentlanguage);
        stmt.setString(11, contenttype);
        stmt.setString(12, characterEncoding);
        stmt.setString(13, isCollection ? "Y" : "N");
        stmt.setString(14, "Y");

        stmt.executeUpdate();
        stmt.close();

        contentStore.createResource(uri, isCollection);
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
        // XXX: do not belong here!?
        // first, check if existing ACL is inherited:
        ResourceImpl existingResource = load(conn, r.getURI());

        if (existingResource.isInheritedACL() && r.isInheritedACL()) {
            Acl newACL = r.getAcl();

            if (existingResource.getAcl().equals(newACL)) {
                /* No need to insert ACL, is inherited and not modified */
                return;
            }
        }

        // ACL is either not inherited OR inherited and THEN modified,
        // so we have to store the ACL entries
        insertACL(conn, r);
    }

    private void insertACL(Connection conn, ResourceImpl r)
            throws SQLException {
        Acl acl = r.getAcl();
        Set actions = acl.getActions();

        /*
         * First, delete any previously defined ACEs for this resource:
         */
        String query = this.queryProvider.getDeleteACLEntryByResourceIdPreparedStatement();
        PreparedStatement deleteStatement = conn.prepareStatement(query);
        deleteStatement.setInt(1, r.getID());
        deleteStatement.executeUpdate();
        deleteStatement.close();

        if (r.isInheritedACL()) {
            /*
             * The ACL is inherited. Update resource entry to reflect this
             * situation (and return)
             */
            query = this.queryProvider.getSetAclInheritedPreparedStatement(true);
            PreparedStatement updateStmt = conn.prepareStatement(query);
            updateStmt.setInt(1, r.getID());
            updateStmt.executeUpdate();
            updateStmt.close();

            return;
        }

        /* Insert an entry for each privilege */
        for (Iterator i = actions.iterator(); i.hasNext();) {
            String action = (String) i.next();

            for (Iterator j = acl.getPrincipalSet(action).iterator(); j.hasNext();) {
                Principal p = (Principal) j.next();

                insertACLEntry(conn, action, r, p);
            }
        }

        /*
         * At this point, we know that the ACL is not inherited. Update the
         * inheritance flag in the resource entry:
         */
        query = this.queryProvider.getSetAclInheritedPreparedStatement(false);
        PreparedStatement updateStmt = conn.prepareStatement(query);
        updateStmt.setInt(1, r.getID());

        updateStmt.executeUpdate();
        updateStmt.close();
    }

    private void insertACLEntry(Connection conn, String action, ResourceImpl resource,
            Principal p)
            throws SQLException {
        String query = this.queryProvider.getLoadActionTypeIdFromNamePreparedStatement();
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, action);
        ResultSet rs = stmt.executeQuery();

        if (!rs.next()) {
            rs.close();
            stmt.close();
            throw new SQLException("Database.insertACLEntry(): Unable to "
                    + "find id for action '" + action + "'");
        }

        int actionID = rs.getInt("action_type_id");

        rs.close();
        stmt.close();

        query = this.queryProvider.getInsertAclEntryPreparedStatement();
        PreparedStatement insertStmt = conn.prepareStatement(query);

        insertStmt.setInt(1, actionID);
        insertStmt.setInt(2, resource.getID());
        insertStmt.setString(3, p.getQualifiedName());
        insertStmt.setString(4, p.getType() == Principal.TYPE_GROUP ? "N" : "Y");

        // XXX: Does this have any point?
        insertStmt.setString(5, resource.getOwner().getQualifiedName());
        insertStmt.setTimestamp(6, new Timestamp(System.currentTimeMillis()));

        insertStmt.executeUpdate();
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
                            stmt.setString(5, values[i]
                                    .getStringRepresentation());
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

        query = this.queryProvider.getDeleteLocksByUriPreparedStatement();
        stmt = conn.prepareStatement(query);
        stmt.setString(1, getURIWildcard(resource.getURI()));
        stmt.setInt(2, resource.getID());
        stmt.executeUpdate();
        stmt.close();

        query = this.queryProvider.getDeletePropertiesByUriPreparedStatement();
        stmt = conn.prepareStatement(query);
        stmt.setString(1, getURIWildcard(resource.getURI()));
        stmt.setInt(2, resource.getID());
        stmt.executeUpdate();
        stmt.close();

        query = this.queryProvider.getDeleteResourcesByUriPreparedStatement();
        stmt = conn.prepareStatement(query);
        stmt.setString(1, getURIWildcard(resource.getURI()));
        stmt.setInt(2, resource.getID());
        stmt.executeUpdate();
        stmt.close();

        contentStore.deleteResource(resource.getURI());
    }
    
    public InputStream getInputStream(ResourceImpl resource)
            throws IOException {
        return contentStore.getInputStream(resource.getURI());
    }

    public void storeContent(ResourceImpl resource, InputStream inputStream)
            throws IOException {
        contentStore.storeContent(resource.getURI(), inputStream);
    }

    protected Map executeACLQuery(Connection conn, List uris)
            throws SQLException {
        Map acls = new HashMap();
        
        String query = this.queryProvider.getLoadAncestorAclsPreparedStatement(uris);
        int n = uris.size();
        PreparedStatement stmt = conn.prepareStatement(query.toString());
        for (Iterator i = uris.iterator(); i.hasNext();) {
            String uri = (String) i.next();
            stmt.setString(n--, uri);
        }
        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {

            String uri = rs.getString("uri");
            String action = rs.getString("action_name");

            AclImpl acl = (AclImpl)acls.get(uri);
            
            if (acl == null) {
                acl = new AclImpl(this.principalManager);
                acls.put(uri, acl);
            }
            
            boolean isGroup = rs.getString("is_user").equals("N");
            String name = rs.getString("user_or_group_name");
            Principal p = null;

            if (isGroup)
                p = principalManager.getGroupPrincipal(name);
            else if (name.startsWith("dav:"))
                p = principalManager.getPseudoPrincipal(name);
            else
                p = principalManager.getUserPrincipal(name);

            acl.addEntry(action, p);
        }

        rs.close();
        stmt.close();
    
        return acls;
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
        stmt.setString(1, getURIWildcard(parent.getURI()));
        stmt.setInt(2, getURIDepth(parent.getURI()) + 1);

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
            
//             Long parentID = new Long(rs.getLong("parent_id"));
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



    private void loadACLs(Connection conn, ResourceImpl[] resources)
        throws SQLException {

        if (resources == null || resources.length == 0) {
            return;
        }

        List uris = new ArrayList();
        
        for (int i = 0; i < resources.length; i++) {

            String[] path = URLUtil.splitUriIncrementally(
                resources[i].getURI());

            for (int j = path.length -1; j >= 0; j--) {

                if (!uris.contains(path[j])) {
                    uris.add(path[j]);
                }
            }
        }

        if (uris.size() == 0) {
            throw new SQLException("No ancestor path");
        }
    

        /* Populate the parent ACL map (these are all the ACLs that
         * will be needed) */
        Map acls = executeACLQuery(conn, uris);

        for (int i = 0; i < resources.length; i++) {

            ResourceImpl resource = resources[i];
            AclImpl acl = null;

            if (!resource.isInheritedACL()) {
            
                if (!acls.containsKey(resource.getURI())) {
                    throw new SQLException(
                        "Database inconsistency: resource " +
                        resource.getURI() + " claims  ACL is not inherited, " +
                        "but no ACL exists");
                }
                
                acl = (AclImpl) acls.get(resource.getURI());

            } else {

                String[] path = URLUtil.splitUriIncrementally(
                    resource.getURI());

                for (int j = path.length - 2; j >= 0; j--) {

                    AclImpl found = (AclImpl) acls.get(path[j]);

                    if (found != null) {
                        try {
                            /* We have to clone the ACL here, because ACLs
                             * and resources are "doubly linked". */
                            acl = (AclImpl) found.clone();
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
            acl.setInherited(resource.isInheritedACL());
            acl.setOwner(resource.getOwner());
            resource.setACL(acl);
        }
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

    
    protected String[] discoverACLs(Connection conn, ResourceImpl resource)
            throws SQLException {

        String query = this.queryProvider.getDiscoverAclsPreparedStatement();
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, getURIWildcard(resource.getURI()));
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
