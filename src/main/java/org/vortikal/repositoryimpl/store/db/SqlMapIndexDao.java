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
package org.vortikal.repositoryimpl.store.db;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repositoryimpl.PropertyManager;
import org.vortikal.repositoryimpl.store.DataAccessException;
import org.vortikal.security.PrincipalFactory;

import com.ibatis.sqlmap.client.SqlMapClient;

/**
 * New index data accessor based on iBatis
 * 
 * @author oyviste
 *
 */
public class SqlMapIndexDao extends AbstractSqlMapDataAccessor implements IndexDao {

    Log logger = LogFactory.getLog(IndexDataAccessorImpl.class);
    
    private PropertyManager propertyManager;
    private PrincipalFactory principalFactory;

    private int queryAuthorizationBatchSize = 1000;

    public void orderedPropertySetIteration(PropertySetHandler handler) 
        throws DataAccessException { 

        try {
            SqlMapClient client = getSqlMapClient();
            String statementId = getSqlMap("orderedPropertySetIteration");
            
            ResourceIdCachingPropertySetRowHandler rowHandler = 
                new ResourceIdCachingPropertySetRowHandler(handler,
                        this.propertyManager, this.principalFactory);
                
            client.queryWithRowHandler(statementId, rowHandler);
            
            rowHandler.handleLastBufferedRows();
            
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }

    }
    
    public void orderedPropertySetIteration(String startUri, PropertySetHandler handler) 
        throws DataAccessException {
        
        try {
            SqlMapClient client = getSqlMapClient();
            String statementId = getSqlMap("orderedPropertySetIterationWithStartUri");
            
            PropertySetRowHandler rowHandler =
                new PropertySetRowHandler(handler, this.propertyManager, 
                                       this.principalFactory);

            Map<String, Object> parameters = new HashMap<String, Object>();
            
            parameters.put("uri", startUri);
            parameters.put("uriWildcard", SqlDaoUtils.getUriSqlWildcard(startUri, 
                                        SqlMapDataAccessor.SQL_ESCAPE_CHAR));
            
            
            client.queryWithRowHandler(statementId, parameters, rowHandler);
            
            rowHandler.handleLastBufferedRows();
            
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
        
    }
    
    public void orderedPropertySetIterationForUris(List<String> uris, 
                                              PropertySetHandler handler)
        throws DataAccessException {
        
        if (uris.size() == 0) {
            throw new IllegalArgumentException("URI list cannot be empty");
        }
        
        try {
            SqlMapClient client = getSqlMapClient();
            String getSessionIdStatement = getSqlMap("nextTempTableSessionId");
            
            Integer sessionId = (Integer)client.queryForObject(getSessionIdStatement);
            
            System.out.println("session id: " + sessionId);
            
            String insertUriTempTableStatement =
                    getSqlMap("insertIntoUriTempTable");
            
            client.startBatch();
            for (String uri: uris) {
                Map params = new HashMap();
                params.put("sessionId", sessionId);
                params.put("uri", uri);
                client.insert(insertUriTempTableStatement, params);
            }
            client.executeBatch();
            // XXX: batch execution DOES NOT WORK and I don't know why, yet.
            // No rows are inserted into database by client.executeBatch()
            
            String statement = getSqlMap("orderedPropertySetIterationForUris");
            
            PropertySetRowHandler rowHandler
             = new PropertySetRowHandler(handler, this.propertyManager, this.principalFactory);
            
            client.queryWithRowHandler(statement, sessionId, rowHandler);
            
            rowHandler.handleLastBufferedRows();
            
            // Clean-up temp table
            statement = getSqlMap("deleteFromUriTempTableBySessionId");
            client.delete(statement, sessionId);
            
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }
    

    @Required
    public void setPropertyManager(PropertyManager propertyManager) {
        this.propertyManager = propertyManager;
    }

    @Required
    public void setPrincipalFactory(PrincipalFactory principalFactory) {
        this.principalFactory = principalFactory;
    }

}

