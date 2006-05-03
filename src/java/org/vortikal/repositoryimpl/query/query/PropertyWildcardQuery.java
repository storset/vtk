package org.vortikal.repositoryimpl.query.query;

import org.vortikal.repository.resourcetype.PropertyTypeDefinition;

public class PropertyWildcardQuery extends AbstractPropertyQuery {

    private String term;
    
    public PropertyWildcardQuery(PropertyTypeDefinition propertyDefinition, String term) {
        super(propertyDefinition);
        this.term = term;
    }

    public String getTerm() {
        return term;
    }

}
