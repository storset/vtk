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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repositoryimpl.ProcessedContentEventDumper;
import org.vortikal.repositoryimpl.RepositoryEventDumperImpl;
import org.vortikal.repositoryimpl.index.observation.ResourceACLModification;
import org.vortikal.repositoryimpl.index.observation.ResourceChange;
import org.vortikal.repositoryimpl.index.observation.ResourceChangeFetcher;
import org.vortikal.repositoryimpl.index.observation.ResourceContentModification;
import org.vortikal.repositoryimpl.index.observation.ResourceCreation;
import org.vortikal.repositoryimpl.index.observation.ResourceDeletion;
import org.vortikal.repositoryimpl.index.observation.ResourcePropModification;

/**
 * Fetch resource changes from database changelog.
 * 
 * <em>Important:</em> 
 * <p>Only entries produced by
 * {@link org.vortikal.repositoryimpl.RepositoryEventDumperImpl} or 
 * {@link org.vortikal.repositoryimpl.ProcessedContentEventDumper} are supported.
 * </p>
 * 
 * TODO: Convert to iBatis, if we decide to keep some of this in the future.
 * 
 * @author oyviste
 */
public class JDBCResourceChangeFetcher implements ResourceChangeFetcher, InitializingBean {
    
    Log logger = LogFactory.getLog(this.getClass());
    private DataSource dataSource;
    
    /**
     * Holds value of property loggerType, which determines what type of log entries
     * we fetch from the DB. Default is 2.
     */
    private int loggerType = 2;

