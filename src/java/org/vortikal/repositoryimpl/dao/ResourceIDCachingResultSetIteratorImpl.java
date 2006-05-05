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

import org.vortikal.repository.Property;
import org.vortikal.repositoryimpl.PropertyManagerImpl;
import org.vortikal.repositoryimpl.PropertySetImpl;
import org.vortikal.security.PrincipalManager;

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
public class ResourceIDCachingResultSetIteratorImpl implements ResultSetIterator {

    private ResultSet rs;
    private PropertyManagerImpl propertyManager;
    private PrincipalManager principalManager;
    
    private boolean hasNext = true;
    
    private String currentURI = null;
    
    private Map resourceIdCache;
    
    public ResourceIDCachingResultSetIteratorImpl(PropertyManagerImpl propertyManager,
                                 PrincipalManager principalManager, ResultSet rs) throws IOException {
        this.propertyManager = propertyManager;
        this.principalManager = principalManager;
        this.rs = rs;
        this.resourceIdCache = new HashMap();
        
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

            JDBCClient.populateStandardProperties(this.propertyManager, this.principalManager,
                                                  propertySet, this.rs);

            putResourceId(currentURI, rs.getInt("resource_id"));
            
            propertySet.setAncestorIds(getAncestorIdsForUri(currentURI));
            
            Map propMap = new HashMap();

            while (currentURI.equals(uri)) {

                JDBCClient.PropHolder holder = new JDBCClient.PropHolder();
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
                JDBCClient.PropHolder holder = (JDBCClient.PropHolder) i.next();
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
    
    private void putResourceId(String uri, int resourceId) {
        this.resourceIdCache.put(uri, new Integer(resourceId));
    }
    
    private int[] getAncestorIdsForUri(String uri) throws IllegalStateException {
        List ancestorUris = new ArrayList();
        
        if (uri.equals("/")) return new int[0];
        
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

}
