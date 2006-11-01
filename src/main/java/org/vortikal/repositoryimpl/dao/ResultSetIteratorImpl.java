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
import java.util.NoSuchElementException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.Property;
import org.vortikal.repositoryimpl.PropertyManager;
import org.vortikal.repositoryimpl.PropertySetImpl;
import org.vortikal.security.PrincipalFactory;


/**
 * ResultSetIterator implementation
 *
 * XXX: exception handling
 */
public class ResultSetIteratorImpl implements Iterator {

    private static Log logger = LogFactory.getLog(ResultSetIteratorImpl.class);
    
    private ResultSet rs;
    private Statement stmt;
    private Connection conn;

    private PropertyManager propertyManager;
    private PrincipalFactory principalFactory;
    
    private boolean hasNext = true;
    
    private String currentURI = null;
    
    
    public ResultSetIteratorImpl(PropertyManager propertyManager,
                                 PrincipalFactory principalFactory,
                                 ResultSet rs,
                                 Statement stmt,
                                 Connection conn) throws IOException {
        this.propertyManager = propertyManager;
        this.principalFactory = principalFactory;
        this.rs = rs;
        this.stmt = stmt;
        this.conn = conn;
        try {
            this.hasNext = this.rs.next();
            this.hasNext = this.hasNext && ! rs.isAfterLast();
        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        }
    }
    

    public boolean hasNext() {
        return this.hasNext;
    }
    

    public Object next() {

        try {

            if (!this.hasNext) throw new NoSuchElementException("No more objects");

            String uri = this.rs.getString("uri");
            if (logger.isDebugEnabled()) {
                logger.debug("next(): uri = '" + uri + "'");
            }

            this.currentURI = uri;

            PropertySetImpl propertySet = new PropertySetImpl(this.currentURI);

            SqlDaoUtils.populateStandardProperties(this.propertyManager, this.principalFactory,
                                                  propertySet, this.rs);
            
            propertySet.setAncestorIds(getAncestorIdsFromString(this.rs.getString("ancestor_ids")));
            
            Map propMap = new HashMap();
            
            while (this.currentURI.equals(uri)) {

                SqlDaoUtils.PropHolder holder = new SqlDaoUtils.PropHolder();
                holder.namespaceUri = this.rs.getString("name_space");
                holder.name = this.rs.getString("name");
                holder.resourceId = this.rs.getInt("resource_id");
            
                if (holder.name != null) { 

                    List values = (List) propMap.get(holder);
                    if (values == null) {
                        values = new ArrayList();
                        holder.type = this.rs.getInt("prop_type_id");
                        holder.values = values;
                        propMap.put(holder, values);
                    }
                    values.add(this.rs.getString("value"));
                }

                if (this.rs.next()) {
                    uri = this.rs.getString("uri");
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
            logger.warn("SQLException during iteration", e);
            return null;
        }
    }
    
    public void close() throws IOException {

        this.hasNext = false;

        try {
            this.rs.close();
        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        } finally {
            try {
                if (this.stmt != null) {
                    this.stmt.close();
                }
                
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            } finally {
                try {
                    if (this.conn != null) {
                        this.conn.commit();
                        this.conn.close();
                    }
                } catch (SQLException e) {
                    throw new IOException(e.getMessage());
                }
            }
        }
    }
    
    public void remove() {
        throw new UnsupportedOperationException("This iterator does not support removal of elements");
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
     * @param idString Single-space separated string of ancestor ids 
     *                 (where ids are parseable integers) 
     * @return array of integers
     */
    private int[] getAncestorIdsFromString(String idString) {
        if (idString == null || "".equals(idString)) {
            return new int[0];
        }
        
        idString = idString.trim();
        
        int n = 1, p = -1, c = 0;
        while ((p = idString.indexOf(' ', p+1)) != -1) ++n;
        
        int[] ids = new int[n];
        
        p = 0; n = -1;
        while ((n = idString.indexOf(' ', (p = n+1))) != -1) {
            ids[c++] = Integer.parseInt(idString.substring(p, n));
        }
        ids[c] = Integer.parseInt(idString.substring(p, idString.length()));
        
        return ids;
    }

}
