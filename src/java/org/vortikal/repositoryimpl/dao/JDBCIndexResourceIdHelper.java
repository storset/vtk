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
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.vortikal.repositoryimpl.index.util.IndexResourceIdHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;

/**
 * Derive resource IDs (for index) from Vortex JDBC database.
 * @author oyviste
 */
public class JDBCIndexResourceIdHelper implements IndexResourceIdHelper, InitializingBean {
    
    Log logger = LogFactory.getLog(this.getClass());    
    private DataSource dataSource;
    
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void afterPropertiesSet() {
        if (this.dataSource == null) {
            throw new BeanInitializationException("JavaBean property 'dataSource' not set.");
        }
    }
    
    /**
     *
     */
    public String getResourceParentCollectionIds(String uri) {
        Connection conn = null;
        ResultSet rs = null;
        int id = -1;
        try {
            // Strip slash at end of uri, if any
            if (uri.endsWith("/") && uri.length() > 1) {
                uri = uri.substring(0, uri.length()-1);
            }
            
            conn = this.dataSource.getConnection();
            conn.setAutoCommit(false);
            
            // Get resource id
            Statement stmt = conn.createStatement();
            rs = stmt.executeQuery(
                    "SELECT resource_id FROM vortex_resource WHERE uri='" + uri +"'");
            
            if (rs.next()) {
                id = rs.getInt(1);
            } else {
                // Resource not found.
                stmt.close();
                rs.close();
                return ""; 
            }
            
            stmt.close();
            rs.close();
            
            // Get all parent IDs
            PreparedStatement pstmt =
                    conn.prepareStatement(
                    "SELECT parent_resource_id FROM parent_child WHERE " +
                    "child_resource_id=?");
            
            StringBuffer parentIds = new StringBuffer();
            pstmt.setInt(1, id);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                id = rs.getInt(1);
                parentIds.append(id);
                parentIds.append(" ");
                pstmt.setInt(1, id);
                rs = pstmt.executeQuery();
            }
            
            
//            Use stored procedure in database.
//            Statement stmt = conn.createStatement();
//            
//            ResultSet rs = null;
//            StringBuffer parentIds = new StringBuffer();
//            
//            rs = stmt.executeQuery("SELECT * FROM resource_parent_ids('" + uri + "')");
//            while (rs.next()) {
//                parentIds.append(rs.getString(1));
//                parentIds.append(" ");
//            }
//                
            
            pstmt.close();
            rs.close();
            
            return parentIds.toString();
 
        } catch (SQLException sqle) {
            logger.warn("SQLException while fetching parent collection IDs of URI " + uri, sqle);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException sqle) {
                logger.warn("SQLException while closing connection.", sqle);
            }
        }
        
        return null;
    }
    
    /** 
     *
     */
    public String getResourceId(String uri) {
        Connection conn = null;
        String id = null;
        try {
            // Strip slash at end of uri, if any
            if (uri.endsWith("/") && uri.length() > 1) {
                uri = uri.substring(0, uri.length()-1);
            }
            
            conn = this.dataSource.getConnection();
            conn.setAutoCommit(false);
            Statement stmt = conn.createStatement();
            ResultSet rs = 
                    stmt.executeQuery("SELECT resource_id FROM vortex_resource WHERE uri='" +
                                      uri + "'");
            
            if (rs.next()) {
                id = rs.getString(1);
            }
            
            stmt.close();
            rs.close();
        } catch (SQLException sqle) {
            logger.warn("SQLException while fetch resource ID of URI " + uri, sqle);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException sqle) {
                logger.warn("SQLException while closing connection.", sqle);
            }
        }
        
        return id;
    }
    
}
