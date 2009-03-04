/* Copyright (c) 2009, University of Oslo, Norway
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
package org.vortikal.repo2;

import java.util.HashMap;
import java.util.Map;

import org.vortikal.repository.ContentStream;
import org.vortikal.repository.store.db.AbstractSqlMapDataAccessor;

public class SqlMapBinaryPropertyStore extends AbstractSqlMapDataAccessor implements BinaryPropertyStore {

    private boolean createSchemas = true;

    public void createTables() throws Exception {
        if (!this.createSchemas) {
            return;
        }
        boolean exists = false;
//        try {
//            String sqlMap = getSqlMap("binaryPropStoreExistsQuery");
//            getSqlMapClientTemplate().queryForObject(sqlMap);
//            exists = true;
//        } catch (Throwable t) {
//        }
        if (!exists) {
            String sqlMap = getSqlMap("createBinaryPropertyStore");
            getSqlMapClientTemplate().update(sqlMap);
        }
    }
    
    public void create(NodeID nodeID, PropertyID propertyID) throws Exception {
        Map<String, String> params = new HashMap<String, String>();
        params.put("nodeID", nodeID.getIdentifier());
        params.put("propertyID", propertyID.getIdentifier());
        params.put("stream", null);
        params.put("type", null);
        String sqlMap = getSqlMap("insertBinaryProperty");
        getSqlMapClientTemplate().insert(sqlMap, params);
    }

    @SuppressWarnings("unchecked")
    public TypedContentStream retrieve(PropertyID propertyID) throws Exception {
        Map<String, String> params = new HashMap<String, String>();
        params.put("propertyID", propertyID.getIdentifier());
        String sqlMap = getSqlMap("retrieveBinaryProperty");
        Map<String, Object> result = (Map<String, Object>) 
            getSqlMapClientTemplate().queryForObject(sqlMap, params);
        ContentStream is = (ContentStream) result.get("stream");
        String type = (String) result.get("type");
        TypedContentStream tis = new TypedContentStream(is.getStream(), is.getLength(), type);
        return tis;
    }

    public void update(PropertyID propertyID, TypedContentStream is) throws Exception {
        String sqlMap = getSqlMap("updateBinaryProperty");
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("propertyID", propertyID.getIdentifier());
        params.put("stream", is);
        params.put("type", is.getContentType());
        getSqlMapClientTemplate().update(sqlMap, params);
    }

    public void delete(PropertyID propertyID) throws Exception {
        Map<String, String> params = new HashMap<String, String>();
        params.put("propertyID", propertyID.getIdentifier());
        String sqlMap = getSqlMap("deleteBinaryProperty");
        getSqlMapClientTemplate().delete(sqlMap, params);
    }

    public void setCreateSchemas(boolean createSchemas) {
        this.createSchemas = createSchemas;
    }

}
