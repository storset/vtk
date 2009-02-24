package org.vortikal.repo2;

import java.util.List;

public interface NodeStore {
    
    public void create(Node node) throws Exception;

    public Node retrieve(NodeID nodeID) throws Exception;

    public List<Node> retrieve(List<NodeID> ids) throws Exception;    

    public void update(Node node) throws Exception;

    public void delete(Node node) throws Exception;
}
