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


import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.PropertySet;
import org.vortikal.repositoryimpl.PropertyManager;
import org.vortikal.repositoryimpl.query.security.ResultSecurityInfo;
import org.vortikal.security.PrincipalManager;

import com.ibatis.sqlmap.client.SqlMapClient;

/**
 * 
 *
 */
public class IndexDataAccessorImpl implements IndexDataAccessor, InitializingBean {
    
    Log logger = LogFactory.getLog(IndexDataAccessorImpl.class);
    
    private PropertyManager propertyManager;
    private PrincipalManager principalManager;
    private DataSource dataSource;
    private SqlMapClient sqlMapClient;
    private Map sqlMaps;

    // XXX: Only temporary, until this class and result set iterators are 
    //      fully converted to iBATIS.
    public static final String SQL_DIALECT_DEFAULT = "default";
    public static final String SQL_DIALECT_ORACLE = "oracle";
    private String sqlDialect = SQL_DIALECT_DEFAULT;
    private boolean oracle = false;
    
    private int queryAuthorizationBatchSize = 1000;
    
    public void afterPropertiesSet() throws BeanInitializationException {
        if (this.propertyManager == null) {
            throw new BeanInitializationException("Property 'propertyManager' not set.");
        } else if (this.principalManager == null) {
            throw new BeanInitializationException("Property 'principalManager' not set.");
        } else if (this.dataSource == null) {
            throw new BeanInitializationException("Property 'dataSource' not set.");
        } else if (this.sqlMapClient == null) {
            throw new BeanInitializationException("Property 'sqlMapClient' not set.");
        } else if (this.sqlMaps == null) {
            throw new BeanInitializationException("Property 'sqlMaps' not set.");
        } else if (this.queryAuthorizationBatchSize < 1) {
            throw new BeanInitializationException("Query result authorization batch size must be 1 or greater");
        }
        
        if (this.sqlDialect.equals(SQL_DIALECT_ORACLE)) {
            this.oracle = true;
        }
    }

