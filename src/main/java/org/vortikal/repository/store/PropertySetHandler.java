package org.vortikal.repository.store;

import java.util.Set;

import org.vortikal.repository.PropertySet;
import org.vortikal.security.Principal;

/**
 * Interface for callback-based <code>PropertySet</code> result fetching.
 * Used by {@link IndexDao}.
 * 
 * @author oyviste
 *
 */
public interface PropertySetHandler {

    /**
     * Handles a <code>PropertySet</code> result with corresponding set
     * of principals which are allowed to read the resource.
     * 
     * @param propertySet
     */
    void handlePropertySet(PropertySet propertySet, Set<Principal> aclReadPrincipals);

}
