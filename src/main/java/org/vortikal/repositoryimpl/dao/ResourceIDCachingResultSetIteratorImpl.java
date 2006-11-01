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
 * This iterator caches IDs of resources and uses them to set the
 * 'ancestorIds' property of <code>PropertySetImpl</code> objects.
 * The underlying <code>ResultSet</code> does not need to provide
 * the 'ancestor_ids' column.
 * 
 * It can only be used if the two following conditions are met:
 * 1) The URIs must come in such an order that ancestors always come 
 *    before their predecessors in the iteration (parent URI always before child).
 * 2) For any given URI, every ancestor up to '/' must be included as part
 *    of the <code>ResultSet</code>.
 *    
 * This class is meant to optimize for <code>PropertySet</code> iteration
 * when doing a complete re-indexing, or any case where an ordered complete traversal
 * over all <code>PropertySet</code>s in the database is done.
 * 
 * XXX: exception handling
 * 
 * @author oyviste
 *
 */
public class ResourceIDCachingResultSetIteratorImpl implements Iterator {

    Log logger = LogFactory.getLog(ResourceIDCachingResultSetIteratorImpl.class);
    
    private ResultSet rs;
    private PropertyManager propertyManager;
    private PrincipalFactory principalFactory;
    
    private boolean hasNext = true;
    
    private String currentURI = null;
    
    private Map resourceIdCache;
    
    public ResourceIDCachingResultSetIteratorImpl(PropertyManager propertyManager,
                                 PrincipalFactory principalFactory, ResultSet rs) throws IOException {
        this.propertyManager = propertyManager;
        this.principalFactory = principalFactory;
        this.rs = rs;
        this.resourceIdCache = new HashMap();
        
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

    public Object next()  {

        try {

            if (!this.hasNext) throw new NoSuchElementException("No more objects");

            String uri = this.rs.getString("uri");
            
            this.currentURI = uri;

            PropertySetImpl propertySet = new PropertySetImpl(this.currentURI);

            SqlDaoUtils.populateStandardProperties(this.propertyManager, this.principalFactory,
                                                  propertySet, this.rs);

            putResourceId(this.currentURI, this.rs.getInt("resource_id"));
            
            propertySet.setAncestorIds(getAncestorIdsForUri(this.currentURI));
            
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

// OLD (not supported by Oracle):
//                 if (!this.rs.isLast()) {
//                     this.rs.next();
//                     uri = rs.getString("uri");
//                 } else {
//                     uri = null;
//                     this.hasNext = false;
//                 }
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
    
    private void putResourceId(String uri, int resourceId) {
        this.resourceIdCache.put(uri, new Integer(resourceId));
    }
    
    private int[] getAncestorIdsForUri(String uri) throws IllegalStateException {
        List ancestorUris = new ArrayList();
        
        if ("/".equals(uri)) return new int[0];
        
        int from = uri.length();
        while (from > 0) {
            from = uri.lastIndexOf('/', from);
            if (from == 0) {
                ancestorUris.add("/");
            } else {
                ancestorUris.add(uri.substring(0, from--));
            }
        }
        
        int[] ids = new int[ancestorUris.size()];
        
        int c = 0;
        for (Iterator i = ancestorUris.iterator(); i.hasNext();) {
            String ancestor = (String)i.next();
            Integer id = (Integer)this.resourceIdCache.get(ancestor);
            
            if (id == null) {
                throw new IllegalStateException("Needed ID for URI '" + uri 
                        + "', but this URI has not yet been encountered in the iteration.");
            }
            
            ids[c++] = id.intValue();
        }
        
        return ids;
    }
    
    public void remove() {
        throw new UnsupportedOperationException("This iterator does not support removal of elements");
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


    public void setPrincipalFactory(PrincipalFactory principalFactory) {
        this.principalFactory = principalFactory;
    }

}