    public Iterator getOrderedPropertySetIterator() throws IOException {
        Connection conn = null;

        try {
            conn = this.dataSource.getConnection();
            conn.setAutoCommit(false);     
            
            String query = "select r.*, p.* from vortex_resource r "
                + "left outer join extra_prop_entry p on r.resource_id = p.resource_id "
                + "order by r.uri, p.extra_prop_entry_id";
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            
            return new ResourceIDCachingResultSetIteratorImpl(this.propertyManager, 
                                                              this.principalManager, rs);

        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        }
        
    }
    
    
    public Iterator getOrderedPropertySetIterator(String startURI) throws IOException {
        Connection conn = null;
        try {
            conn = this.dataSource.getConnection();
            conn.setAutoCommit(false);     
            
            String query = "select resource_ancestor_ids(r.uri) AS ancestor_ids, r.*, p.* from vortex_resource r "
                + "left outer join extra_prop_entry p  on r.resource_id = p.resource_id "
                + "where r.uri = ? or r.uri like ? order by r.uri, p.extra_prop_entry_id";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, startURI);
            stmt.setString(2, SqlDaoUtils.getUriSqlWildcard(startURI));
            ResultSet rs = stmt.executeQuery();
            
            return new ResultSetIteratorImpl(this.propertyManager, this.principalManager,
                                             rs, stmt, conn);
        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        }

    }
    
    
    public PropertySet getPropertySetForURI(String uri) throws IOException {
        Connection conn = null;
        try {
            conn = this.dataSource.getConnection();
            conn.setAutoCommit(false);     
            
            String query = "select resource_ancestor_ids(r.uri) AS ancestor_ids, r.*, p.* from vortex_resource r "
                + "left outer join extra_prop_entry p on r.resource_id = p.resource_id "
                + "where r.uri = ? order by p.extra_prop_entry_id";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, uri);
            ResultSet rs = stmt.executeQuery();
            
            ResultSetIteratorImpl iterator =  new ResultSetIteratorImpl(
                this.propertyManager, this.principalManager, rs, stmt, conn);
            
            PropertySet propSet = null;

            if (iterator.hasNext()) {
                propSet = (PropertySet) iterator.next();
            }
            iterator.close();
            return propSet;

        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        }
    }
    
    public Iterator getPropertySetIteratorForURIs(List uris) throws IOException {
        
        if (uris.size() == 0) {
            throw new IllegalArgumentException("At least one URI must be specified for retrieval.");
        }
        
        try {
            Connection conn = this.dataSource.getConnection();
            conn.setAutoCommit(false);

            int sessionId = getNewSessionId(conn);
            PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO vortex_tmp(session_id, uri) VALUES (?,?)");
            
            for (Iterator i = uris.iterator(); i.hasNext();) {
                pstmt.setInt(1, sessionId);
                pstmt.setString(2, (String)i.next());
                pstmt.addBatch();
            }
            
            pstmt.executeBatch();
            pstmt.close();            
            
            String query =
                "select resource_ancestor_ids(r.uri) AS ancestor_ids, r.*, p.* "
              + "from vortex_tmp vu, vortex_resource r "
              + "left outer join extra_prop_entry p on r.resource_id = p.resource_id "
              + "where r.uri = vu.uri AND vu.session_id=? "
              + "order by p.resource_id, p.extra_prop_entry_id";            
            
            pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, sessionId);
            
            ResultSet rs = pstmt.executeQuery();
            
            // Delete from vortex_tmp
            PreparedStatement deleteStmt = 
                conn.prepareStatement("DELETE FROM vortex_tmp WHERE session_id = ?");
            deleteStmt.setInt(1, sessionId);
            deleteStmt.executeUpdate();
            deleteStmt.close();
            
            return new ResultSetIteratorImpl(this.propertyManager, 
                                             this.principalManager,
                                             rs, pstmt, conn);
            
        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        }
    }

    private int getNewSessionId(Connection conn) throws SQLException  {
        
        String nextvalQuery = this.oracle ? 
                "SELECT vortex_tmp_session_id_seq.nextval FROM dual" :
                "SELECT nextval('vortex_tmp_session_id_seq')";
        
        PreparedStatement pstmt = conn.prepareStatement(nextvalQuery);
        ResultSet rs = pstmt.executeQuery();
        
        int sessionId = -1;
        if (rs.next()) {
            sessionId = rs.getInt(1);
        } else {
            throw new SQLException("Unable to get next value from session id sequence");
        }
        rs.close();
        pstmt.close();
        
        return sessionId;
    }
    
    private PreparedStatement buildAuthorizationQueryPreparedStatement(
                                                             int sessionId,
                                                             Set principalNames,
                                                             Connection conn) 
        throws SQLException {
        
        StringBuffer query = new StringBuffer();
        query.append("SELECT ace.resource_id FROM acl_entry ace, vortex_tmp vtmp ");
        query.append("WHERE ace.resource_id = vtmp.resource_id AND vtmp.session_id = ? ");
        query.append("AND ace.user_or_group_name IN ('pseudo:all'");
        if (principalNames.size() > 0) {
            query.append(",'pseudo:authenticated',");
        }
        for (int i=0; i<principalNames.size(); i++) {
            query.append("?");
            if (i < principalNames.size()-1) {
                query.append(",");
            }
        }
        query.append(") AND ace.action_type_id IN (1, 3, 4)");
        PreparedStatement pstmt = conn.prepareStatement(query.toString());
        
        int n = 1;
        pstmt.setInt(n++,  sessionId);
        for (Iterator i = principalNames.iterator(); i.hasNext();) {
            String name = (String)i.next();
            pstmt.setString(n++, name);
        }
        
        return pstmt;
    }
    
    /*
     *  (non-Javadoc)
     *  TODO: Optimize if possible (use different approaches, etc.)
     *        I think most of the time is spent hashing integers to 
     *        avoid unnecessary database lookups
     *        (which I'm guessing are even more expensive).
     * @see org.vortikal.repositoryimpl.dao.IndexDataAccessor#processQueryResultsAuthorization(java.util.List)
     */
    public void processQueryResultsAuthorization(Set principalNames,
                                                 List rsiList) 
        throws IOException {

        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Processing list of " + rsiList.size() + " elements");
        }
        
        Connection conn = null;
        try {
            conn = this.dataSource.getConnection();
            conn.setAutoCommit(false);
            
            int sessionId = getNewSessionId(conn);

            // Avoid looking up the same ID more than once in database by using sets
            // of all encountered uniqe IDs and authorized IDs.
            Set authorizedIds = new HashSet();
            Set allIds = new HashSet();
            
            PreparedStatement insertTmpStmt = conn.prepareStatement(
                "INSERT INTO vortex_tmp(session_id, resource_id) VALUES (?,?)");
            
            PreparedStatement authorizeStmt =
                 buildAuthorizationQueryPreparedStatement(sessionId, 
                                                             principalNames, conn);
            
            PreparedStatement deleteTmpStmt = conn.prepareStatement(
                "DELETE FROM vortex_tmp WHERE session_id = ?");
            deleteTmpStmt.setInt(1, sessionId);

            int batchSize = this.queryAuthorizationBatchSize;
            int batchStart = 0;
            int batchEnd = batchSize > rsiList.size() ? rsiList.size() : batchSize;
            for(;;) {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Processing batch " + batchStart + " to " + 
                            batchEnd);
                }
                
                int n = 0;
                for (int i = batchStart; i < batchEnd; i++) {
                    ResultSecurityInfo rsi = (ResultSecurityInfo) rsiList.get(i);
                    Integer id = rsi.getAclNodeId();
                    
                    if (! allIds.add(id)) {
                        continue;
                    }
                    
                    insertTmpStmt.setInt(1, sessionId);
                    insertTmpStmt.setInt(2, id.intValue());
                    insertTmpStmt.addBatch();
                    ++n;
                }

                if (n > 0) {
                    if (this.logger.isDebugEnabled()) {
                        this.logger.debug("Need to check " + n + " entries in database");
                    }
                    
                    // We need to do database lookup for some IDs
                    insertTmpStmt.executeBatch();
                    
                    ResultSet rs = authorizeStmt.executeQuery();
                    while (rs.next()) {
                        Integer id = new Integer(rs.getInt(1));
                        if (authorizedIds.add(id) && this.logger.isDebugEnabled()) {
                            this.logger.debug("Adding ACL node id " +
                                          id + " to set of authorized IDs");
                        }
                        
                        this.logger.debug("Current ACL node id: " + id);
                    }
                    rs.close();
                    
                    deleteTmpStmt.executeUpdate();
                }
                
                for (int i = batchStart; i < batchEnd; i++) {
                    ResultSecurityInfo rsi = (ResultSecurityInfo) rsiList.get(i);
                    
                    rsi.setAuthorized(
                            authorizedIds.contains(rsi.getAclNodeId())
                            || principalNames.contains(rsi.getOwnerAsUserOrGroupName()));
                    
                    if (this.logger.isDebugEnabled()) {
                        if (rsi.isAuthorized()) {
                            this.logger.debug("Authorized resource with ACL node id: " 
                                                                + rsi.getAclNodeId());
                        } else {
                            this.logger.debug("NOT authorized resource with ACL node id: " 
                                    + rsi.getAclNodeId());
                        }
                    }
                }
                
                if (batchEnd == rsiList.size()) {
                    break;
                }
                batchStart += batchSize;
                batchEnd = (batchEnd + batchSize) > rsiList.size() ? 
                        rsiList.size() : batchEnd + batchSize;
            } 
            
            insertTmpStmt.close();  
            authorizeStmt.close();
            deleteTmpStmt.close();

            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Set of authorized uniqe IDs contains " + authorizedIds.size() + " elements.");
                this.logger.debug("Set of all uniqe IDs contains " + allIds.size() + " elements.");
            }

        } catch (SQLException sqle) {
            throw new IOException(sqle.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.commit();
                    conn.close();
                } catch (SQLException sqle) {
                    this.logger.warn("SQLException while closing connection", sqle);
                }
            }
        }
    }
    
    public void close(Iterator iterator) throws IOException {
        if (iterator instanceof ResultSetIteratorImpl) {
            ((ResultSetIteratorImpl)iterator).close();
        } else if (iterator instanceof ResourceIDCachingResultSetIteratorImpl) {
            ((ResourceIDCachingResultSetIteratorImpl)iterator).close();
        } else {
            throw new IllegalArgumentException("Unknown iterator implementation");
        }
    }
    
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void setPropertyManager(PropertyManager propertyManager) {
        this.propertyManager = propertyManager;
    }
    
    public void setPrincipalManager(PrincipalManager principalManager) {
        this.principalManager = principalManager;
    }
    
    public void setSqlMapClient(SqlMapClient sqlMapClient) {
        this.sqlMapClient = sqlMapClient;
    }

    public void setSqlMaps(Map sqlMaps) {
        this.sqlMaps = sqlMaps;
    }
    
    public void setSqlDialect(String sqlDialect) {
        this.sqlDialect = sqlDialect;
    }

    // For iBATIS
    private String getSqlMap(String statementId) {
        if (this.sqlMaps.containsKey(statementId)) {
            return (String) this.sqlMaps.get(statementId);
        }
        return statementId;
    }

    public void setQueryAuthorizationBatchSize(int queryAuthorizationBatchSize) {
        this.queryAuthorizationBatchSize = queryAuthorizationBatchSize;
    }

}
