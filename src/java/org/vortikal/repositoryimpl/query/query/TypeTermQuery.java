package org.vortikal.repositoryimpl.query.query;


public class TypeTermQuery implements Query {

    private String term;
    private TypeOperator operator;
    
    public TypeTermQuery(String term, TypeOperator operator) {
        this.term = term;
        this.operator = operator;
        
    }

    public TypeOperator getOperator() {
        return operator;
    }

    public void setOperator(TypeOperator operator) {
        this.operator = operator;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }
}
