package org.vortikal.repo2;

public final class PropertyID {

    private String identifier;
    
    public String getIdentifier() {
        return this.identifier;
    }
    
    public static PropertyID valueOf(String identifier) {
        return new PropertyID(identifier);
    }
    
    private PropertyID(String identifier) {
        this.identifier = identifier;
    }
    
    public String toString() {
        return this.identifier;
    }

    public boolean equals(Object obj) {
        if (obj instanceof PropertyID) {
            PropertyID other = (PropertyID) obj;
            return other.identifier.equals(this.identifier);
        }
        return false;
    }
    
    public int hashCode() {
        return this.identifier.hashCode();
    }

}
