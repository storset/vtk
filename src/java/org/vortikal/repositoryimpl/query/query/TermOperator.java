package org.vortikal.repositoryimpl.query.query;

public class TermOperator {

    public static final TermOperator EQ = new TermOperator("EQ");
    public static final TermOperator NE = new TermOperator("NE");
    public static final TermOperator GT = new TermOperator("GT");
    public static final TermOperator LT = new TermOperator("LT");
    public static final TermOperator GE = new TermOperator("GE");
    public static final TermOperator LE = new TermOperator("LE");
    
    private String id;

    private TermOperator(String id) {
        this.id = id;
    }

    public String toString() {
        return id;
    }

}
