package org.vortikal.repository.resourcetype;

import java.util.Date;

import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.security.Principal;

public interface NameChangePropertyEvaluator {

    boolean nameModification(Principal principal, Property property, PropertySet ancestorPropertySet, Date time);

}
