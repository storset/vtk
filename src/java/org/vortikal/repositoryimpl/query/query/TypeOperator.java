package org.vortikal.repositoryimpl.query.query;

public class TypeOperator {

    public static final TypeOperator EQ = new TypeOperator("EQ");
    public static final TypeOperator NE = new TypeOperator("NE");
    public static final TypeOperator IN = new TypeOperator("IN");
    public static final TypeOperator NI = new TypeOperator("NI");
    
    private String id;

    private TypeOperator(String id) {
        this.id = id;
    }

    public String toString() {
        return id;
    }
    
    
}
