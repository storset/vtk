package org.vortikal.repositoryimpl.query.query;


public class NamePrefixQuery implements NameQuery {

    private final String term;

    public NamePrefixQuery(String term) {
        this.term = term;
    }

    public String getTerm() {
        return term;
    }

}
