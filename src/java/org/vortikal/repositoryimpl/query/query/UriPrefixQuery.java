package org.vortikal.repositoryimpl.query.query;

public class UriPrefixQuery implements UriQuery {

    private final String uri;

    public UriPrefixQuery(String uri) {
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }

}
