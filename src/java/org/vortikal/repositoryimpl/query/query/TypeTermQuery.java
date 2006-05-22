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

    public String dump(String prefix) {
        StringBuffer buf = new StringBuffer().append(prefix);
        buf.append(this.getClass().getName()).append("\n");
        
        buf.append(prefix).append("Operator = ").append(operator);
        buf.append(", term = ").append(term).append("\n");
        return buf.toString();
    }

}
