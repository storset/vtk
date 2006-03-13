package org.vortikal.repository.resourcetype.property;

import org.vortikal.repository.PropertySet;
import org.vortikal.repository.resourcetype.Content;
import org.vortikal.repository.resourcetype.PropertyEvaluator;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.security.Principal;

public class ContentModifiedByEvaluator implements PropertyEvaluator {

    public Value extractFromContent(String operation, Principal principal,
            Content content, Value currentValue) throws Exception {
        Value value = new Value();
        value.setValue(principal.getQualifiedName());
        return value;
    }

    public Value extractFromProperties(String operation, Principal principal,
            PropertySet newProperties, Value currentValue) throws Exception {
        return currentValue;
    }

}
