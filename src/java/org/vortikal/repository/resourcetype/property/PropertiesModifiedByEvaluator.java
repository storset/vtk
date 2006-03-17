package org.vortikal.repository.resourcetype.property;

import java.util.Date;

import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.resourcetype.CreatePropertyEvaluator;
import org.vortikal.repository.resourcetype.PropertiesModificationPropertyEvaluator;
import org.vortikal.security.Principal;

public class PropertiesModifiedByEvaluator implements CreatePropertyEvaluator, 
    PropertiesModificationPropertyEvaluator {

    public boolean create(Principal principal, Property property, PropertySet ancestorPropertySet, boolean isCollection, Date time) throws PropertyEvaluationException {
        property.setStringValue(principal.getQualifiedName());
        return true;
    }

    public boolean propertiesModification(Principal principal, Property property, PropertySet ancestorPropertySet, Date time) throws PropertyEvaluationException {
        property.setDateValue(time);
        return true;
    }

}
