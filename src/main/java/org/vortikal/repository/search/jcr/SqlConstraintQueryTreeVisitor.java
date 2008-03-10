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

import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.search.query.AndQuery;
import org.vortikal.repository.search.query.NamePrefixQuery;
import org.vortikal.repository.search.query.NameRangeQuery;
import org.vortikal.repository.search.query.NameTermQuery;
import org.vortikal.repository.search.query.NameWildcardQuery;
import org.vortikal.repository.search.query.OrQuery;
import org.vortikal.repository.search.query.PropertyExistsQuery;
import org.vortikal.repository.search.query.PropertyPrefixQuery;
import org.vortikal.repository.search.query.PropertyRangeQuery;
import org.vortikal.repository.search.query.PropertyTermQuery;
import org.vortikal.repository.search.query.PropertyWildcardQuery;
import org.vortikal.repository.search.query.Query;
import org.vortikal.repository.search.query.QueryTreeVisitor;
import org.vortikal.repository.search.query.TermOperator;
import org.vortikal.repository.search.query.TypeTermQuery;
import org.vortikal.repository.search.query.UriDepthQuery;
import org.vortikal.repository.search.query.UriOperator;
import org.vortikal.repository.search.query.UriPrefixQuery;
import org.vortikal.repository.search.query.UriTermQuery;
import org.vortikal.repository.store.jcr.JcrDaoConstants;
import org.vortikal.repository.store.jcr.JcrPathUtil;

/**
 * A {@link QueryTreeVisitor} which generates a JCR1.0 SQL query constraint
 * string. 
 * 
 * XXX: Largely unfinished. This class should build JCR SQL-equivalent constraints
 *      for our own query node types.
 *
 */
public class SqlConstraintQueryTreeVisitor implements QueryTreeVisitor {

    private static final Log LOG = LogFactory.getLog(
                                            SqlConstraintQueryTreeVisitor.class);
    
    private int currentDepth = 0;
    private ResourceTypeTree resourceTypeTree;
    
    public SqlConstraintQueryTreeVisitor(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
    }
    
    private StringBuilder checkDataParam(Object data) {
        if (data == null) {
            return new StringBuilder();
        } else if (data instanceof StringBuilder) {
            return (StringBuilder)data;
        } else {
            throw new IllegalArgumentException("Visitor data parameter must be an instance of StringBuilder");
        }
    }
    
    public Object visit(AndQuery andQuery, Object data) throws UnsupportedQueryException {

        StringBuilder buffer = checkDataParam(data);
        
        buffer.append(" ");
        if (this.currentDepth > 0) buffer.append("(");
        Iterator<Query> iterator = andQuery.getQueries().iterator();
        ++this.currentDepth;
        while (iterator.hasNext()) {
            Query query = iterator.next();
            buffer.append(query.accept(this, data));
            
            if (iterator.hasNext()) {
                buffer.append(" AND ");
            }
        }
        --this.currentDepth;
        if (this.currentDepth > 0) buffer.append(")");
        buffer.append(" ");
        
        return buffer;
    }

    public Object visit(OrQuery orQuery, Object data) throws UnsupportedQueryException {
        StringBuilder buffer = checkDataParam(data);
        
        buffer.append(" ");
        if (this.currentDepth > 0) buffer.append("(");
        Iterator<Query> iterator = orQuery.getQueries().iterator();
        ++this.currentDepth;
        while (iterator.hasNext()) {
            Query query = iterator.next();
            buffer.append(query.accept(this, data));
            
            if (iterator.hasNext()) {
                buffer.append(" OR ");
            }
        }
        --this.currentDepth;
        if (this.currentDepth > 0) buffer.append(")");
        buffer.append(" ");
        
        return buffer;
    }
    

    public Object visit(NamePrefixQuery npQuery, Object data) throws UnsupportedQueryException {
        throw new UnsupportedQueryException("NamePrefixQuery not supported, yet");
    }

    public Object visit(NameRangeQuery nrQuery, Object data) throws UnsupportedQueryException {
        throw new UnsupportedQueryException("NameRangeQuery not supported, yet");
    }

    public Object visit(NameWildcardQuery nwQuery, Object data) throws UnsupportedQueryException {
        throw new UnsupportedQueryException("NameWildcardQuery not supported, yet");
    }

    public Object visit(NameTermQuery ntQuery, Object data) throws UnsupportedQueryException {
        throw new UnsupportedQueryException("NameTermQuery not supported, yet");
    }

