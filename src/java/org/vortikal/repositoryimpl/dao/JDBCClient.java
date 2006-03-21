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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.beans.factory.DisposableBean;
import org.vortikal.repository.Acl;
import org.vortikal.repository.IllegalOperationException;
import org.vortikal.repository.Lock;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repositoryimpl.AclImpl;
import org.vortikal.repositoryimpl.ACLPrincipal;
import org.vortikal.repositoryimpl.LockImpl;
import org.vortikal.repositoryimpl.PropertyManagerImpl;
import org.vortikal.repositoryimpl.ResourceImpl;
import org.vortikal.util.repository.URIUtil;

/**
 * This class is going to be a "generic" JDBC database accessor. Currently, only
 * PostgreSQL is supported.
 *
 */
public class JDBCClient extends AbstractDataAccessor implements DisposableBean {

    private String databaseDriver;
    private int maxDatabaseConnections = 6;
    private String databaseURL;
    private String databaseUser;
    private String databasePassword;

    private BasicDataSource pool;

    public void setDatabaseDriver(String databaseDriver) {
        this.databaseDriver = databaseDriver;
    }

    public void setDatabasePassword(String databasePassword) {
        this.databasePassword = databasePassword;
    }

    public void setDatabaseURL(String databaseURL) {
        this.databaseURL = databaseURL;
    }

    public void setDatabaseUser(String databaseUser) {
        this.databaseUser = databaseUser;
    }

    public void setMaxDatabaseConnections(int maxDatabaseConnections) {
        this.maxDatabaseConnections = maxDatabaseConnections;
    }

    public void afterPropertiesSet()
            throws Exception {
        super.afterPropertiesSet();

        logger.info("Setting up database");

        if (contentStore == null) {
            throw new IOException("Missing property 'fileStore'");
        }
        
        if (databaseURL == null) {
            throw new IOException("Missing property \"databaseURL\"");
        }

        if (databaseUser == null) {
            throw new IOException("Missing property \"databaseUser\"");
        }

        if (databasePassword == null) {
            throw new IOException("Missing property \"databasePassword\"");
        }

        if (maxDatabaseConnections == 0) {
            throw new IOException("Missing property " + "\"maxDatabaseConnections\"");
        }

        if (databaseDriver == null) {
            throw new IOException("Missing property " + "\"databaseDriver\"");
        }

        pool = new BasicDataSource();

        logger.info("Using driver " + databaseDriver);
        pool.setDriverClassName(databaseDriver);
        pool.setMaxActive(maxDatabaseConnections);
        pool.setUrl(databaseURL);
        pool.setUsername(databaseUser);
        pool.setPassword(databasePassword);
        pool.setDefaultAutoCommit(false);
    }

    /**
     * Gets a connection from the pool with auto commit set to false.
     * 
     * @return a <code>Connection</code>
     * @exception SQLException
     *                if an error occurs
     */
    public Connection getConnection()
            throws SQLException {
        Connection conn = pool.getConnection();
        conn.setAutoCommit(false);     
        return conn;
    }

    private boolean tableExists(Connection conn, String tableName)
            throws IOException {
        try {
            DatabaseMetaData md = conn.getMetaData();

            ResultSet rs = md.getTables(null, null, tableName, null);
            boolean exists = rs.next();

            rs.close();

            return exists;
        } catch (SQLException e) {
            logger.warn("Error occurred while checking for table existance: ", e);
            throw new IOException(e.getMessage());
        }
    }

    public boolean validate()
            throws IOException {
        Connection conn = null;
        boolean exists = false;

        try {
            conn = getConnection();
            exists = ((tableExists(conn, "vortex_resource") || tableExists(conn,
                    "VORTEX_RESOURCE"))
                    && (tableExists(conn, "lock_type") || tableExists(conn, "LOCK_TYPE"))
                    && (tableExists(conn, "vortex_lock") || tableExists(conn,
                            "VORTEX_LOCK"))
                    && (tableExists(conn, "action_type") || tableExists(conn,
                            "ACTION_TYPE"))
                    && (tableExists(conn, "acl_entry") || tableExists(conn, "ACL_ENTRY")) && (tableExists(
                    conn, "extra_prop_entry") || tableExists(conn, "EXTRA_PROP_ENTRY")));

            conn.commit();

            if (logger.isDebugEnabled()) {
                logger.debug(exists ? "All required tables exist"
                        : "Table(s) are missing");
            }
        } catch (SQLException e) {
            logger.warn("Error occurred while checking database validity: ", e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                    conn = null;
                }
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }

        return exists;
    }

