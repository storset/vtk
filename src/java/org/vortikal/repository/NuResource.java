package org.vortikal.repository;

public interface NuResource extends PropertySet {

    public Property createProperty(String namespace, String name);
    
    public void deleteProperty(Property property);
}
