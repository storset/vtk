package org.vortikal.repo2;

public final class NodeID {

	private String identifier;
	
	public String getIdentifier() {
		return this.identifier;
	}
	
	public static NodeID valueOf(String identifier) {
		return new NodeID(identifier);
	}
	
	private NodeID(String identifier) {
		this.identifier = identifier;
	}
	
	public String toString() {
		return this.identifier;
	}

    public boolean equals(Object obj) {
        if (obj instanceof NodeID) {
            NodeID other = (NodeID) obj;
            return other.identifier.equals(this.identifier);
        }
        return false;
    }
    
    public int hashCode() {
        return this.identifier.hashCode();
    }
}
