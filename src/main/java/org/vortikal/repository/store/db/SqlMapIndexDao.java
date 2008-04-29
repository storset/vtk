/* Copyright (c) 2007, University of Oslo, Norway
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
package org.vortikal.repository.store.db;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.ibatis.SqlMapClientCallback;
import org.springframework.orm.ibatis.SqlMapClientTemplate;
import org.vortikal.repository.ChangeLogEntry;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.search.query.security.ResultSecurityInfo;
import org.vortikal.repository.store.IndexDao;
import org.vortikal.repository.store.PropertySetHandler;
import org.vortikal.security.PrincipalFactory;

import com.ibatis.sqlmap.client.SqlMapExecutor;

/**
 * Index data accessor based on iBatis.
 */
public class SqlMapIndexDao extends AbstractSqlMapDataAccessor implements IndexDao {

    private static final Log LOG = LogFactory.getLog(SqlMapIndexDao.class);
    
    private int loggerId;
    private int loggerType;

    private int queryAuthorizationBatchSize = 1000;

    private ResourceTypeTree resourceTypeTree;

    private PrincipalFactory principalFactory;
    
    public void orderedPropertySetIteration(PropertySetHandler handler) 
        throws DataAccessException { 

        SqlMapClientTemplate client = getSqlMapClientTemplate();
        String statementId = getSqlMap("orderedPropertySetIteration");

        ResourceIdCachingPropertySetRowHandler rowHandler = 
            new ResourceIdCachingPropertySetRowHandler(
                handler, this.resourceTypeTree, this.principalFactory);

        client.queryWithRowHandler(statementId, rowHandler);

        rowHandler.handleLastBufferedRows();
    }
    
    public void orderedPropertySetIteration(String startUri, PropertySetHandler handler) 
        throws DataAccessException {
        
        SqlMapClientTemplate client = getSqlMapClientTemplate();

        String statementId = getSqlMap("orderedPropertySetIterationWithStartUri");

        PropertySetRowHandler rowHandler = new PropertySetRowHandler(handler,
                this.resourceTypeTree, this.principalFactory);

        Map<String, Object> parameters = new HashMap<String, Object>();

        parameters.put("uri", startUri);
        parameters.put("uriWildcard", SqlDaoUtils.getUriSqlWildcard(startUri,
                AbstractSqlMapDataAccessor.SQL_ESCAPE_CHAR));

        client.queryWithRowHandler(statementId, parameters, rowHandler);

        rowHandler.handleLastBufferedRows();
    }
    
    public void orderedPropertySetIterationForUris(final List<String> uris, 
                                              PropertySetHandler handler)
        throws DataAccessException {
        
        if (uris.size() == 0) {
            return;
        }
        
        SqlMapClientTemplate client = getSqlMapClientTemplate();

        String getSessionIdStatement = getSqlMap("nextTempTableSessionId");

        final Integer sessionId = (Integer) client
                .queryForObject(getSessionIdStatement);

        final String insertUriTempTableStatement = getSqlMap("insertUriIntoTempTable");

        Integer batchCount = (Integer) client
                .execute(new SqlMapClientCallback() {

                    public Object doInSqlMapClient(SqlMapExecutor sqlMapExec)
                            throws SQLException {

                        sqlMapExec.startBatch();
                        for (String uri : uris) {
                            Map params = new HashMap();
                            params.put("sessionId", sessionId);
                            params.put("uri", uri);
                            sqlMapExec.insert(insertUriTempTableStatement,
                                    params);
                        }
                        int batchCount = sqlMapExec.executeBatch();
                        return new Integer(batchCount);
                    }
                });

        if (LOG.isDebugEnabled()) {
            LOG.debug("Number of inserts batched (uri list): " + batchCount);
        }

        String statement = getSqlMap("orderedPropertySetIterationForUris");

        PropertySetRowHandler rowHandler = new PropertySetRowHandler(handler,
                this.resourceTypeTree, this.principalFactory);

        client.queryWithRowHandler(statement, sessionId, rowHandler);

        rowHandler.handleLastBufferedRows();

        // Clean-up temp table
        statement = getSqlMap("deleteFromTempTableBySessionId");
        client.delete(statement, sessionId);

    }

    private class ResultAuthorizationSqlMapClientCallback 
        implements SqlMapClientCallback {

        private List<ResultSecurityInfo> batch;
        private Set<Integer> allIds;
        private Integer sessionId;
        private int numberOfEntriesInserted = 0;
        
        public ResultAuthorizationSqlMapClientCallback(List<ResultSecurityInfo> batch, 
                                                Set<Integer> allIds,
                                                Integer sessionId) {
            this.batch = batch;
            this.allIds = allIds;
            this.sessionId = sessionId;
        }
        
        public Object doInSqlMapClient(SqlMapExecutor sqlMapExec) throws SQLException {

            Map params = new HashMap();
            String statement = getSqlMap("insertResourceIdIntoTempTable");
            
            sqlMapExec.startBatch();
            for (ResultSecurityInfo rsi: this.batch) {
                
                Integer id = rsi.getAclNodeId();
                if (! this.allIds.add(id)){
                    continue;
                }
                
                params.put("sessionId", this.sessionId);
                params.put("resourceId", id);
                sqlMapExec.insert(statement, params);
                params.clear();
                ++this.numberOfEntriesInserted;
            }
            int batchCount = sqlMapExec.executeBatch();
            
            return new Integer(batchCount);
        }
        
        public int getNumberOfEntriesInserted() {
            return this.numberOfEntriesInserted;
        }
        
    }
    
