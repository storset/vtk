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
import java.util.ArrayList;
import java.util.List;

import org.vortikal.repository.Comment;
import org.vortikal.repository.Resource;
import org.vortikal.repositoryimpl.store.CommentDAO;
import org.vortikal.repositoryimpl.store.DataAccessException;

public class SqlMapCommentDAO extends AbstractSqlMapDataAccessor implements CommentDAO {

    public List<Comment> listCommentsByResource(Resource resource) throws DataAccessException {
        try {
            String sqlMap = getSqlMap("listCommentsByResource");

            List<Comment> comments = new ArrayList<Comment>();
            List theComments =
                this.sqlMapClient.queryForList(sqlMap, resource);
            for (Object o: theComments) {
                Comment comment = (Comment) o;
                comments.add(comment);
            }
            return comments;

        } catch (SQLException e) {
            throw new DataAccessException(
                "Error occurred while listing comments for resource " + resource, e);
        } 
    }

    public void delete(Comment comment) throws RuntimeException {
        try {

            String sqlMap = getSqlMap("deleteComment");
            this.sqlMapClient.delete(sqlMap, comment);
            
        } catch (SQLException e) {
            throw new DataAccessException(
                "Error occurred while deleting comment: " + comment, e);
        } 
    }
    
    public void deleteAll(Resource resource) throws RuntimeException {
        try {
            this.sqlMapClient.startTransaction();

            String sqlMap = getSqlMap("deleteAllComments");
            this.sqlMapClient.delete(sqlMap, resource);
            
        } catch (SQLException e) {
            throw new DataAccessException(
                "Error occurred while deleting all comments on resource " + resource, e);
        } finally {
            try {
                this.sqlMapClient.endTransaction();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Comment create(Resource resource, Comment comment) throws RuntimeException {
        try {

            String sqlMap = getSqlMap("insertComment");
            this.sqlMapClient.insert(sqlMap, comment);

            // XXX: define new semantics for creating a new comment:
            // client should first obtain a new unique ID, then call
            // create(comment).

            return comment;
            
        } catch (SQLException e) {
            throw new DataAccessException(
                "Error occurred while creating comment: "
                + comment + " on resource " + resource, e);
        } 
    }
    

    public Comment update(Comment comment) throws RuntimeException {
        try {

            String sqlMap = getSqlMap("updateComment");
            this.sqlMapClient.update(sqlMap, comment);

            sqlMap = getSqlMap("loadCommentById");
            comment = (Comment) this.sqlMapClient.queryForObject(sqlMap, new Integer(comment.getID()));
            return comment;

        } catch (SQLException e) {
            throw new DataAccessException(
                "Error occurred while updating comment: " + comment, e);
        } 
    }

}
