package org.vortikal.repositoryimpl.query.query;

public class UriOperator {

    public static final UriOperator EQ = new UriOperator("EQ");
    public static final UriOperator NE = new UriOperator("NE");
    
    private String id;

    private UriOperator(String id) {
        this.id = id;
    }

    public String toString() {
        return id;
    }

}
