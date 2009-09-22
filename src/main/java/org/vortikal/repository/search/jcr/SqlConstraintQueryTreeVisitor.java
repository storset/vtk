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

import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.search.query.ACLExistsQuery;
import org.vortikal.repository.search.query.ACLInheritedFromQuery;
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
import org.vortikal.repository.search.query.UriPrefixQuery;
import org.vortikal.repository.search.query.UriSetQuery;
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
 * XXX: All properties are added as JCR-strings in JcrDao. This results in:
 *      - No sensible sorting possible on non-string types.
 *      - No sensible range/greater/less comparisons on non-string types.
 *      
 * XXX: Add option of flagging that post-processing of query result set is
 *      necessary, because some queries cannot be represented properly
 *      as JCR-SQL.
 *
 */
public class SqlConstraintQueryTreeVisitor implements QueryTreeVisitor {

    // The character used as an escape in SQL LIKE expresssions.
    private static final char SQL_LIKE_ESCAPE_CHAR = '@';
        
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
                buffer.append(" ").append(SqlConstraintOperator.AND).append(" ");
            }
        }
        --this.currentDepth;
        if (this.currentDepth > 0) buffer.append(")");
        
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
                buffer.append(" ").append(SqlConstraintOperator.OR).append(" ");
            }
        }
        --this.currentDepth;
        if (this.currentDepth > 0) buffer.append(")");
        
        return buffer;
    }
    

    public Object visit(NamePrefixQuery npQuery, Object data) throws UnsupportedQueryException {
        StringBuilder buffer = checkDataParam(data);
        
        String nameValuePrefix = npQuery.getTerm();
        
        buffer.append(" ");
        if (npQuery.isInverted()) {
            buffer.append(SqlConstraintOperator.UNARY_NOT).append(" (");
        }
        
        buffer.append(JcrDaoConstants.RESOURCE_NAME).append(" ");
        buffer.append(SqlConstraintOperator.LIKE).append(" ");
        buffer.append("'").append(escapeLIKEValue(nameValuePrefix)).append("%' ");
        buffer.append(SqlConstraintOperator.LIKE_ESCAPE).append(" '").append(SQL_LIKE_ESCAPE_CHAR).append("'");
        
        if (npQuery.isInverted()) {
            buffer.append(")");
        }
        
        return buffer;
    }

    public Object visit(NameRangeQuery nrQuery, Object data) throws UnsupportedQueryException {
        StringBuilder buffer = checkDataParam(data);
        
        String nameValueFrom = nrQuery.getFromTerm();
        String nameValueTo = nrQuery.getToTerm();
        
        buffer.append(" ");
        buffer.append(JcrDaoConstants.RESOURCE_NAME).append(" ");
        if (nrQuery.isInclusive()) {
            buffer.append(SqlConstraintOperator.GREATER_EQUAL).append(" ");
        } else {
            buffer.append(SqlConstraintOperator.GREATER).append(" ");
        }
        buffer.append("'").append(nameValueFrom).append("'").append(" ");
        buffer.append(SqlConstraintOperator.AND).append(" ");
        
        buffer.append(JcrDaoConstants.RESOURCE_NAME).append(" ");
        if (nrQuery.isInclusive()) {
            buffer.append(SqlConstraintOperator.LESS_EQUAL).append(" ");
        } else {
            buffer.append(SqlConstraintOperator.LESS).append(" ");
        }
        buffer.append("'").append(nameValueTo).append("'");
        
        return buffer;
    }

    public Object visit(NameWildcardQuery nwQuery, Object data) throws UnsupportedQueryException {
        throw new UnsupportedQueryException("NameWildcardQuery not supported, yet");
    }

    public Object visit(NameTermQuery ntQuery, Object data) throws UnsupportedQueryException {
        StringBuilder buffer = checkDataParam(data);
        
        TermOperator op = ntQuery.getOperator();
        String nameValue = ntQuery.getTerm();
        
        buffer.append(" ");
        buffer.append(JcrDaoConstants.RESOURCE_NAME).append(" ");
        buffer.append(getBinaryConstraintOperator(op)).append(" ");
        buffer.append("'").append(nameValue).append("'");
        
        return buffer;
    }

    public Object visit(PropertyExistsQuery peQuery, Object data) throws UnsupportedQueryException {
        StringBuilder buffer = checkDataParam(data);
        
        String propName = getJcrPropertyName(peQuery.getPropertyDefinition());
        
        buffer.append(" ");
        
        buffer.append(propName);
        if (peQuery.isInverted()) {
            buffer.append(" ").append(SqlConstraintOperator.UNARY_IS_NULL).append(" ");
        } else {
            buffer.append(" ").append(SqlConstraintOperator.UNARY_IS_NOT_NULL).append(" ");
        }
        
        return buffer;
    }

    public Object visit(PropertyPrefixQuery ppQuery, Object data) throws UnsupportedQueryException {
        
        StringBuilder buffer = checkDataParam(data);
        
        String propValuePrefix = ppQuery.getTerm();
        String propName = getJcrPropertyName(ppQuery.getPropertyDefinition());
        
        buffer.append(" ");
        if (ppQuery.isInverted()) {
            buffer.append(SqlConstraintOperator.UNARY_NOT).append(" (");
        }
        buffer.append(propName).append(" ");
        buffer.append(SqlConstraintOperator.LIKE).append(" ");
        buffer.append("'").append(escapeLIKEValue(propValuePrefix)).append("%' ");
        buffer.append(SqlConstraintOperator.LIKE_ESCAPE).append(" '").append(SQL_LIKE_ESCAPE_CHAR).append("'");
        if (ppQuery.isInverted()) {
            buffer.append(")");
        }
        
        return buffer;
    }

    public Object visit(PropertyRangeQuery prQuery, Object data) throws UnsupportedQueryException {
        throw new UnsupportedQueryException("PropertyRangeQuery not supported, yet");
    }
    
    public Object visit(UriSetQuery usQuery, Object data) throws UnsupportedQueryException {
        throw new UnsupportedQueryException("UriSetQuery not supported");
    }

    public Object visit(PropertyTermQuery ptQuery, Object data) throws UnsupportedQueryException {
        StringBuilder buffer = checkDataParam(data);
        
        PropertyTypeDefinition def = ptQuery.getPropertyDefinition();
        String value = ptQuery.getTerm();
        String propName = getJcrPropertyName(def);
        
        TermOperator op = ptQuery.getOperator();
        
        buffer.append(" ");
        buffer.append(propName);
        buffer.append(" ");
        buffer.append(getBinaryConstraintOperator(op));
        buffer.append(" ");
        
        buffer.append(getSqlPropertyValueExpression(value));
        
        return buffer;
    }

    public Object visit(PropertyWildcardQuery pwQuery, Object data) throws UnsupportedQueryException {
        throw new UnsupportedQueryException("PropertyWildcardQuery is not supported, yet.");
    }

    public Object visit(TypeTermQuery ttQuery, Object data) throws UnsupportedQueryException {
        StringBuilder buffer = checkDataParam(data);
        
        String typeName = ttQuery.getTerm();
        TermOperator operator = ttQuery.getOperator();
        
        buffer.append(" ");
        if (operator == TermOperator.NI) {
            buffer.append(SqlConstraintOperator.UNARY_NOT).append(" ");
        } else if (operator != TermOperator.IN){
            throw new UnsupportedQueryException("Unsupported type operator: "+ operator);
        }
        
        buffer.append("(");
        buffer.append(JcrDaoConstants.RESOURCE_TYPE).append(" ");
        buffer.append(SqlConstraintOperator.EQUAL).append(" ");
        buffer.append("'").append(typeName).append("'");
        
        // Get list of descendant type names
        // XXX: method in ResourceTypeTree is named wrongly, it does not return self, only descendants.
        List<String> descendantNames = this.resourceTypeTree.getResourceTypeDescendants(typeName);
        if (descendantNames != null && !descendantNames.isEmpty()) {
            buffer.append(" ").append(SqlConstraintOperator.OR).append(" ");
            Iterator<String> iterator = descendantNames.iterator();
            while (iterator.hasNext()) {
                buffer.append(JcrDaoConstants.RESOURCE_TYPE).append(" ");
                buffer.append(SqlConstraintOperator.EQUAL).append(" ");
                buffer.append("'").append(iterator.next()).append("'");
                if (iterator.hasNext()){
                    buffer.append(" ").append(SqlConstraintOperator.OR).append(" ");
                }
            }
        }
        buffer.append(")");        
        
        return buffer;
    }

    public Object visit(UriDepthQuery udQuery, Object data) throws UnsupportedQueryException {
        throw new UnsupportedQueryException("UriDepthQuery is currently not supported.");
    }
    
    public Object visit(ACLExistsQuery aclExistsQuery, Object data) throws
        UnsupportedOperationException {
        throw new UnsupportedQueryException("ACLExistsQuery is currently not supported.");
    }

    public Object visit(ACLInheritedFromQuery aclIHFQuery, Object data) throws
        UnsupportedOperationException {
        throw new UnsupportedQueryException("ACLInheritedFromQuery is currently not supported.");
    }
    
    public Object visit(UriPrefixQuery upQuery, Object data) throws UnsupportedQueryException {
        // XXX: Descendant or self does not work because of JCR query limitations on the jcr:path 
        //      pseudo property.
        //      This means only descendants will be provided, not self.
        
        // XXX: Path prefix queries are quite slow with JackRabbit (or so it seems).
        
        // XXX: Inversion does not work at all for jcr:path.
        
        StringBuilder buffer = checkDataParam(data);
        
        String jcrPathPrefix = JcrPathUtil.uriToPath(upQuery.getUri());
        
        buffer.append(" ");
        if (upQuery.isInverted()) {
            buffer.append(SqlConstraintOperator.UNARY_NOT).append(" (");
        }
        
        buffer.append("jcr:path ");
        buffer.append(SqlConstraintOperator.LIKE).append(" ");
        buffer.append("'").append(escapeLIKEValue(jcrPathPrefix));        
        if (!jcrPathPrefix.endsWith("/")) {
            buffer.append("/");
        }
        buffer.append("%' ").append(SqlConstraintOperator.LIKE_ESCAPE).append(" ");
        buffer.append("'").append(SQL_LIKE_ESCAPE_CHAR).append("'");
        
        if (upQuery.isInverted()) {
            buffer.append(")");
        }

        return buffer;
    }

    public Object visit(UriTermQuery utQuery, Object data) throws UnsupportedQueryException {
        
        // XXX: inversion is not supported.
        
        StringBuilder buffer = checkDataParam(data);
        
        TermOperator op = utQuery.getOperator();
        buffer.append(" ");
        if (TermOperator.NE.equals(op)){
            buffer.append(SqlConstraintOperator.UNARY_NOT).append(" (");
        } else if (TermOperator.EQ.equals(op)){
            throw new UnsupportedQueryException("Unsupported UriOperator: " + op);
        }
        
        String jcrPath = JcrPathUtil.uriToPath(utQuery.getUri());
        buffer.append("jcr:path ").append(SqlConstraintOperator.EQUAL).append(" ");
        buffer.append("'").append(jcrPath).append("'");
        
        if (TermOperator.NE.equals(op)) {
            buffer.append(")");
        }
        
        return buffer;
    }
    
    private String getJcrPropertyName(PropertyTypeDefinition def){
        String propName = def.getName();
        String prefix = def.getNamespace().getPrefix();
        if (prefix != null) {
            propName = prefix + JcrDaoConstants.VRTX_PREFIX_SEPARATOR + propName;
        }
       
        return JcrDaoConstants.VRTX_PREFIX + propName;
    }
    
    private SqlConstraintOperator getBinaryConstraintOperator(TermOperator op) 
        throws UnsupportedQueryException {
        
        SqlConstraintOperator sqlOp = null;
        
        if (op == TermOperator.EQ) {
            sqlOp = SqlConstraintOperator.EQUAL;            
        } else if (op == TermOperator.NE) {
            sqlOp = SqlConstraintOperator.NOT_EQUAL;
        } else if (op == TermOperator.GE) {
            sqlOp = SqlConstraintOperator.GREATER_EQUAL;
        } else if (op == TermOperator.GT) {
            sqlOp = SqlConstraintOperator.GREATER;
        } else if (op == TermOperator.LE) {
            sqlOp = SqlConstraintOperator.LESS_EQUAL;
        } else if (op == TermOperator.LT) {
            sqlOp = SqlConstraintOperator.LESS;
        } else {
            throw new UnsupportedQueryException("Unsupported term operator: " + op);
        }
        
        return sqlOp;
    }
    
    private String escapeLIKEValue(String value) {
        
        StringBuilder buffer = new StringBuilder(value.length());
        
        for (char c: value.toCharArray()) {
            switch (c){
            case '%':
                buffer.append(SQL_LIKE_ESCAPE_CHAR).append(c); break;
            case '_':
                buffer.append(SQL_LIKE_ESCAPE_CHAR).append(c); break;
            case SQL_LIKE_ESCAPE_CHAR:
                buffer.append(SQL_LIKE_ESCAPE_CHAR).append(c); break;
            default:
                buffer.append(c);
            }
        }
        
        return buffer.toString();
    }
    
    private String getSqlPropertyValueExpression(String termValue) { 

        // XXX: JcrDao adds all node properties as type strings.
        // Therefore, there is not yet support for sorting on numbers, dates and so on.
        // Neither is range queries or greater/less comparisons on these types.
        return "'" + termValue + "'";
        
//        switch (def.getType()) {
//            case HTML:
//            case STRING:
//            case IMAGE_REF:
//            case TIMESTAMP: // This one probably needs additional formatting ..
//            case DATE:      // This one probably needs additional formatting ..
//                return "'" + termValue + "'";
//                
//            case INT:
//            case LONG:
//            case BOOLEAN:
//                return termValue;
//            
//            default:
//                return "'" + termValue + "'";
//        }

    }
    
}
