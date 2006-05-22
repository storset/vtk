package org.vortikal.repositoryimpl.query.query;

public class UriPrefixQuery implements UriQuery {

    private final String uri;

    public UriPrefixQuery(String uri) {
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }

    public String dump(String prefix) {
        StringBuffer buf = new StringBuffer().append(prefix);
        buf.append(this.getClass().getName()).append("\n");
        
        buf.append(prefix).append("Uri = ").append(uri).append("\n");

        return buf.toString();
    }

}
