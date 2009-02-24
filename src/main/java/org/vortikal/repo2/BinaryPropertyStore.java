package org.vortikal.repo2;


public interface BinaryPropertyStore {

    public void create(PropertyID propID) throws Exception;
    
    public TypedContentStream retrieve(PropertyID propertyID) throws Exception;
    
    public void update(PropertyID propertyID, TypedContentStream is) throws Exception;
    
    public void delete(PropertyID propertyID) throws Exception;
    
}
