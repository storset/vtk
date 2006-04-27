package org.vortikal.repositoryimpl.queryparser;

import java.util.List;

public interface QueryNode {

    public List getChildren();

    public String getNodeName();

    public String getValue();
    
    public void dump(String prefix);
}
