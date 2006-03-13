package org.vortikal.repository.resourcetype.property;

import java.util.Date;

import org.vortikal.repository.PropertySet;
import org.vortikal.repository.RepositoryOperations;
import org.vortikal.repository.resourcetype.Content;
import org.vortikal.repository.resourcetype.PropertyEvaluator;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.security.Principal;

public class PropertiesLastModifiedEvaluator implements PropertyEvaluator {

    public Value extractFromContent(String operation, Principal principal,
            Content content, Value currentValue) throws Exception {
        return currentValue;
    }

    public Value evaluateProperties(String operation, Principal principal,
            PropertySet newProperties, Value currentValue, Value oldValue) throws Exception {

        Value value = currentValue;
        
        if (operation.equals(RepositoryOperations.CREATE) ||
                operation.equals(RepositoryOperations.CREATE_COLLECTION) ||
                (operation.equals(RepositoryOperations.STORE) && oldValue.equals(currentValue))) {
            value = new Value();
            value.setDateValue(new Date());
        }
        return value;
    }

}
