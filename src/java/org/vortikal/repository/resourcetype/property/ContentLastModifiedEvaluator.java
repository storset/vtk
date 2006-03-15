package org.vortikal.repository.resourcetype.property;

import org.vortikal.repository.PropertySet;
import org.vortikal.repository.resourcetype.Content;
import org.vortikal.repository.resourcetype.PropertyEvaluator;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.security.Principal;

public class ContentLastModifiedEvaluator implements PropertyEvaluator {

    public Value extractFromContent(String operation, Principal principal,
            Content content, Value currentValue) throws Exception {
        Value value = new Value();
        value.setValue(principal.getQualifiedName());
        return value;
    }

    public Value evaluateProperties(String operation, Principal principal, PropertySet newProperties, Value currentValue, Value oldValue) throws Exception {
        return currentValue;
    }

}
