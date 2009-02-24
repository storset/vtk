package org.vortikal.repo2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.util.Assert;
import org.vortikal.repository.store.db.AbstractSqlMapDataAccessor;

public class SqlMapNodeStore extends AbstractSqlMapDataAccessor implements NodeStore {
    
    private boolean createSchemas = true;
    private Cache cache;

    public void createTables() {
        if (!this.createSchemas) {
            return;
        }
        boolean exists = false;
//      try {
//          String sqlMap = getSqlMap("nodeStoreExistsQuery");
//          getSqlMapClientTemplate().queryForObject(sqlMap);
//          exists = true;
//      } catch (Throwable t) {
//      }
      if (!exists) {
          String sqlMap = getSqlMap("createNodeStore");
          getSqlMapClientTemplate().update(sqlMap);
      }
    }
    
    public void create(Node node) throws Exception {
        Assert.notNull(node.getNodeID());
        JSONObject json = new JSONObject();
        for (String name: node.getChildNames()) {
            json.put(name, node.getChildID(name).getIdentifier());
        }
        
        Map<String, String> params = new HashMap<String, String>();
        params.put("nodeID", node.getNodeID().getIdentifier());
        if (node.getParentID() != null) {
            params.put("parentID", node.getParentID().getIdentifier());
        } else {
            params.put("parentID", null);
        }
        params.put("childPtrs", json.toString());
        
        String data = null;
        if (node.getData() != null) {
            data = node.getData().toString();
        }
        params.put("data", data);
        String sqlMap = getSqlMap("insertNode");
        getSqlMapClientTemplate().insert(sqlMap, params);
        
        cacheNode(node);
    }


    @SuppressWarnings("unchecked")
    public Node retrieve(NodeID nodeID) throws Exception {
        if (nodeID == null) {
            throw new IllegalArgumentException("Node ID cannot be NULL");
        }
        Node cached = getCached(nodeID);
        if (cached != null) {
            return cached;
        }
        
        String sqlMap = getSqlMap("retrieveNode");
        Map<String, String> result = (Map<String, String>)
            getSqlMapClientTemplate().queryForObject(sqlMap, nodeID.getIdentifier());

        if (result == null) {
            throw new IllegalStateException("No such node: " + nodeID);
        }

        NodeID parentNodeID = null;
        String parentID = result.get("parentID");
        if (parentID != null) {
            parentNodeID = NodeID.valueOf(parentID);
        }

        Map<String, NodeID> children = new HashMap<String, NodeID>();
        String childPtrs = result.get("childPtrs");
        if (childPtrs != null) {
            JSONObject json = new JSONObject(childPtrs);
            Iterator keys = json.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                String value = json.getString(key);
                children.put(key, NodeID.valueOf(value));
            }
        }
        JSONObject jsonData = null;
        String storedData = result.get("data");
        if (storedData != null) {
            jsonData = new JSONObject(result.get("data"));
        }
        Node n = new Node(nodeID, parentNodeID, children, jsonData);
        cacheNode(n);
        return n;
    }


    @SuppressWarnings("unchecked")
    public List<Node> retrieve(List<NodeID> ids) throws Exception {
        if (ids == null || ids.size() == 0) {
            throw new IllegalArgumentException("A list of node IDs must be specified");
        }
        
        List<Node> resultList = new ArrayList<Node>();
        List<String> remaining = new ArrayList<String>();
        
        for (NodeID id: ids) {
            Node cached = getCached(id);
            if (cached != null) {
                resultList.add(cached);
            } else {
                remaining.add(id.getIdentifier());
            }
        }

        if (remaining.size() == 0) {
            return resultList;
        }

        String sqlMap = getSqlMap("retrieveNodes");
        List<Map<String, String>> allResults = (List<Map<String, String>>)
        getSqlMapClientTemplate().queryForList(sqlMap, remaining);

        for (Map<String, String> result: allResults) {
            
            NodeID nodeID = NodeID.valueOf(result.get("id"));
            NodeID parentID = NodeID.valueOf(result.get("parentID"));
            Map<String, NodeID> children = new HashMap<String, NodeID>();
            String childPtrs = result.get("childPtrs");

            if (childPtrs != null) {
                
                JSONObject json = new JSONObject(childPtrs);
                Iterator keys = json.keys();
                
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    String value = json.getString(key);
                    children.put(key, NodeID.valueOf(value));
                }
            }
            JSONObject jsonData = null;
            String storedData = result.get("data");
            if (storedData != null) {
                jsonData = new JSONObject(result.get("data"));
            }
            Node n = new Node(nodeID, parentID, children, jsonData);
            resultList.add(n);
            cacheNode(n);
        }
        return resultList;      
    }
    

    public void update(Node node) throws Exception {
        JSONObject json = new JSONObject();
        for (String name: node.getChildNames()) {
            json.put(name, node.getChildID(name).getIdentifier());
        }

        String parentID = null;
        if (node.getParentID() != null) {
            parentID = node.getParentID().getIdentifier();
        }

        Map<String, String> params = new HashMap<String, String>();
        params.put("nodeID", node.getNodeID().getIdentifier());
        params.put("parentID", parentID);
        params.put("childPtrs", json.toString());
        String data = null;
        if (node.getData() != null) {
            data = node.getData().toString();
        }
        params.put("data", data);

        String sqlMap = getSqlMap("updateNode");
        getSqlMapClientTemplate().update(sqlMap, params);

        cacheNode(node);
    }


    public void delete(Node node) throws Exception {
        String sqlMap = getSqlMap("deleteNode");
        getSqlMapClientTemplate().delete(sqlMap, node.getNodeID().getIdentifier());
        removeCached(node.getNodeID());
    }
    
    @Required public void setCache(Cache cache) {
        this.cache = cache;
    }

    public void setCreateSchemas(boolean createSchemas) {
        this.createSchemas = createSchemas;
    }

    private void cacheNode(Node node) {
        Element element = new Element(node.getNodeID().getIdentifier(), node);
        this.cache.put(element);
    }
    
    private Node getCached(NodeID nodeID) {
        Element element = this.cache.get(nodeID.getIdentifier());
        if (element == null) {
            return null;
        }
        return (Node) element.getObjectValue();
    }
    
    private void removeCached(NodeID nodeID) {
            this.cache.remove(nodeID.getIdentifier());
    }
    
}
