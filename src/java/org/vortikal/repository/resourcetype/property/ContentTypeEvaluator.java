package org.vortikal.repository.resourcetype.property;

import java.util.Date;

import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.resourcetype.CreatePropertyEvaluator;
import org.vortikal.security.Principal;
import org.vortikal.util.repository.MimeHelper;

public class ContentTypeEvaluator implements CreatePropertyEvaluator {

    public boolean create(Principal principal, Property property, 
            PropertySet ancestorPropertySet, boolean isCollection, Date time) 
    throws PropertyEvaluationException {
        if (!isCollection) {
            property.setStringValue(MimeHelper.map(ancestorPropertySet.getName()));
        } else {
            property.setStringValue("application/x-vortex-collection");
        }
        return true;
    }

}