    /**
     * Changelog consumer id
     */
    private int loggerId = 1;
        
    
    public void afterPropertiesSet() {
        if (this.dataSource == null) {
            throw new BeanInitializationException("JavaBean property 'dataSource' not set.");
        }
        
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("There are " + countPendingChanges()
                         + " pending changes in the changelog.");
        }
    }
    
    /**
     * Fetch all currently stored changes from changelog.
     *
     */
    public List fetchChanges() {
        Connection conn = null;
        
        List result = new ArrayList();
        try {
            conn = this.dataSource.getConnection();
            conn.setAutoCommit(false); 
           
            String query = "SELECT cl.* FROM changelog_entry cl " +
                           "WHERE cl.logger_type=? AND cl.logger_id=?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, this.loggerType);
            pstmt.setInt(2, this.loggerId);
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                ResourceChange c = getResourceChange(rs);
                result.add(c);
            }
            
            rs.close();
            pstmt.close();
        } catch (SQLException sqle) {
            this.logger.warn("SQLException: ", sqle);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                    conn = null;
                }
            } catch (SQLException sqle) {
                this.logger.warn("SQLException when closing connection: ", sqle);
            }
        }
        
        return result;
    }
    
    /**
     * Convert from changelog_entry in DB to ResourceChange object.
     * @param rs the ResultSet from the changelog_entry query.
     * @param conn a database connection (need for further queries).
     * @throws java.sql.SQLException
     * @return a ResourceChange object.
     */
    private ResourceChange getResourceChange(ResultSet rs)
    throws SQLException {
        
        ResourceChange c = null;
        String op = rs.getString("operation");
        if (op.equals(RepositoryEventDumperImpl.CREATED)) {
            c = new ResourceCreation();
        } else if (op.equals(RepositoryEventDumperImpl.DELETED)) {
            c = new ResourceDeletion();
            ((ResourceDeletion)c).setResourceId(rs.getString("resource_id"));
        } else if (op.equals(RepositoryEventDumperImpl.MODIFIED_PROPS)) {
            c = new ResourcePropModification();
        } else if (op.equals(RepositoryEventDumperImpl.MODIFIED_CONTENT)) {
            c = new ResourceContentModification();
        } else if (op.equals(RepositoryEventDumperImpl.ACL_MODIFIED)
                  || op.equals(ProcessedContentEventDumper.ACL_READ_ALL_NO) // Stay compatible with ProcessedContentEventDumper
                  || op.equals(ProcessedContentEventDumper.ACL_READ_ALL_YES)) {
            c = new ResourceACLModification();
        } else {
            logger.warn("Unknown operation '" + op + "' in database changelog."
                     +  "Make sure logger id and logger type are configured correctly.");
            throw new SQLException("Unknown operation '" + op + 
                                                    "' in database changelog"); 
        }
        
        c.setUri(rs.getString("uri"));
        c.setId(rs.getInt("changelog_entry_id"));
        c.setTimestamp(rs.getTimestamp("timestamp").getTime());
        c.setLoggerId(rs.getInt("logger_id"));
        c.setLoggerType(rs.getInt("logger_type"));
        c.setCollection(rs.getString("is_collection").equals("Y"));

        return c;
    }
    
    /**
     * Fetch at most n stored changes.
     *
     */
    public List fetchChanges(int n) {
        throw new UnsupportedOperationException("Method not implemented.");
    }
    
    /**
     * Fetch only the most recent change for every resource, ignoring previous
     * changes for a given resource. The set of resource changes should thus
     * only contain uniqe IDs.
     *
     */
    public List fetchLastChanges() {
        Connection conn = null;
        int maxId = -1;
        
        List result = null;
        try {
            conn = this.dataSource.getConnection();
            conn.setAutoCommit(false);
                
            // Fetch all relevant changes up to MAX(changelog_entry_id)
            String maxIdQuery = "SELECT MAX(cl.changelog_entry_id) FROM changelog_entry cl " +
                     "WHERE cl.logger_type=? AND cl.logger_id=?";
            
            PreparedStatement pstmt = conn.prepareStatement(maxIdQuery);
            pstmt.setInt(1, this.loggerType);
            pstmt.setInt(2, this.loggerId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                maxId = rs.getInt(1);
                this.logger.debug("MAX(changelog_entry_id)=" + maxId);
            }
            rs.close();
            pstmt.close();

            String changelogEntriesQuery = 
                "SELECT cl1.* "
                + "FROM changelog_entry cl1 "
                + "WHERE "
                + "cl1.operation = 'deleted' AND cl1.is_collection = 'Y' "
                // This fixes a subtle problem regarding deletion
                // of collections. The difference between collection deletions
                // and every other operation, is that the event doesn't map 1:1 with
                // the number of affected resources, but rather 1:N. A collection deletion
                // can involve many resources in a tree. 
                // Therefore, we must make sure that we don't miss any of 
                // these events. Only picking the very latest event for collections, 
                // can result in masking earlier deletion events for collections
                // whose names were the same as newly created collections.
                // This will cause the indexing system to miss these important events, 
                // and the old tree will remain in the index, except for the replaced parent node.
                + "AND cl1.changelog_entry_id <= ? "
                + "AND cl1.logger_id = ? "
                + "AND cl1.logger_type = ? "
                + "OR " 
                + "cl1.changelog_entry_id IN " 
                + "( SELECT MAX(cl2.changelog_entry_id) "
                + "  FROM changelog_entry cl2 "
                + "  WHERE cl1.uri = cl2.uri " 
                + "  AND cl2.logger_id = ? " 
                + "  AND cl2.logger_type = ? "
                + "  AND cl2.changelog_entry_id <= ? ) "

                + "ORDER BY cl1.changelog_entry_id";
                // Changed ORDER BY to order by event id, instead of URI. It makes more sense
                // to list events in the order they happened, rather than what the URI looks like.
                // The URI is only an ID treated specially, and it doesn't
                // necessarily need a parent URI to exist in the index. The index is not a file
                // system, only a collection of documents, which happen to have an URI.
                // Should be more efficient to sort by a number, instead of string, as well.
            pstmt = conn.prepareStatement(changelogEntriesQuery);
            pstmt.setInt(1, maxId);
            pstmt.setInt(2, this.loggerId);
            pstmt.setInt(3, this.loggerType);
            pstmt.setInt(4, this.loggerId);
            pstmt.setInt(5, this.loggerType);
            pstmt.setInt(6, maxId);
            
            // NOTE: We cannot avoid adding resources to the list of changes that might
            //       be deleted by the time the resource change consumer gets the change
            //       events (it's impossible to predict later deletions based on the currently selected
            //       set of rows). Thus, such situations must be handled appropriately by the consumers.
            
            rs = pstmt.executeQuery();
            
            result = new ArrayList();

            while (rs.next()) {
                ResourceChange c = getResourceChange(rs);
                result.add(c);
            }
            
            rs.close();
            pstmt.close();
        } catch (SQLException sqle) {
                this.logger.warn("SQLException: ", sqle);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                    conn = null;
                }
            } catch (SQLException sqle) {
                this.logger.warn("SQLException when closing connection: ", sqle);
            }
        }
        
        return result;
    }
    
    /**
     * Fetch at most n of only the most recent change for every resource.
     *
     */
    public List fetchLastChanges(int n) {
        throw new UnsupportedOperationException("Method not implemented.");
    }
    
    /**
     * Count number of pending changes in the database changelog.
     *
     */
    public int countPendingChanges() {
        Connection conn = null;
        int result = -1;
        try {
            conn = this.dataSource.getConnection();
            conn.setAutoCommit(false);
            String query =
                    "SELECT COUNT(1) FROM changelog_entry cl WHERE cl.logger_type =? " + 
                    "AND cl.logger_id =?";
            
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, this.loggerType);
            pstmt.setInt(2, this.loggerId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                result = rs.getInt(1);
            }
            
            rs.close();
            pstmt.close();
        } catch (SQLException sqle) {
            this.logger.warn("SQLException: ", sqle);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                    conn = null;
                }
            } catch (SQLException sqle) {
                this.logger.warn("SQLException while closing connection: ", sqle);
            }
        }
        
        return result;
    }
    
    /**
     * Notify of the changes that have been indexed, so that they can be removed
     * from storage. Any stored earlier changes to the resources should also be
     * removed from storage.
     * 
     * NOTE: Assuming that bigger changelog_entry_id always means later in time.
     */
    public void removeChanges(List changes) {
        Connection conn = null;
        try {
            conn = this.dataSource.getConnection();
            conn.setAutoCommit(false);
            // Delete indexed changes to resources, including all those that
            // occured before the last change to a resource. This should be OK to do.
            // NOTE: might be cases where the above is not true, need to consider this some more.
            
            int maxId = -1;
            for (Iterator iterator = changes.iterator(); iterator.hasNext();) {
                ResourceChange c = (ResourceChange) iterator.next();
                maxId = Math.max(maxId, c.getId());
            }
            
            if (maxId == -1) return;

            String query = 
                    "DELETE FROM changelog_entry WHERE changelog_entry_id <= ? " + 
                    "AND logger_type=? " +
                    "AND logger_id=?";
            
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, maxId);
            pstmt.setInt(2, this.loggerType);
            pstmt.setInt(3, this.loggerId);
            pstmt.execute();
            conn.commit();
            pstmt.close();
        } catch (SQLException sqle) {
            this.logger.warn("SQLException: ", sqle);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                    conn = null;
                }
            } catch (SQLException sqle) {
                this.logger.warn("SQLException when closing connection: ", sqle);
            }
        }
    }
    
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public int getLoggerType() {
        return this.loggerType;
    }

    public void setLoggerType(int loggerType) {
        this.loggerType = loggerType;
    }


    public void setLoggerId(int loggerId)  {

        this.loggerId = loggerId;
    }

    public int getLoggerId()  {

        return this.loggerId;
    }
}
