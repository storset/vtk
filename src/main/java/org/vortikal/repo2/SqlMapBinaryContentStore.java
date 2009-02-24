package org.vortikal.repo2;

import java.util.HashMap;
import java.util.Map;

import org.vortikal.repository.ContentStream;
import org.vortikal.repository.store.db.AbstractSqlMapDataAccessor;

public class SqlMapBinaryContentStore extends AbstractSqlMapDataAccessor implements ContentStore {

    private boolean createSchemas = true;

    public void createTables() throws Exception {
        if (!this.createSchemas) {
            return;
        }
        boolean exists = false;
//        try {
//            String sqlMap = getSqlMap("binaryContentStoreExistsQuery");
//            getSqlMapClientTemplate().queryForObject(sqlMap);
//            exists = true;
//        } catch (Throwable t) {
//        }
        if (!exists) {
            String sqlMap = getSqlMap("createBinaryContentStore");
            getSqlMapClientTemplate().update(sqlMap);
        }
    }

    public void create(NodeID nodeID) throws Exception {
        Map<String, String> params = new HashMap<String, String>();
        params.put("nodeID", nodeID.getIdentifier());
        params.put("stream", null);
        String sqlMap = getSqlMap("insertContent");
        getSqlMapClientTemplate().insert(sqlMap, params);
    }

    @SuppressWarnings("unchecked")
    public ContentStream retrieve(NodeID nodeID) throws Exception {
        Map<String, String> params = new HashMap<String, String>();
        params.put("nodeID", nodeID.getIdentifier());
        String sqlMap = getSqlMap("retrieveContent");
        Map<String, Object> result = (Map<String, Object>) 
            getSqlMapClientTemplate().queryForObject(sqlMap, params);
        ContentStream is = (ContentStream) result.get("stream");
        return is;
//        BinaryStream bs = (BinaryStream) result.get("stream");
//        return bs.getStream();
    }

    public void update(NodeID nodeID, ContentStream is) throws Exception {
        //BinaryStream bs = new BinaryStream(is, -1);
        String sqlMap = getSqlMap("updateContent");
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("nodeID", nodeID.getIdentifier());
        params.put("stream", is);
        getSqlMapClientTemplate().update(sqlMap, params);
    }

    public void delete(NodeID nodeID) throws Exception {
        Map<String, String> params = new HashMap<String, String>();
        params.put("nodeID", nodeID.getIdentifier());
        String sqlMap = getSqlMap("deleteContent");
        getSqlMapClientTemplate().delete(sqlMap, params);
    }

    public void setCreateSchemas(boolean createSchemas) {
        this.createSchemas = createSchemas;
    }

}
