package org.vortikal.repo2;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

public class Node {
	private NodeID nodeID;
	private NodeID parentID;
	private Map<String, NodeID> childMap;
	private Map<NodeID, String> reverseChildMap;
	private JSONObject data;
	
	public Node(NodeID nodeID, NodeID parentID, Map<String, NodeID> children, JSONObject data) {
	    this.nodeID = nodeID;
		this.parentID = parentID;
		this.childMap = new HashMap<String, NodeID>(children);
		this.reverseChildMap = new HashMap<NodeID, String>(children.size());
		for (String name: children.keySet()) {
		    this.reverseChildMap.put(children.get(name), name);
		}
		this.data = data;
	}
	
	public NodeID getNodeID() {
		return this.nodeID;
	}

	public NodeID getParentID() {
		return this.parentID;
	}
	
	public Map<String, NodeID> getChildMap() {
	    return Collections.unmodifiableMap(this.childMap);
	}
	
	public NodeID getChildID(String name) {
		return this.childMap.get(name);
	}
	
	public String getChildName(NodeID childID) {
	    return this.reverseChildMap.get(childID);
	}
	
	public Set<String> getChildNames() {
		return Collections.unmodifiableSet(this.childMap.keySet());
	}
	
	public Set<NodeID> getChildIDs() {
	    return Collections.unmodifiableSet(this.reverseChildMap.keySet());
	}
	
	public synchronized void addChild(String name, NodeID nodeID) {
		if (this.childMap.containsKey(name)) {
			throw new IllegalArgumentException("A child of name '" + name 
					+ "' already exists in this node");
		}
		this.childMap.put(name, nodeID);
		this.reverseChildMap.put(nodeID, name);
	}
	
	public synchronized void removeChild(String name) {
	    if (!this.childMap.containsKey(name)) {
	        throw new IllegalArgumentException("No child of name '" 
	                + name + "' extsis in this node");
	    }
	    NodeID removed = this.childMap.remove(name);
	    this.reverseChildMap.remove(removed);
	}
	
	public JSONObject getData() {
		return this.data;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder(this.getClass().getName());
		sb.append("[id=").append(this.nodeID);
		sb.append(", parent=").append(this.parentID);
		sb.append(", children=").append(this.childMap);
		sb.append(", data=").append(this.data);
		return sb.toString();
	}
}
