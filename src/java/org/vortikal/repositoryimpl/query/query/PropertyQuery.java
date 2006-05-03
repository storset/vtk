package org.vortikal.repositoryimpl.query.query;

import org.vortikal.repository.resourcetype.PropertyTypeDefinition;

public interface PropertyQuery extends Query {

    public PropertyTypeDefinition getPropertyDefinition();
    
}
