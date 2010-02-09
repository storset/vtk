/* Copyright (c) 2008, University of Oslo, Norway
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
package org.vortikal.repository.search.jcr;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.search.QueryException;
import org.vortikal.repository.search.ResultSet;
import org.vortikal.repository.search.ResultSetImpl;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.Searcher;
import org.vortikal.repository.store.DataAccessException;
import org.vortikal.repository.store.jcr.JcrDao;



/**
 * Experimental JCR searcher implementation using JCR-1.0 SQL syntax for searching
 * a JCR repository. 
 * 
 * Does not work very well, yet. Currently very untested.
 * 
 * - No result set size limiting.
 * - No ACL checking.
 *
 */
public class JcrSqlSearcherImpl implements Searcher {
    
    private static final Log LOG = LogFactory.getLog(JcrSqlSearcherImpl.class);
    
    private JcrDao jcrDao;
    private ResourceTypeTree resourceTypeTree;

    public ResultSet execute(String token, Search search) throws QueryException {

        SqlConstraintQueryTreeVisitor constraintBuilder 
            = new SqlConstraintQueryTreeVisitor(this.resourceTypeTree);
        
        SqlQueryAssembler assembler = new SqlQueryAssembler(search, constraintBuilder);
        
        String sqlQuery = assembler.getAssembledQuery();
        
        LOG.debug("Original search: " + search);
        LOG.debug("Assembled SQL query: " + sqlQuery);
        
        ResultSetImpl resultSet = new ResultSetImpl();

        Session session = null;
        try {
            session = this.jcrDao.getSession();
            QueryManager qm = session.getWorkspace().getQueryManager();
        
            Query query = qm.createQuery(sqlQuery, javax.jcr.query.Query.SQL);
            QueryResult result = query.execute();
            
            NodeIterator nodes = result.getNodes();

            while (nodes.hasNext()) {
                Node node = nodes.nextNode();
                if (node.isNodeType("vrtx:hierarchyNode")) {
                    resultSet.addResult(this.jcrDao.nodeToResource(node));
                } else {
                    LOG.warn("Unexpected node type in result set: " + node.getPrimaryNodeType().getName());
                }
            }

            resultSet.setTotalHits(resultSet.getSize());
        } catch (RepositoryException re) {
            throw new QueryException("JCR RepositoryException: " + re.getMessage());
        } catch (DataAccessException dae) {
            throw new QueryException("DataAccessException", dae);
        } finally {
            if (session != null) session.logout();
        }

        LOG.debug("Got " + resultSet.getSize() + " query results.");
        return resultSet;
    }
            
    @Required
    public void setJcrDao(JcrDao jcrDao) {
        this.jcrDao = jcrDao;
    }
    
    @Required
    public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
    }

}
