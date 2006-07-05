package org.vortikal.repository.resourcetype.property;

import java.util.HashSet;
import java.util.Set;

import org.vortikal.repository.resourcetype.Constraint;
import org.vortikal.repository.resourcetype.ConstraintViolationException;
import org.vortikal.repository.resourcetype.Value;


public class StringEnumerationConstraint implements Constraint {

    private Set allowedValues = new HashSet();
    
    public void validate(Value value) throws ConstraintViolationException {
        if (!this.allowedValues.contains(value.getValue()))
            throw new ConstraintViolationException("Value not in allowed set");
    }

    public void setAllowedValues(String[] allowedValues) {
        if (allowedValues == null)
            return;
        
        for (int i = 0; i < allowedValues.length; i++) {
            this.allowedValues.add(allowedValues[i]);
        }
    }

}
