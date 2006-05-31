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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.PropertySet;
import org.vortikal.repositoryimpl.PropertyManager;
import org.vortikal.security.PrincipalManager;

import com.ibatis.sqlmap.client.SqlMapClient;

public class IndexDataAccessorImpl implements IndexDataAccessor, InitializingBean {
    
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
        }
        
        if (this.sqlDialect.equals(SQL_DIALECT_ORACLE)) {
            this.oracle = true;
        }
    }

    public ResultSetIterator getOrderedPropertySetIterator() throws IOException {
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
    
    
    public ResultSetIterator getOrderedPropertySetIterator(String startURI) throws IOException {
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
            
            ResultSetIterator iterator =  new ResultSetIteratorImpl(
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
    
    /**
     * 
     */
    public ResultSetIterator getPropertySetIteratorForURIs(List uris) throws IOException {
        
        if (uris.size() == 0) {
            throw new IllegalArgumentException("At least one URI must be specified for retrieval.");
        }
        
        Connection conn;
        try {
            conn = this.dataSource.getConnection();
            conn.setAutoCommit(false);

            int sessionId = insertIntoTempTable(uris, conn);
            
            String query = "select resource_ancestor_ids(r.uri) AS ancestor_ids, r.*, p.* "
                         + "from vortex_uri_tmp vu, vortex_resource r "
                         + "left outer join extra_prop_entry p on r.resource_id = p.resource_id "
                         + "where r.uri = vu.uri AND vu.session_id=? "
                         + "order by p.resource_id, p.extra_prop_entry_id";            
            
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, sessionId);
            
//          Old IN (.., .., ..) style query code:            
//            int n = uris.size();
//            StringBuffer query = 
//                new StringBuffer("select resource_ancestor_ids(r.uri) AS ancestor_ids, r.*, p.* from vortex_resource r "
//                + "left outer join extra_prop_entry p on r.resource_id = p.resource_id "
//                + "where r.uri in (");
//            for (int i=0; i<n; i++) {
//                query.append("?");
//                if (i < n-1) query.append(",");
//            }
//            query.append(") order by r.uri, p.extra_prop_entry_id");
//
//            PreparedStatement stmt = conn.prepareStatement(query.toString());
//            n = 1;
//            for (Iterator i = uris.iterator(); i.hasNext();) {
//                String uri = (String)i.next();
//                stmt.setString(n++, uri);
//            }

            ResultSet rs = pstmt.executeQuery();
            
            // Remove rows from temporary URI table
            PreparedStatement deleteFromTempStmt = conn.prepareStatement(
                    "DELETE FROM vortex_uri_tmp WHERE session_id=?");
            deleteFromTempStmt.setInt(1, sessionId);
            deleteFromTempStmt.executeUpdate();
//             deleteFromTempStmt.close();
            
//             conn.commit();
            
            return new ResultSetIteratorImpl(this.propertyManager, this.principalManager,
                                             rs, deleteFromTempStmt, conn);
        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        }
    }
    
    private int insertIntoTempTable(List uris, Connection conn)
        throws SQLException {
        
        String nextvalQuery = this.oracle ? 
                "SELECT vortex_uri_tmp_session_id_seq.nextval FROM dual" :
                "SELECT nextval('vortex_uri_tmp_session_id_seq')";
        
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
        
        pstmt = conn.prepareStatement(
                "INSERT INTO vortex_uri_tmp(session_id, uri) VALUES (?,?)");
        
        for (Iterator i = uris.iterator(); i.hasNext();) {
            pstmt.setInt(1, sessionId);
            pstmt.setString(2, (String)i.next());
            pstmt.addBatch();
        }
        
        pstmt.executeBatch();
        pstmt.close();
        
        return sessionId;
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

    private String getSqlMap(String statementId) {
        if (this.sqlMaps.containsKey(statementId)) {
            return (String) this.sqlMaps.get(statementId);
        }
        return statementId;
    }

}
