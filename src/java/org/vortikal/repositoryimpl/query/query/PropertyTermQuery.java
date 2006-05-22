package org.vortikal.repositoryimpl.query.query;

import org.vortikal.repository.resourcetype.PropertyTypeDefinition;

public class PropertyTermQuery extends AbstractPropertyQuery {

    private String term;
    private TermOperator operator;
    
    public PropertyTermQuery(PropertyTypeDefinition propertyDefinition, String term, TermOperator operator) {
        super(propertyDefinition);
        this.term = term;
        this.operator = operator;
    }

    public TermOperator getOperator() {
        return operator;
    }

    public void setOperator(TermOperator operator) {
        this.operator = operator;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
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
