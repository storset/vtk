package org.vortikal.repositoryimpl.query.query;

public class UriTermQuery implements UriQuery {

    private final String uri;

    public UriTermQuery(String uri) {
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }

}
