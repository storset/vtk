package org.vortikal.repository.resourcetype;

import java.util.Date;

import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.resourcetype.property.PropertyEvaluationException;
import org.vortikal.security.Principal;

public interface ContentModificationPropertyEvaluator {

    public Property contentModification(Principal principal, Property property,
            PropertySet ancestorPropertySet, Content content, Date time) 
        throws PropertyEvaluationException;

}