    public Object visit(PropertyExistsQuery peQuery, Object data) throws UnsupportedQueryException {
        StringBuilder buffer = checkDataParam(data);
        
        PropertyTypeDefinition def = peQuery.getPropertyDefinition();
        
        String propName = def.getName();
        String prefix = def.getNamespace().getPrefix();
        if (prefix != null) {
            propName = prefix + JcrDaoConstants.VRTX_PREFIX_SEPARATOR + propName;
        }
        propName = JcrDaoConstants.VRTX_PREFIX + propName;
        
        buffer.append(" ");
        
        buffer.append(propName).append(" ");
        if (peQuery.isInverted()) {
            buffer.append(SqlConstraintOperator.IS_NULL);
        } else {
            buffer.append(SqlConstraintOperator.IS_NOT_NULL);
        }
        
        buffer.append(" ");
        
        return buffer;
    }

    public Object visit(PropertyPrefixQuery ppQuery, Object data) throws UnsupportedQueryException {
        throw new UnsupportedQueryException("PropertyPrefixQuery not supported, yet");
    }

    public Object visit(PropertyRangeQuery prQuery, Object data) throws UnsupportedQueryException {
        throw new UnsupportedQueryException("PropertyRangeQuery not supported, yet");
    }

    public Object visit(PropertyTermQuery ptQuery, Object data) throws UnsupportedQueryException {
        throw new UnsupportedQueryException("PropertyTermQuery not supported, yet");
    }

    public Object visit(PropertyWildcardQuery pwQuery, Object data) throws UnsupportedQueryException {
        throw new UnsupportedQueryException("PropertyWildcardQuery not supported, yet");
    }

    public Object visit(TypeTermQuery ttQuery, Object data) throws UnsupportedQueryException {
        StringBuilder buffer = checkDataParam(data);
        
        String typeName = ttQuery.getTerm();
        TermOperator operator = ttQuery.getOperator();
        
        buffer.append(" ");
        if (operator == TermOperator.NI) {
            buffer.append(SqlConstraintOperator.NOT).append(" (");
        } else if (operator != TermOperator.IN){
            throw new UnsupportedQueryException("Unsupported type operator: "+ operator);
        }

        buffer.append(JcrDaoConstants.RESOURCE_TYPE).append(" = ");
        buffer.append("'").append(typeName).append("'");
        
        // Get list of descendant type names
        // XXX: method in ResourceTypeTree is named wrongly, it does not return self, only descendants.
        List<String> descendantNames = this.resourceTypeTree.getDescendantsAndSelf(typeName);
        if (descendantNames != null && !descendantNames.isEmpty()) {
            buffer.append(" OR ");
            Iterator iterator = descendantNames.iterator();
            while (iterator.hasNext()) {
                buffer.append(JcrDaoConstants.RESOURCE_TYPE).append(" = ");
                buffer.append("'").append(iterator.next()).append("'");
                if (iterator.hasNext()){
                    buffer.append(" OR ");
                }
            }
        }
        
        if (operator == TermOperator.NI) {
            buffer.append(")");
        }
        buffer.append(" ");
        
        return buffer;
    }

    public Object visit(UriDepthQuery udQuery, Object data) throws UnsupportedQueryException {
        throw new UnsupportedQueryException("UriDepthQuery not supported, yet");
    }

    public Object visit(UriPrefixQuery upQuery, Object data) throws UnsupportedQueryException {
        // XXX: Descendant or self does not work because of JCR query limitations on the jcr:path 
        //      pseudo property.
        //      This means only descendants will be provided, not self.
        
        // XXX: Path prefix queries are quite slow with JackRabbit (or so it seems).
        
        // XXX: Inversion on path prefix seems to *not* work at all (looks like
        //      JackRabbit ignores NOT before a LIKE on jcr:path).
        
        StringBuilder buffer = checkDataParam(data);
        
        String jcrPathPrefix = JcrPathUtil.uriToPath(upQuery.getUri());
        
        buffer.append(" ").append("jcr:path ");
        if (upQuery.isInverted()) {
            buffer.append(SqlConstraintOperator.NOT).append(" ");
        }
        buffer.append(SqlConstraintOperator.LIKE).append(" ");
        buffer.append("'").append(jcrPathPrefix);        
        if (!jcrPathPrefix.endsWith("/")) {
            buffer.append("/");
        }
        buffer.append("%' ");
           
        return buffer;
    }

    public Object visit(UriTermQuery utQuery, Object data) throws UnsupportedQueryException {
        
        StringBuilder buffer = checkDataParam(data);
        
        UriOperator op = utQuery.getOperator();
        buffer.append(" ");
        buffer.append("jcr:path ");
        if (op == UriOperator.EQ) {
            buffer.append(SqlConstraintOperator.EQUAL);
        } else if (op == UriOperator.NE){
            buffer.append(SqlConstraintOperator.NOT_EQUAL); 
        } else {
            throw new UnsupportedQueryException("Unsupported UriOperator: " + op);
        }
        buffer.append(" ");
        
        String jcrPath = JcrPathUtil.uriToPath(utQuery.getUri());
        
        buffer.append("'").append(jcrPath).append("'");
        
        return buffer;
    }

}
