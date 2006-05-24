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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.Property;
import org.vortikal.repositoryimpl.PropertyManager;
import org.vortikal.repositoryimpl.PropertySetImpl;
import org.vortikal.security.PrincipalManager;


/**
 * ResultSetIterator implementation
 *
 * XXX: exception handling
 */
public class ResultSetIteratorImpl implements ResultSetIterator {

    private static Log logger = LogFactory.getLog(ResultSetIteratorImpl.class);
    
    private ResultSet rs;
    private PropertyManager propertyManager;
    private PrincipalManager principalManager;
    
    private boolean hasNext = true;
    
    private String currentURI = null;
    
    
    public ResultSetIteratorImpl(PropertyManager propertyManager,
                                 PrincipalManager principalManager, ResultSet rs) throws IOException {
        this.propertyManager = propertyManager;
        this.principalManager = principalManager;
        this.rs = rs;

        try {
            this.hasNext = this.rs.next();
            this.hasNext = this.hasNext && ! rs.isAfterLast();
        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        }
    }
    

    public boolean hasNext() throws IOException {
        return this.hasNext;
    }
    

    public Object next() throws IOException {

        try {

            if (!this.hasNext) throw new IllegalStateException("No more objects");

            String uri = this.rs.getString("uri");
            currentURI = uri;

            PropertySetImpl propertySet = new PropertySetImpl(currentURI);

            SqlDaoUtils.populateStandardProperties(this.propertyManager, this.principalManager,
                                                  propertySet, this.rs);
            
            propertySet.setAncestorIds(getAncestorIdsFromString(rs.getString("ancestor_ids")));
            
            Map propMap = new HashMap();
            
            while (currentURI.equals(uri)) {

                SqlDaoUtils.PropHolder holder = new SqlDaoUtils.PropHolder();
                holder.namespaceUri = rs.getString("name_space");
                holder.name = rs.getString("name");
                holder.resourceId = rs.getInt("resource_id");
            
                if (holder.name != null) { 

                    List values = (List) propMap.get(holder);
                    if (values == null) {
                        values = new ArrayList();
                        holder.type = rs.getInt("prop_type_id");
                        holder.values = values;
                        propMap.put(holder, values);
                    }
                    values.add(rs.getString("value"));
                }

                if (!this.rs.isLast()) {
                    this.rs.next();
                    uri = rs.getString("uri");
                } else {
                    uri = null;
                    this.hasNext = false;
                }
            }

            for (Iterator i = propMap.keySet().iterator(); i.hasNext();) {
                SqlDaoUtils.PropHolder holder = (SqlDaoUtils.PropHolder) i.next();
                Property property = this.propertyManager.createProperty(
                    holder.namespaceUri, holder.name, 
                    (String[]) holder.values.toArray(new String[]{}), holder.type);
                propertySet.addProperty(property);
            }
            return propertySet;

        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        }
    }
    
    public void close() throws IOException {

        this.hasNext = false;

        Statement stmt = null;
        Connection conn = null;

        try {
            stmt = this.rs.getStatement();
            if (stmt != null) {
                conn = stmt.getConnection();
            }
            this.rs.close();
        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            } finally {
                try {
                    if (conn != null) {
                        conn.commit();
                        conn.close();
                    }
                } catch (SQLException e) {
                    throw new IOException(e.getMessage());
                }
            }
        }
    }
    
    /**
     * XXX: Horrible hack mapping from a single string of ids returned by database 
     * function for getting ancestor ids, to a Java int[] array. Needed because
     * we haven't ported Oracle-function to something more intelligent, and we
     * don't want to have two different Java-classes for Oracle and PostgreSQL.
     * 
     * Only temporary, until we get the id-problem 
     * sorted out, either by not using procedures/functions at all, or change
     * how we do the SQL/procedures.
     * 
     * @param idString White-space separated String of ancestor ids (parseable as integers) 
     * @return array of integers
     */
    private int[] getAncestorIdsFromString(String idString) {
        String[] ids = idString.trim().split(" ");
        
        int[] integerIds = new int[ids.length];
        for (int i=0; i<integerIds.length; i++) {
            integerIds[i] = Integer.parseInt(ids[i]);
        }
        
        return integerIds;
    }

}
