package org.vortikal.repositoryimpl.query.query;

public class UriTermQuery implements UriQuery {

    private final String uri;
    private final UriOperator operator;

    public UriTermQuery(String uri, UriOperator operator) {
        this.uri = uri;
        this.operator = operator;
    }

    public String getUri() {
        return uri;
    }

    public UriOperator getOperator() {
        return operator;
    }

    public String dump(String prefix) {
        StringBuffer buf = new StringBuffer().append(prefix);
        buf.append(this.getClass().getName()).append("\n");
        
        buf.append(prefix).append("Operator = ").append(operator);
        buf.append(prefix).append("Uri = ").append(uri).append("\n");
        return buf.toString();
    }

}