    public void processQueryResultsAuthorization(
                            Set<String> principalNames,
                            List<ResultSecurityInfo> rsiList)
        throws DataAccessException {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Processing list of " 
                    + rsiList.size() + " elements");
        }
        
        SqlMapClientTemplate client = getSqlMapClientTemplate();

        String statement = getSqlMap("nextTempTableSessionId");
        
        Integer sessionId = (Integer)client.queryForObject(statement);
        
        Set<Integer> authorizedIds = new HashSet<Integer>();
        Set<Integer> allIds = new HashSet<Integer>();
        
        int batchSize = this.queryAuthorizationBatchSize;
        int batchStart = 0;
        int batchEnd = batchSize > rsiList.size() ? rsiList.size() : batchSize;
        for (;;) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Processing batch " + batchStart + " to " + batchEnd);
            }

            ResultAuthorizationSqlMapClientCallback callback = new ResultAuthorizationSqlMapClientCallback(
                    rsiList.subList(batchStart, batchEnd), allIds, sessionId);

            Integer batchCount = (Integer) client.execute(callback);
            int n = callback.getNumberOfEntriesInserted();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Number of inserts batched (query auth): "
                        + batchCount);
            }

            Map params = new HashMap();
            statement = getSqlMap("queryResultAuthorization");
            if (n > 0) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Need to check " + n + " entries in database");
                }

                params.put("sessionId", sessionId);
                // XXX: No no no, iBATIS does not allow Set value iteration,
                // must wrap in List implementation... :(
                params.put("principalNames", new ArrayList(principalNames));

                List<Integer> authorizedList = (List<Integer>) client
                        .queryForList(statement, params);

                for (Integer authorizedId : authorizedList) {
                    boolean added = authorizedIds.add(authorizedId);
                    if (LOG.isDebugEnabled()) {

                        if (added) {
                            LOG.debug("Adding ACL node id " + authorizedId
                                    + " to set of authorized IDs");
                        }

                        LOG.debug("Current ACL node id: " + authorizedId);
                    }
                }

                // Clean up temp table for current batch
                statement = getSqlMap("deleteFromTempTableBySessionId");
                client.delete(statement, sessionId);

            }

            // Process current batch
            for (int i = batchStart; i < batchEnd; i++) {
                ResultSecurityInfo rsi = rsiList.get(i);

                rsi.setAuthorized(authorizedIds.contains(rsi.getAclNodeId())
                        || principalNames.contains(rsi
                                .getOwnerAsUserOrGroupName()));

                if (LOG.isDebugEnabled()) {
                    if (rsi.isAuthorized()) {
                        LOG.debug("Authorized resource with ACL node id: "
                                + rsi.getAclNodeId());
                    } else {
                        LOG.debug("NOT authorized resource with ACL node id: "
                                + rsi.getAclNodeId());
                    }
                }
            }

            if (batchEnd == rsiList.size()) {
                break;
            }

            batchStart += batchSize;
            batchEnd = (batchEnd + batchSize) > rsiList.size() ? rsiList.size()
                    : batchEnd + batchSize;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Set of authorized IDs contains " + authorizedIds.size()
                    + " elements");
            LOG.debug("Set of all unique IDs contains " + allIds.size()
                    + " elements");
        }
            
        
    }
    
    public List<ChangeLogEntry> getLastChangeLogEntries()
        throws DataAccessException {
        
        SqlMapClientTemplate client = getSqlMapClientTemplate();

        String statement = getSqlMap("getMaxChangeLogEntryId");

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("loggerId", this.loggerId);
        params.put("loggerType", this.loggerType);
        Integer maxId = (Integer) client.queryForObject(statement, params);

        params.put("maxId", maxId);
        statement = getSqlMap("getLastChangeLogEntries");

        List<ChangeLogEntry> entries = client.queryForList(statement, params);

        return entries;
    }
    
    public void removeChangeLogEntries(List<ChangeLogEntry> entries)
        throws DataAccessException {
        
        int maxId = -1;
        for (ChangeLogEntry entry: entries) {
            maxId = Math.max(maxId, entry.getChangeLogEntryId());
        }
        
        SqlMapClientTemplate client = getSqlMapClientTemplate();

        String statement = getSqlMap("removeChangeLogEntries");
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("loggerId", this.loggerId);
        params.put("loggerType", this.loggerType);
        params.put("maxId", maxId);

        client.delete(statement, params);
    }


    public void setQueryAuthorizationBatchSize(int queryAuthorizationBatchSize) {
        this.queryAuthorizationBatchSize = queryAuthorizationBatchSize;
    }
    
    @Required
    public void setLoggerId(int loggerId) {
        this.loggerId = loggerId;
    }

    @Required
    public void setLoggerType(int loggerType) {
        this.loggerType = loggerType;
    }

    @Required
    public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
    }

    @Required
    public void setPrincipalFactory(PrincipalFactory principalFactory) {
        this.principalFactory = principalFactory;
    }

}

