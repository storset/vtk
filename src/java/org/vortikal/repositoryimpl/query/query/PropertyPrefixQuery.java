package org.vortikal.repositoryimpl.query.query;

import org.vortikal.repository.resourcetype.PropertyTypeDefinition;

public class PropertyPrefixQuery extends AbstractPropertyQuery {

    private final String term;

    public PropertyPrefixQuery(PropertyTypeDefinition propertyDefinition, String term) {
        super(propertyDefinition);
        this.term = term;
    }

    public String getTerm() {
        return term;
    }

}
