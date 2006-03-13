package org.vortikal.repository.resourcetype.property;

import org.vortikal.repository.PropertySet;
import org.vortikal.repository.RepositoryOperations;
import org.vortikal.repository.resourcetype.Content;
import org.vortikal.repository.resourcetype.PropertyEvaluator;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.security.Principal;
import org.vortikal.util.repository.MimeHelper;

public class CreationTimeEvaluator implements PropertyEvaluator {

    public Value extractFromContent(String operation, Principal principal,
            Content content, Value currentValue) throws Exception {
        return currentValue;
    }

    public Value extractFromProperties(String operation, Principal principal,
            PropertySet newProperties, Value currentValue) throws Exception {

        Value value = currentValue;

        if (operation.equals(RepositoryOperations.CREATE)) {
            value = new Value();
            value.setValue(MimeHelper.map(newProperties.getName()));
        }
        
        if (operation.equals(RepositoryOperations.CREATE_COLLECTION) ) {
            value = new Value();
            value.setValue("application/x-vortex-collection");
        }
        
        return value;
    }

}
