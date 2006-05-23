package org.vortikal.repositoryimpl.query.query;

import org.vortikal.repository.resourcetype.PropertyTypeDefinition;

/**
 * XXX: Missing not exists... Should include NotQuery and syntax instead? 
 */
public class PropertyExistsQuery extends AbstractPropertyQuery {

    private boolean invert;
    
    public PropertyExistsQuery(PropertyTypeDefinition propertyDefinition, boolean invert) {
        super(propertyDefinition);
        this.invert = invert;
    }

    public String dump(String prefix) {
        StringBuffer buf = new StringBuffer().append(prefix);
        buf.append(this.getClass().getName()).append("\n");

        PropertyTypeDefinition def = getPropertyDefinition();
        
        buf.append(prefix).append("Property namespace = ").append(def.getNamespace());
        buf.append(", name = ").append(def.getName()).append("\n");
        buf.append("Inverted: " + invert);
        return buf.toString();
    }

}
