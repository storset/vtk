package org.vortikal.repository.resourcetype.property;

import java.util.Date;

import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.resourcetype.CreatePropertyEvaluator;
import org.vortikal.security.Principal;

public class CollectionEvaluator implements CreatePropertyEvaluator {

    public Property create(Principal principal, Property property, PropertySet ancestorPropertySet, boolean isCollection, Date time) throws PropertyEvaluationException {
        property.setBooleanValue(isCollection);
        return property;
    }

    
}
