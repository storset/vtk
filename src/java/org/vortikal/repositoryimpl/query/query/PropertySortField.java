package org.vortikal.repositoryimpl.query.query;

import org.vortikal.repository.resourcetype.PropertyTypeDefinition;

public class PropertySortField extends AbstractSortField {

    private PropertyTypeDefinition definition;
    
    public PropertySortField(PropertyTypeDefinition def) {
        this.definition = def;
    }

    public PropertySortField(PropertyTypeDefinition def, SortFieldDirection direction) {
        super(direction);
        this.definition = def;
    }
    
    public PropertyTypeDefinition getDefinition() {
        return this.definition;
    }

}
