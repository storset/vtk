package org.vortikal.repo2;

import org.vortikal.repository.ContentStream;

public interface ContentStore {

    public void create(NodeID nodeID) throws Exception;
    
    public ContentStream retrieve(NodeID nodeID) throws Exception;
    
    public void update(NodeID nodeID, ContentStream is) throws Exception;
    
    public void delete(NodeID nodeID) throws Exception;
}
