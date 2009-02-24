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
    
    public void create(PropertyID propertyID) throws Exception {
        Map<String, String> params = new HashMap<String, String>();
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
