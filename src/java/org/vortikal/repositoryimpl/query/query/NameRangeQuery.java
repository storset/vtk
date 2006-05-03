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

}