    public void destroy() {
        try {
            pool.close();
        } catch (SQLException e) {
            logger.error("Error closing pooled connections:", e);
        }
    }


    public ResourceImpl load(String uri) throws IOException {
        Connection conn = null;
        ResourceImpl retVal = null;

        try {
            conn = getConnection();
            retVal = load(conn, uri);
            conn.commit();
        } catch (SQLException e) {
            logger.warn("Error occurred while loading resource(s)", e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                    conn = null;
                }
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }

        return retVal;
    }

    private ResourceImpl load(Connection conn, String uri) throws SQLException {

        String query = "select r.* from VORTEX_RESOURCE r where r.uri = ?";
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

        ResourceImpl resource = new ResourceImpl(uri, principalManager, propertyManager);
        resource.setID(rs.getInt("resource_id"));
        resource.setInheritedACL(rs.getString("acl_inherited").equals("Y"));
        
        Property prop = this.propertyManager.createProperty(Namespace.DEFAULT_NAMESPACE, PropertyType.COLLECTION_PROP_NAME, new Boolean(rs.getString("is_collection").equals("Y")));
        resource.addProperty(prop);
        
        prop = this.propertyManager.createProperty(Namespace.DEFAULT_NAMESPACE, PropertyType.CREATIONTIME_PROP_NAME, new Date(rs.getTimestamp("creation_time").getTime()));
        resource.addProperty(prop);

        prop = this.propertyManager.createProperty(Namespace.DEFAULT_NAMESPACE, PropertyType.OWNER_PROP_NAME, rs.getString("resource_owner"));
        resource.addProperty(prop);

        prop = this.propertyManager.createProperty(Namespace.DEFAULT_NAMESPACE, PropertyType.DISPLAYNAME_PROP_NAME, rs.getString("display_name"));
        resource.addProperty(prop);

        prop = this.propertyManager.createProperty(Namespace.DEFAULT_NAMESPACE, PropertyType.CONTENTTYPE_PROP_NAME, rs.getString("content_type"));
        resource.addProperty(prop);

        prop = this.propertyManager.createProperty(Namespace.DEFAULT_NAMESPACE, PropertyType.CHARACTERENCODING_PROP_NAME, rs.getString("character_encoding"));
        resource.addProperty(prop);

        prop = this.propertyManager.createProperty(Namespace.DEFAULT_NAMESPACE, PropertyType.CONTENTLOCALE_PROP_NAME, rs.getString("content_language"));
        resource.addProperty(prop);

        prop = this.propertyManager.createProperty(Namespace.DEFAULT_NAMESPACE, PropertyType.CONTENTLASTMODIFIED_PROP_NAME, new Date(rs.getTimestamp("content_last_modified").getTime()));
        resource.addProperty(prop);

        prop = this.propertyManager.createProperty(Namespace.DEFAULT_NAMESPACE, PropertyType.CONTENTMODIFIEDBY_PROP_NAME, rs.getString("content_modified_by"));
        resource.addProperty(prop);

        prop = this.propertyManager.createProperty(Namespace.DEFAULT_NAMESPACE, PropertyType.PROPERTIESLASTMODIFIED_PROP_NAME, new Date(rs.getTimestamp("properties_last_modified").getTime()));
        resource.addProperty(prop);

        prop = this.propertyManager.createProperty(Namespace.DEFAULT_NAMESPACE, PropertyType.PROPERTIESMODIFIEDBY_PROP_NAME, rs.getString("properties_modified_by"));
        resource.addProperty(prop);

        try {
            long contentLength = contentStore.getContentLength(resource.getURI());
            prop = this.propertyManager.createProperty(Namespace.DEFAULT_NAMESPACE, PropertyType.CONTENTLENGTH_PROP_NAME, new Long(contentLength));
            resource.addProperty(prop);
        } catch (IllegalOperationException e) {
            // Probably a collection
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


    private void loadProperties(Connection conn, ResourceImpl resource)
            throws SQLException {

        String uri = resource.getURI();
        
        String query = "select * from EXTRA_PROP_ENTRY where resource_id in " + 
        "(select resource_id from vortex_resource where uri = '" + uri + "')";

        Statement propStmt = conn.createStatement();
        ResultSet rs = propStmt.executeQuery(query);

        while (rs.next()) {

            Namespace ns = Namespace.getNamespace(rs.getString("name_space"));
            String name = rs.getString("name");
            String value = rs.getString("value");

            Property prop = this.propertyManager.createProperty(ns, name, value);
            resource.addProperty(prop);
        }

        rs.close();
        propStmt.close();

    }

    public void deleteExpiredLocks() throws IOException {
        Connection conn = null;

        try {
            conn = getConnection();
            deleteExpiredLocks(conn);
            conn.commit();
        } catch (SQLException e) {
            logger.warn("Error occurred while deleting expired locks", e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                    conn = null;
                }
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }
    }

    private void deleteExpiredLocks(Connection conn)
            throws SQLException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String format = formatter.format(new Date());

        String query = "delete from VORTEX_LOCK "
                + "where timeout < '" + format + "'";

        Statement stmt = conn.createStatement();
        int n = stmt.executeUpdate(query);
        if (logger.isInfoEnabled()) {
            logger.info("Deleted " + n + " expired locks");
        }
        stmt.close();
    }


    public void addChangeLogEntry(String loggerID, String loggerType, String uri,
                                  String operation, int resourceId, boolean collection,
                                  boolean recurse) throws IOException {
        Connection conn = null;

        try {
            conn = getConnection();
            addChangeLogEntry(conn, loggerID, loggerType, uri, operation, resourceId,
                              collection, recurse);
            conn.commit();
        } catch (SQLException e) {
            logger.warn("Error occurred while adding changelog entry for " + uri, e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                    conn = null;
                }
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }
    }

    private void addChangeLogEntry(Connection conn, String loggerID, String loggerType,
                                   String uri, String operation, int resourceId,
                                   boolean collection, boolean recurse) throws SQLException {
        if (collection && recurse) {

            int id = Integer.parseInt(loggerID);
            int type = Integer.parseInt(loggerType);

            String statement = "INSERT INTO changelog_entry "
                + "(changelog_entry_id, logger_id, logger_type, "
                + "operation, timestamp, uri, resource_id, is_collection) "
                + "select nextval('changelog_entry_seq_pk'), " + id + ", " + type + ", "
                + "'" + operation + "', now(), uri, ";
            if (resourceId == -1) {
                statement += "NULL, ";
            } else {
                statement += "resource_id, ";
            }
            statement += "is_collection from vortex_resource "
                + "where uri = '" + uri + "' or uri like '" + getURIWildcard(uri) + "'";
            
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(statement);
            stmt.close();

        } else {


            try {
                int id = Integer.parseInt(loggerID);
                int type = Integer.parseInt(loggerType);

                String statement = "INSERT INTO changelog_entry "
                    + "(changelog_entry_id, logger_id, logger_type, "
                    + "operation, timestamp, uri, resource_id, is_collection) "
                    + "VALUES (nextval('changelog_entry_seq_pk'), ?, ?, ?, ?, ?, ?, ?)";

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

    public String[] discoverLocks(ResourceImpl directory)
            throws IOException {
        Connection conn = null;

        try {
            conn = getConnection();

            if (!directory.isCollection()) {
                LockImpl lock = loadLock(conn, directory.getURI());

                return (lock != null) ? new String[] {directory.getURI()} : new String[0];
            }

            String[] lockedURIs = discoverLocks(conn, directory);

            conn.commit();

            return lockedURIs;
        } catch (SQLException e) {
            logger.warn("Error occurred while discovering locks", e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                    conn = null;
                }
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }
    }

    private String[] discoverLocks(Connection conn, ResourceImpl directory)
            throws SQLException {
        String uri = directory.getURI();

        String query = "select r.uri, l.* from VORTEX_LOCK l inner join VORTEX_RESOURCE r "
                + "on l.resource_id = r.resource_id "
                + "where r.resource_id in ("
                + "select resource_id from VORTEX_RESOURCE where uri like ?)";
        

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
        String query = "select * from VORTEX_LOCK where resource_id in ("
                + "select resource_id from VORTEX_RESOURCE where uri = ?)";

        PreparedStatement stmt = conn.prepareStatement(query);

        stmt.setString(1, uri);

        ResultSet rs = stmt.executeQuery();

        LockImpl lock = null;

        if (rs.next()) {
            lock = new LockImpl(rs.getString("token"), 
                    principalManager.getPrincipal(rs.getString("lock_owner")), 
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

        String query = "select r.uri as uri, l.* from VORTEX_RESOURCE r "
                + "inner join VORTEX_LOCK l on r.resource_id = l.resource_id "
                + "where r.uri in (";

        for (int i = 0; i < uris.length; i++) {
            query += ((i < (uris.length - 1)) ? "?, " : "?)");
        }

        PreparedStatement stmt = conn.prepareStatement(query);

        for (int i = 0; i < uris.length; i++) {
            stmt.setString(i + 1, uris[i]);
        }

        ResultSet rs = stmt.executeQuery();
        Map result = new HashMap();

        while (rs.next()) {
            LockImpl lock = new LockImpl(rs.getString("token"), 
                    principalManager.getPrincipal(rs.getString("lock_owner")),
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

    public String[] listSubTree(ResourceImpl parent)
            throws IOException {
        Connection conn = null;
        String[] retVal = null;

        try {
            conn = getConnection();
            retVal = listSubTree(conn, parent);
            conn.commit();
        } catch (SQLException e) {
            logger.warn("Error occurred while listing resource tree", e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                    conn = null;
                }
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }

        return retVal;
    }

    private String[] listSubTree(Connection conn, ResourceImpl parent)
            throws SQLException {
        if (parent.getURI() == null) {
            return new String[] {};
        }

        String query = "select uri from VORTEX_RESOURCE "
                + "where uri like ? order by uri asc";

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

    public void store(ResourceImpl r)
            throws IOException {
        Connection conn = null;

        try {
            conn = getConnection();
            store(conn, r);
            conn.commit();
        } catch (SQLException e) {
            logger.warn("Error occurred while storing resource " + r.getURI(), e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                    conn = null;
                }
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }
    }

    private void store(Connection conn, ResourceImpl r)
            throws SQLException, IOException {
        String uri = r.getURI();
        
        String parent = URIUtil.getParentURI(uri);

        Date contentLastModified = r.getContentLastModified();
        Date propertiesLastModified = r.getPropertiesLastModified();
        Date creationTime = r.getCreationTime();

        String displayName = r.getDisplayName();

        String contentType = r.getContentType();
        String characterEncoding = r.getCharacterEncoding();
        String owner = r.getOwner().getQualifiedName();
        String contentModifiedBy = r.getContentModifiedBy().getQualifiedName();
        String propertiesModifiedBy = r.getPropertiesModifiedBy().getQualifiedName();
        Locale locale = r.getContentLocale();
        String contentLanguage = (locale == null) ? null : locale.toString();
        boolean collection = r.isCollection();
        
        String query = "select * from VORTEX_RESOURCE where uri = ?";
        PreparedStatement existStmt = conn.prepareStatement(query);

        existStmt.setString(1, uri);

        ResultSet rs = existStmt.executeQuery();
        boolean existed = rs.next();

        rs.close();
        existStmt.close();

        if (existed) {
            // Save the VORTEX_RESOURCE table entry:
            PreparedStatement stmt = conn
                    .prepareStatement("update VORTEX_RESOURCE set "
                            + "content_last_modified = ?, "
                            + "properties_last_modified = ?, "
                            + "content_modified_by = ?, "
                            + "properties_modified_by = ?, " + "resource_owner = ?, "
                            + "display_name = ?, " + "content_language = ?, "
                            + "content_type = ?, " + "character_encoding = ?, "
                            + "creation_time = ? " + "where uri = ?");

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

        String statement = "insert into VORTEX_RESOURCE "
                + "(resource_id, uri, depth, creation_time, content_last_modified, properties_last_modified, "
                + "content_modified_by, properties_modified_by, "
                + "resource_owner, display_name, "
                + "content_language, content_type, character_encoding, is_collection, acl_inherited) "
                + "values (nextval('vortex_resource_seq_pk'), "
                + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

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

        if (lock == null) {
            Statement stmt = conn.createStatement();

            stmt.execute("delete from VORTEX_LOCK where resource_id = " + r.getID());
            stmt.close();
        }

        if (lock != null) {
            String lockToken = lock.getLockToken();
            Date timeout = lock.getTimeout();
            String type = lock.getLockType();
            String user = lock.getPrincipal().getQualifiedName();
            String ownerInfo = lock.getOwnerInfo();
            String depth = lock.getDepth();

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("select * from VORTEX_LOCK where token = '"
                    + lockToken + "'");
            boolean exists = rs.next();

            rs.close();
            stmt.close();

            stmt = conn.createStatement();
            rs = stmt.executeQuery("select lock_type_id from LOCK_TYPE where "
                    + "name = '" + type + "'");

            if (!rs.next()) {
                rs.close();
                stmt.close();
                throw new SQLException("Unknown lock type: " + type);
            }

            int lockType = rs.getInt("lock_type_id");

            rs.close();
            stmt.close();

            if (exists) {
                PreparedStatement updateStmt = conn
                        .prepareStatement("update VORTEX_LOCK set "
                                + "lock_type_id = ?, lock_owner = ?, lock_owner_info = ?, "
                                + "depth = ?, timeout = ? " + "where token = ?");

                updateStmt.setInt(1, lockType);
                updateStmt.setString(2, user);
                updateStmt.setString(3, ownerInfo);
                updateStmt.setString(4, depth);
                updateStmt.setTimestamp(5, new Timestamp(timeout.getTime()));
                updateStmt.setString(6, lockToken);

                updateStmt.executeUpdate();
                updateStmt.close();
            } else {
                PreparedStatement insertStmt = conn
                        .prepareStatement("insert into VORTEX_LOCK "
                                + "(lock_id, token, resource_id, lock_type_id, lock_owner, "
                                + "lock_owner_info, depth, timeout) "
                                + "values (nextval('vortex_lock_seq_pk'), "
                                + "?, ?, ?, ?, ?, ?, ?)");

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
        Statement deleteStatement = conn.createStatement();

        deleteStatement.executeUpdate("delete from ACL_ENTRY where " + "resource_id = "
                + r.getID());

        deleteStatement.close();

        if (r.isInheritedACL()) {
            /*
             * The ACL is inherited. Update resource entry to reflect this
             * situation (and return)
             */
            Statement updateStmt = conn.createStatement();

            updateStmt.executeUpdate("update VORTEX_RESOURCE set acl_inherited = 'Y' "
                    + "where resource_id = " + r.getID());
            updateStmt.close();

            return;
        }

        /* Insert an entry for each privilege */
        for (Iterator i = actions.iterator(); i.hasNext();) {
            String action = (String) i.next();

            for (Iterator j = acl.getPrincipalList(action).iterator(); j.hasNext();) {
                ACLPrincipal p = (ACLPrincipal) j.next();

                insertACLEntry(conn, action, r, p);
            }
        }

        /*
         * At this point, we know that the ACL is not inherited. Update the
         * inheritance flag in the resource entry:
         */
        Statement updateStmt = conn.createStatement();

        updateStmt.executeUpdate("update VORTEX_RESOURCE set acl_inherited = 'N' "
                + "where resource_id = " + r.getID());
        updateStmt.close();
    }

    private void insertACLEntry(Connection conn, String action, ResourceImpl resource,
            ACLPrincipal p)
            throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("select action_type_id from "
                + "ACTION_TYPE where name = '" + action + "'");

        if (!rs.next()) {
            rs.close();
            stmt.close();
            throw new SQLException("Database.insertACLEntry(): Unable to "
                    + "find id for action '" + action + "'");
        }

        int actionID = rs.getInt("action_type_id");

        rs.close();
        stmt.close();

        PreparedStatement insertStmt = conn
                .prepareStatement("insert into ACL_ENTRY (acl_entry_id, action_type_id, "
                        + "resource_id, user_or_group_name, "
                        + "is_user, granted_by_user_name, granted_date) "
                        + "values (nextval('acl_entry_seq_pk'), ?, ?, ?, ?, ?, ?)");

        insertStmt.setInt(1, actionID);
        insertStmt.setInt(2, resource.getID());
        insertStmt.setString(3, p.getUrl());
        insertStmt.setString(4, p.isGroup() ? "N" : "Y");

        // XXX: Shouldn't this be principal, not owner?
        insertStmt.setString(5, resource.getOwner().getQualifiedName());
        insertStmt.setTimestamp(6, new Timestamp(System.currentTimeMillis()));

        insertStmt.executeUpdate();
        insertStmt.close();
    }

    private void storeProperties(Connection conn, ResourceImpl r)
            throws SQLException {

        Statement deleteStatement = conn.createStatement();

        deleteStatement.executeUpdate("delete from EXTRA_PROP_ENTRY where "
                + "resource_id = '" + r.getID() + "'");

        deleteStatement.close();

        List properties = r.getProperties();

        if (properties != null) {
            for (Iterator iter = properties.iterator(); iter.hasNext();) {
                Property property = (Property) iter.next();
                if (!PropertyType.SPECIAL_PROPERTIES_SET.contains(property.getName())) {
                    insertPropertyEntry(conn, r, property);
                }
            }
        }
    }
    
    private void insertPropertyEntry(Connection conn, ResourceImpl r, Property property)
            throws SQLException {
        PreparedStatement stmt = conn
                .prepareStatement("insert into EXTRA_PROP_ENTRY (extra_prop_entry_id, resource_id, "
                        + "name_space, name, value) "
                        + "values (nextval('extra_prop_entry_seq_pk'), ?, ?, ?, ?)");

        stmt.setInt(1, r.getID());
        stmt.setString(2, property.getNamespace().getUrl());
        stmt.setString(3, property.getName());
        stmt.setString(4, property.getStringValue());

        stmt.executeUpdate();
        stmt.close();
    }

    public void delete(ResourceImpl resource)
            throws IOException {
        Connection conn = null;

        try {
            conn = getConnection();
            delete(conn, resource);
            conn.commit();
        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                    conn = null;
                }
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }
    }

    public void delete(Connection conn, ResourceImpl resource)
            throws SQLException {
        String query = "delete from ACL_ENTRY where resource_id in ("
                + "select resource_id from VORTEX_RESOURCE "
                + "where uri like ? or resource_id = ?)";
        PreparedStatement stmt = conn.prepareStatement(query);

        stmt.setString(1, getURIWildcard(resource.getURI()));
        stmt.setInt(2, resource.getID());
        stmt.executeUpdate();
        stmt.close();

        query = "delete from VORTEX_LOCK where resource_id in ("
                + "select resource_id from VORTEX_RESOURCE "
                + "where uri like ? or resource_id = ?)";
        stmt = conn.prepareStatement(query);
        stmt.setString(1, getURIWildcard(resource.getURI()));
        stmt.setInt(2, resource.getID());
        stmt.executeUpdate();
        stmt.close();

        query = "delete from EXTRA_PROP_ENTRY where resource_id in ("
                + "select resource_id from VORTEX_RESOURCE "
                + "where uri like ? or resource_id = ?)";
        stmt = conn.prepareStatement(query);
        stmt.setString(1, getURIWildcard(resource.getURI()));
        stmt.setInt(2, resource.getID());
        stmt.executeUpdate();
        stmt.close();

        query = "delete from VORTEX_RESOURCE where resource_id in ("
                + "select resource_id from VORTEX_RESOURCE "
                + "where uri like ? or resource_id = ?)";
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
        
        StringBuffer query = 
            new StringBuffer("select r.uri, a.*, t.namespace as action_namespace, "
                           + "t.name as action_name from ACL_ENTRY a "
                           + "inner join ACTION_TYPE t on a.action_type_id = t.action_type_id "
                           + "inner join VORTEX_RESOURCE r on r.resource_id = a.resource_id "
                           + "where r.uri in (");

        int n = uris.size();
        for (int i = 0; i < n; i++) {
            query.append((i < n - 1) ? "?, " : "?)");
        }
        
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
            

            acl.addEntry(action,rs.getString("user_or_group_name"),
                    rs.getString("is_user").equals("N"));
        }

        rs.close();
        stmt.close();
    
        return acls;
    }



    public ResourceImpl[] loadChildren(ResourceImpl parent)
            throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("load children:" + parent.getURI());
        }
        Connection conn = null;
        ResourceImpl[] retVal = null;

        try {
            conn = getConnection();
            retVal = loadChildren(conn, parent);
            conn.commit();
        } catch (SQLException e) {
            logger.warn("Error occurred while loading children: " + parent.getURI(), e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                    conn = null;
                }
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }

        return retVal;
    }


    /**
     * Loads children for a given resource.
     */
    private String[] loadChildURIs(Connection conn, String uri)
            throws SQLException {

        StringBuffer query = new StringBuffer();
        query.append("select uri from vortex_resource where ");
        query.append("uri like '").append(getURIWildcard(uri)).append("'");
        query.append("and depth = ").append(getURIDepth(uri) + 1);

        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query.toString());

        List childURIs = new ArrayList();
        while (rs.next()) {
            String childUri = rs.getString("uri");
            childURIs.add(childUri);
        }
        rs.close();
        stmt.close();

        return (String[]) childURIs.toArray(new String[childURIs.size()]);
    }

    private ResourceImpl[] loadChildren(Connection conn, ResourceImpl parent)
        throws SQLException {

        Map locks = loadLocksForChildren(conn, parent);

        StringBuffer query = new StringBuffer();
        query.append("select * from vortex_resource where ");
        query.append("uri like ? and depth = ?");

        PreparedStatement stmt = conn.prepareStatement(query.toString());
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


        String query = "select uri from vortex_resource where uri like '"
            + getURIWildcard(parent.getURI()) + "' and depth = "
            + (getURIDepth(parent.getURI()) + 2);


        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query);

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


    private void loadPropertiesForChildren(Connection conn, ResourceImpl parent,
            ResourceImpl[] resources)
            throws SQLException {
        if ((resources == null) || (resources.length == 0)) {
            return;
        }

        String query = "select * from EXTRA_PROP_ENTRY where resource_id in ("
            + "select resource_id from vortex_resource "
            + "where uri like '" + getURIWildcard(parent.getURI()) + "' and depth = "
            + (getURIDepth(parent.getURI()) + 1) + ")";

        Statement propStmt = conn.createStatement();
        ResultSet rs = propStmt.executeQuery(query);
        Map resourceMap = new HashMap();

        for (int i = 0; i < resources.length; i++) {
            resourceMap.put(new Integer(resources[i].getID()), resources[i]);
        }
        
        while (rs.next()) {
            // FIXME: type conversion
            Integer resourceID = new Integer((int) rs.getLong("resource_id"));

            Property prop = this.propertyManager.createProperty(
                    Namespace.getNamespace(rs.getString("name_space")), 
                    rs.getString("name"), rs.getString("value"));
            ResourceImpl r = (ResourceImpl) resourceMap.get(resourceID);
            r.addProperty(prop);
        }

        rs.close();
        propStmt.close();

    }

    private Map loadLocksForChildren(Connection conn, ResourceImpl parent)
            throws SQLException {

        String query = "select r.uri as uri, l.* from VORTEX_RESOURCE r "
            + "inner join VORTEX_LOCK l on r.resource_id = l.resource_id "
            + "where r.resource_id in (select resource_id from vortex_resource "
            + "where uri like '" + getURIWildcard(parent.getURI()) + "' and depth = "
            + (getURIDepth(parent.getURI()) + 1) + ")";

        PreparedStatement stmt = conn.prepareStatement(query);

        ResultSet rs = stmt.executeQuery();
        Map result = new HashMap();

        while (rs.next()) {
            LockImpl lock = new LockImpl(rs.getString("token"), 
                    this.principalManager.getPrincipal(rs.getString("lock_owner")),
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

    //------------- NEW STUFF

    public String[] discoverACLs(ResourceImpl resource) throws IOException {
        Connection conn = null;
        String[] retVal = null;

        try {
            conn = getConnection();
            retVal = discoverACLs(conn, resource);
            conn.commit();
        } catch (SQLException e) {
            logger.warn("Error occurred finding ACLs ", e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                    conn = null;
                }
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }

        return retVal;
    }
    
    protected String[] discoverACLs(Connection conn, ResourceImpl resource)
            throws SQLException {

        String query = "select distinct r.uri as uri from ACL_ENTRY a inner join VORTEX_RESOURCE r "
            + "on a.resource_id = r.resource_id "
            + "where r.resource_id in ("
            + "select resource_id  from VORTEX_RESOURCE where uri like ?)";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt = conn.prepareStatement(query);
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


    public void copy(ResourceImpl resource, String destURI, boolean copyACLs,
                     boolean setOwner, String owner) throws IOException {
        Connection conn = null;

        try {
            conn = getConnection();
            copy(conn, resource, destURI, copyACLs, setOwner, owner);
            long timestamp = System.currentTimeMillis();
            conn.commit();
            long duration = System.currentTimeMillis() - timestamp;
            System.out.println("__COPY: " + resource.getURI() + " -> " + destURI
                               + ": commit took " + duration + " ms");
            
        } catch (SQLException e) {
            logger.warn("Error occurred while copying resource " + resource, e);
            throw new IOException(e.getMessage());
        } catch (IOException e) {
            logger.warn("Error occurred while copying resource " + resource, e);
            throw e;
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                    conn = null;
                }
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }
    }
    
    private void copy(Connection conn, ResourceImpl resource, String destURI, boolean copyACLs,
                      boolean setOwner, String owner) throws SQLException, IOException {

        int depthDiff = getURIDepth(destURI) - getURIDepth(resource.getURI());
    
        String ownerVal = setOwner ? "'" + owner + "'" : "resource_owner";

        String query = "insert into vortex_resource (resource_id, prev_resource_id, "
            + "uri, depth, creation_time, content_last_modified, properties_last_modified, "
            + "content_modified_by, properties_modified_by, resource_owner, "
            + "display_name, content_language, content_type, character_encoding, "
            + "is_collection, acl_inherited) "
            + "select nextval('vortex_resource_seq_pk'), resource_id, "
            + "'" + destURI + "' || substring(uri, length('" + resource.getURI() + "') + 1), "
            + "depth + " + depthDiff + ", creation_time, content_last_modified, "
            + "properties_last_modified, " 
            + "content_modified_by, properties_modified_by, " + ownerVal + ", display_name, "
            + "content_language, content_type, character_encoding, is_collection, "
            + "acl_inherited from vortex_resource "
            + "where uri = '" + resource.getURI() + "'"
            + "or uri like '" + getURIWildcard(resource.getURI()) + "'";
        
        System.out.println("__: " + query);
        

        Statement stmt = conn.createStatement();
        stmt.executeUpdate(query);
        stmt.close();

        query = "insert into extra_prop_entry (extra_prop_entry_id, "
            + "resource_id, name_space, name, value) "
            + "select nextval('extra_prop_entry_seq_pk'), r.resource_id, p.name_space, "
            + "p.name, p.value from vortex_resource r inner join extra_prop_entry p "
            + "on r.prev_resource_id = p.resource_id where r.uri = '" + destURI
            + "' or r.uri like '" + getURIWildcard(destURI) + "' "
            + "and r.prev_resource_id is not null";
        
        
        stmt = conn.createStatement();
        stmt.executeUpdate(query);
        stmt.close();


        if (copyACLs) {

            query = "insert into acl_entry (acl_entry_id, resource_id, "
                + "action_type_id, user_or_group_name, is_user, granted_by_user_name, "
                + "granted_date) "
                + "select nextval('acl_entry_seq_pk'), r.resource_id, a.action_type_id, "
                + "a.user_or_group_name, a.is_user, a.granted_by_user_name, a.granted_date "
                + "from vortex_resource r inner join acl_entry a "
                + "on r.prev_resource_id = a.resource_id "
                + "where r.uri = '" + destURI
                + "' or r.uri like '" + getURIWildcard(destURI) + "' "
                + "and r.prev_resource_id is not null";
        }


        query = "update vortex_resource set prev_resource_id = null "
            + "where uri = '" + destURI + "' or uri like '" + destURI + "/%'";

        System.out.println("__: " + query);

        stmt = conn.createStatement();
        stmt.executeUpdate(query);
        stmt.close();

        long timestamp = System.currentTimeMillis();
        contentStore.copy(resource.getURI(), destURI);
        long duration = System.currentTimeMillis() - timestamp;

        if (logger.isDebugEnabled()) {
            logger.debug("Successfully copied '" + resource.getURI() + "' to '"
                         + destURI + "' in " + duration + " ms");
        }
            System.out.println("Successfully copied '" + resource.getURI() + "' to '"
                               + destURI + "' in " + duration + " ms");
    }
    
}
