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

    public String dump(String prefix) {
        StringBuffer buf = new StringBuffer().append(prefix);
        buf.append(this.getClass().getName()).append("\n");

        PropertyTypeDefinition def = getPropertyDefinition();
        
        buf.append(prefix).append("Property namespace = '").append(def.getNamespace());
        buf.append("', name = '").append(def.getName()).append("', term = '").append(term).append("'\n");
        
        return buf.toString();
    }
    
}
