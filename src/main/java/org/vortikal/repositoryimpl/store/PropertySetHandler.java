package org.vortikal.repositoryimpl.store;

import org.vortikal.repository.PropertySet;

/**
 * Interface for callback-based <code>PropertySet</code> result fetching.
 * 
 * @author oyviste
 *
 */
public interface PropertySetHandler {

    /**
     * Handles a <code>PropertySet</code> result.
     * 
     * @param propertySet
     */
    void handlePropertySet(PropertySet propertySet);

}
