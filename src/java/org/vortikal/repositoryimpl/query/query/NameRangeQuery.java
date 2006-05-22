package org.vortikal.repositoryimpl.query.query;


public class NameRangeQuery implements NameQuery {

    private final String fromTerm;
    private final String toTerm;
    private final boolean inclusive;
    
    public NameRangeQuery(String fromTerm, String toTerm, boolean inclusive) {
        this.fromTerm = fromTerm;
        this.toTerm = toTerm;
        this.inclusive = inclusive;
    }

    public String getFromTerm() {
        return fromTerm;
    }

    public boolean isInclusive() {
        return inclusive;
    }

    public String getToTerm() {
        return toTerm;
    }

    public String dump(String prefix) {
        StringBuffer buf = new StringBuffer().append(prefix);
        buf.append(this.getClass().getName()).append("\n");

        buf.append(prefix).append("fromTerm = '").append(fromTerm);
        buf.append("', toTerm = '").append(toTerm).append("', inclusive = '");
        buf.append(inclusive).append("'\n");
        
        return buf.toString();
    }

}
