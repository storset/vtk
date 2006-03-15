package org.vortikal.repository.resourcetype.property;

import org.vortikal.repository.IllegalOperationException;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.RepositoryOperations;
import org.vortikal.repository.resourcetype.Content;
import org.vortikal.repository.resourcetype.PropertyEvaluator;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalManager;

public class OwnerEvaluator implements PropertyEvaluator {

    private PrincipalManager principalManager;
    
    public Value extractFromContent(String operation, Principal principal,
            Content content, Value currentValue) throws Exception {
        return currentValue;
    }


    private void validateOwner(Principal newOwner) {
        if (newOwner == null) {
            throw new IllegalOperationException("Unable to delete owner of " +
                    "resource: All resources must have an owner.");
        }

        if (!principalManager.validatePrincipal(newOwner)) {
            throw new IllegalOperationException(
                    "Unable to set owner of resource to invalid owner: '" + newOwner + "'");
        }
        
    }

    /**
     * @param principalManager The principalManager to set.
     */
    public void setPrincipalManager(PrincipalManager principalManager) {
        this.principalManager = principalManager;
    }


    public Value evaluateProperties(String operation, Principal principal, PropertySet newProperties, Value currentValue, Value oldValue) throws Exception {
        Value value = currentValue;
        Principal newOwner = null;
        
        if (operation.equals(RepositoryOperations.CREATE) ||
            operation.equals(RepositoryOperations.CREATE_COLLECTION) ) {
            newOwner = principal;
        }
        
        if (newOwner != null) {
            validateOwner(newOwner);
            value.setValue(newOwner.getQualifiedName());
        }
        
        return value;
    }

}
