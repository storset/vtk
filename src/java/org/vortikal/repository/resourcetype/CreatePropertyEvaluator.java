package org.vortikal.repository.resourcetype;

import java.util.Date;

import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.resourcetype.property.PropertyEvaluationException;
import org.vortikal.security.Principal;

public interface CreatePropertyEvaluator {

    public boolean create(Principal principal, Property property, 
            PropertySet ancestorPropertySet, boolean isCollection, Date time)
        throws PropertyEvaluationException;
    
}
