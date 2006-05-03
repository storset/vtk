package org.vortikal.repositoryimpl.query.query;



public class NameTermQuery implements NameQuery {

    private String term;
    private TermOperator operator;
    
    public NameTermQuery(String term, TermOperator operator) {
        this.term = term;
        this.operator = operator;
        
    }

    public TermOperator getOperator() {
        return operator;
    }

    public void setOperator(TermOperator operator) {
        this.operator = operator;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }
}
