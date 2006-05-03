package org.vortikal.repositoryimpl.query.query;

import org.vortikal.repository.resourcetype.PropertyTypeDefinition;

public abstract class AbstractPropertyQuery implements PropertyQuery {

    private PropertyTypeDefinition propertyDefinition;

    public AbstractPropertyQuery(PropertyTypeDefinition propertyDefinition) {
        this.propertyDefinition = propertyDefinition;
    }
    
    public PropertyTypeDefinition getPropertyDefinition() {
        return this.propertyDefinition;
    }

}
