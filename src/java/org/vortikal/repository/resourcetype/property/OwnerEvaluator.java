package org.vortikal.repository.resourcetype.property;

import java.util.Date;
import java.util.List;

import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.resourcetype.ConstraintViolationException;
import org.vortikal.repository.resourcetype.CreatePropertyEvaluator;
import org.vortikal.repository.resourcetype.PropertyValidator;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalManager;

public class OwnerEvaluator implements CreatePropertyEvaluator, PropertyValidator {

    private PrincipalManager principalManager;
    
    public boolean create(Principal principal, Property property, PropertySet ancestorPropertySet, boolean isCollection, Date time) throws PropertyEvaluationException {
        property.setStringValue(principal.getQualifiedName());
        return true;
    }

    public void validate(Principal principal, PropertySet ancestorPropertySet, Property property) throws ConstraintViolationException {
        if (property.getStringValue() == null) {
            throw new ConstraintViolationException("All resources must have an owner.");
        }

        Principal owner = principalManager.getPrincipal(property.getStringValue());
        if (!principalManager.validatePrincipal(owner)) {
            throw new ConstraintViolationException(
                    "Unable to set owner of resource to invalid owner: '" 
                    + principal.getQualifiedName() + "'");
        }
    }

    public void setPrincipalManager(PrincipalManager principalManager) {
        this.principalManager = principalManager;
    }

}
