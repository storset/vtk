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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import javax.sql.DataSource;

import org.vortikal.repository.PropertySet;
import org.vortikal.repositoryimpl.PropertyManagerImpl;
import org.vortikal.security.PrincipalManager;

public class IndexDataAccessorImpl implements IndexDataAccessor {
    
    private PropertyManagerImpl propertyManager;
    private PrincipalManager principalManager;
    private DataSource dataSource;


    public ResultSetIterator getOrderedPropertySetIterator() throws IOException {
        Connection conn = null;

        try {
            conn = this.dataSource.getConnection();
            conn.setAutoCommit(false);     
            
            String query = "select r.*, p.* from vortex_resource r "
                + "left outer join extra_prop_entry p on r.resource_id = p.resource_id order by r.uri";
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            
            return new ResourceIDCachingResultSetIteratorImpl(this.propertyManager, this.principalManager, rs);

        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        }
        
    }
    
    
    public ResultSetIterator getOrderedPropertySetIterator(String startURI) throws IOException {
        Connection conn = null;
        try {
            conn = this.dataSource.getConnection();
            conn.setAutoCommit(false);     
            
            String query = "select resource_ancestor_ids(r.uri) AS ancestor_ids, r.*, p.* from vortex_resource r "
                + "left outer join extra_prop_entry p  on r.resource_id = p.resource_id "
                + "where r.uri = ? or r.uri like ? order by r.uri";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, startURI);
            stmt.setString(2, JDBCClient.getURIWildcard(startURI));
            ResultSet rs = stmt.executeQuery();
            
            return new ResultSetIteratorImpl(this.propertyManager, this.principalManager, rs);
        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        }

    }
    
    
    public PropertySet getPropertySetForURI(String uri) throws IOException {
        Connection conn = null;
        try {
            conn = this.dataSource.getConnection();
            conn.setAutoCommit(false);     
            
            String query = "select resource_ancestor_ids(r.uri) AS ancestor_ids, r.*, p.* from vortex_resource r "
                + "left outer join extra_prop_entry p on r.resource_id = p.resource_id "
                + "where r.uri = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, uri);
            ResultSet rs = stmt.executeQuery();
            
            ResultSetIterator iterator =  new ResultSetIteratorImpl(
                this.propertyManager, this.principalManager, rs);
            
            PropertySet propSet = null;

            if (iterator.hasNext()) {
                propSet = (PropertySet) iterator.next();
            }
            iterator.close();
            return propSet;

        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        }
    }
    
    public ResultSetIterator getPropertySetIteratorForURIs(List uris) throws IOException {
        
        if (uris.size() == 0) {
            throw new IllegalArgumentException("At least one URI must be specified for retrieval.");
        }
        
        Connection conn;
        try {
            conn = this.dataSource.getConnection();
            conn.setAutoCommit(false);

            int n = uris.size();
            StringBuffer query = 
                new StringBuffer("select resource_ancestor_ids(r.uri) AS ancestor_ids, r.*, p.* from vortex_resource r "
                + "left outer join extra_prop_entry p on r.resource_id = p.resource_id "
                + "where r.uri in (");
            for (int i=0; i<n; i++) {
                query.append("?");
                if (i < n-1) query.append(",");
            }
            query.append(")");

            PreparedStatement stmt = conn.prepareStatement(query.toString());
            
            n = 1;
            for (Iterator i = uris.iterator(); i.hasNext();) {
                String uri = (String)i.next();
                stmt.setString(n++, uri);
            }

            ResultSet rs = stmt.executeQuery();
            
            return new ResultSetIteratorImpl(this.propertyManager, this.principalManager, rs);
        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        }
    }
 
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void setPropertyManager(PropertyManagerImpl propertyManager) {
        this.propertyManager = propertyManager;
    }
    
    public void setPrincipalManager(PrincipalManager principalManager) {
        this.principalManager = principalManager;
    }

}
