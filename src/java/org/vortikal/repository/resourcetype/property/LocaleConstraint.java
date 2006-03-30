package org.vortikal.repository.resourcetype.property;

import java.util.Locale;

import org.vortikal.repository.resourcetype.Constraint;
import org.vortikal.repository.resourcetype.ConstraintViolationException;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.util.repository.LocaleHelper;

public class LocaleConstraint implements Constraint {

    public void validate(Value value) throws ConstraintViolationException {
        Locale locale = LocaleHelper.getLocale(value.getValue());
        if (locale == null)
            throw new ConstraintViolationException("Value not a legal locale string");
    }

}
