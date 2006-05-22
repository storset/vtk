package org.vortikal.repositoryimpl.query.query;

import org.vortikal.repository.resourcetype.PropertyTypeDefinition;

/**
 * XXX: Missing not exists... Should include NotQuery and syntax instead? 
 */
public class PropertyExistsQuery extends AbstractPropertyQuery {

    public PropertyExistsQuery(PropertyTypeDefinition propertyDefinition) {
        super(propertyDefinition);
    }

    public String dump(String prefix) {
        StringBuffer buf = new StringBuffer().append(prefix);
        buf.append(this.getClass().getName()).append("\n");

        PropertyTypeDefinition def = getPropertyDefinition();
        
        buf.append(prefix).append("Property namespace = ").append(def.getNamespace());
        buf.append(", name = ").append(def.getName()).append("\n");
        
        return buf.toString();
    }

}
