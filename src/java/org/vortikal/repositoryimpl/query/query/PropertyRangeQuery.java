package org.vortikal.repositoryimpl.query.query;

import org.vortikal.repository.resourcetype.PropertyTypeDefinition;

public class PropertyRangeQuery extends AbstractPropertyQuery {

    private final String fromTerm;
    private final String toTerm;
    private final boolean inclusive;
    
    public PropertyRangeQuery(PropertyTypeDefinition propertyDefinition, 
            String fromTerm, String toTerm, boolean inclusive) {
        super(propertyDefinition);
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
