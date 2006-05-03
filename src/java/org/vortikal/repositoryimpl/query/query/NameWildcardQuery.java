package org.vortikal.repositoryimpl.query.query;


public class NameWildcardQuery implements NameQuery {

    private String term;
    
    public NameWildcardQuery(String term) {
        this.term = term;
    }

    public String getTerm() {
        return term;
    }

}
