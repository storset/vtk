package org.vortikal.repository.resourcetype.property;

import org.vortikal.repository.PropertySet;
import org.vortikal.repository.RepositoryOperations;
import org.vortikal.repository.resourcetype.Content;
import org.vortikal.repository.resourcetype.PropertyEvaluator;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.security.Principal;

public class PropertiesModifiedByEvaluator implements PropertyEvaluator {

    public Value extractFromContent(String operation, Principal principal,
            Content content, Value currentValue) throws Exception {
        return currentValue;
    }

    public Value evaluateProperties(String operation, Principal principal, PropertySet newProperties, Value currentValue, Value oldValue) throws Exception {
        if (operation == RepositoryOperations.CREATE ||
                operation == RepositoryOperations.CREATE_COLLECTION ||
                operation == RepositoryOperations.STORE) {
            Value value = new Value();
            value.setValue(principal.getQualifiedName());
            return value;
        }
        return currentValue;
    }

}
