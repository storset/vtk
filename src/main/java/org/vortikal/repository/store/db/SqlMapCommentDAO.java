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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Comment;
import org.vortikal.repository.Path;
import org.vortikal.repository.Resource;
import org.vortikal.repository.store.CommentDAO;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalFactory;

public class SqlMapCommentDAO extends AbstractSqlMapDataAccessor implements CommentDAO {

    private PrincipalFactory principalFactory;
    
    @Required
    public void setPrincipalFactory(PrincipalFactory principalFactory) {
        this.principalFactory = principalFactory;
    }

    @Override
    public int getNumberOfComments(Resource resource) throws RuntimeException {
        String sqlMap = getSqlMap("numberOfCommentsByResource");

        return (Integer) getSqlMapClientTemplate().queryForObject(sqlMap, resource.getURI().toString());
    }
    
    @Override
    public List<Comment> listCommentsByResource(Resource resource,
            boolean deep, int max) throws RuntimeException {
        String sqlMap = deep ?
                getSqlMap("listCommentsByResourceRecursively") :
                getSqlMap("listCommentsByResource");

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("uri", resource.getURI().toString());
        parameters.put("max", max);
        parameters.put("uriWildcard", 
                SqlDaoUtils.getUriSqlWildcard(resource.getURI(), 
                        SQL_ESCAPE_CHAR));
        
        List<Comment> comments = new ArrayList<Comment>();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> theComments =
            getSqlMapClientTemplate().queryForList(sqlMap, parameters);

        for (Map<String, Object> commentMap: theComments) {
            Comment c = new Comment();
            c.setID(commentMap.get("ID").toString());
            c.setURI(Path.fromString((String) commentMap.get("URI")));
            c.setTime((Date) commentMap.get("time"));
            Principal author = this.principalFactory.getPrincipal((String) commentMap.get("author"), Principal.Type.USER);
            c.setAuthor(author);
            c.setContent((String) commentMap.get("content"));
            // XXX: title, approved

            comments.add(c);
        }
        return comments;
    }

    @Override
    public void deleteComment(Comment comment) {
        String sqlMap = getSqlMap("deleteComment");
        getSqlMapClientTemplate().delete(sqlMap, Integer.valueOf(comment.getID()));
    }
    
    @Override
    public void deleteAllComments(Resource resource) {
        String sqlMap = getSqlMap("deleteAllComments");
        getSqlMapClientTemplate().delete(sqlMap, resource);
    }

    @Override
    public Comment createComment(Comment comment) {
        String sqlMap = getSqlMap("insertComment");
        getSqlMapClientTemplate().insert(sqlMap, comment);
        // XXX: define new semantics for creating a new comment:
        // client should first obtain a new unique ID, then call
        // create(comment).
        return comment;
    }
    
    @Override
    public Comment updateComment(Comment comment) {
        String sqlMap = getSqlMap("updateComment");
        getSqlMapClientTemplate().update(sqlMap, comment);
        sqlMap = getSqlMap("loadCommentById");
        comment = (Comment) getSqlMapClientTemplate().queryForObject(sqlMap, Integer.valueOf(comment.getID()));
        return comment;
    }

}
