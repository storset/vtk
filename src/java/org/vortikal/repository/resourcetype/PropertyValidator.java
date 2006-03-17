package org.vortikal.repository.resourcetype;

import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.security.Principal;

public interface PropertyValidator {

    public void validate(Principal principal, PropertySet ancestorPropertySet, Property property) 
    throws ConstraintViolationException;

}
