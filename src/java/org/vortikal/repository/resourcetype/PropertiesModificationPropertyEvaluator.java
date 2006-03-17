package org.vortikal.repository.resourcetype;

import java.util.Date;
import java.util.List;

import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.resourcetype.property.PropertyEvaluationException;
import org.vortikal.security.Principal;

public interface PropertiesModificationPropertyEvaluator {

    public Property propertiesModification(Principal principal, 
            Property property, PropertySet ancestorPropertySet, Date time) 
        throws PropertyEvaluationException;

}
