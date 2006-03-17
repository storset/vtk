package org.vortikal.repository.resourcetype.property;

import java.util.Date;

import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.resourcetype.Content;
import org.vortikal.repository.resourcetype.ContentModificationPropertyEvaluator;
import org.vortikal.repository.resourcetype.CreatePropertyEvaluator;
import org.vortikal.security.Principal;

public class ContentLastModifiedEvaluator implements CreatePropertyEvaluator, 
    ContentModificationPropertyEvaluator {

    public Property create(Principal principal, Property property, PropertySet ancestorPropertySet, boolean isCollection, Date time) throws PropertyEvaluationException {
        property.setDateValue(time);
        return property;
    }

    public Property contentModification(Principal principal, Property property, PropertySet ancestorPropertySet, Content content, Date time) throws PropertyEvaluationException {
        property.setDateValue(time);
        return property;
    }

}
