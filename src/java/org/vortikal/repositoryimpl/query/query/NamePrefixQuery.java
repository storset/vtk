package org.vortikal.repositoryimpl.query.query;



public class NamePrefixQuery implements NameQuery {

    private final String term;

    public NamePrefixQuery(String term) {
        this.term = term;
    }

    public String getTerm() {
        return term;
    }

    public String dump(String prefix) {
        StringBuffer buf = new StringBuffer().append(prefix);
        buf.append(this.getClass().getName()).append("\n");
        
        buf.append(prefix).append("Term = ").append(term).append("\n");

        return buf.toString();
    }
    
}
