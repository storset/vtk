package org.vortikal.repository.resourcetype.property;

import org.vortikal.repository.PropertySet;
import org.vortikal.repository.RepositoryOperations;
import org.vortikal.repository.resourcetype.Content;
import org.vortikal.repository.resourcetype.PropertyEvaluator;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.security.Principal;

public class CollectionEvaluator implements PropertyEvaluator {

    public Value extractFromContent(String operation, Principal principal,
            Content content, Value currentValue) throws Exception {
        return currentValue;
    }

    public Value evaluateProperties(String operation, Principal principal, PropertySet newProperties, Value currentValue, Value oldValue) throws Exception {
        Value value = new Value();
        
        if (operation.equals(RepositoryOperations.CREATE)) {
            value.setBooleanValue(false);
        } else if (operation.equals(RepositoryOperations.CREATE_COLLECTION)) {
            value.setBooleanValue(true);
        } else {
            value = currentValue;
        }

        return value;
    }


}
