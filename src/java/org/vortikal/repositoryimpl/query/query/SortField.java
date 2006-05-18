package org.vortikal.repositoryimpl.query.query;

public class SortField {

    private String name;
    private String prefix;
    private SortFieldDirection direction;
    
    public SortField(String name, 
                     String prefix, 
                     SortFieldDirection direction) {
        
        this.name = name;
        this.prefix = prefix;
        this.direction = direction;
        
    }
    
    public String getName() {
        return this.name;
    }
    
    public String getPrefix() {
        return this.prefix;
    }
    
    public SortFieldDirection getDirection() {
        return this.direction;
    }
    
}
