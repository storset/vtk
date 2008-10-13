package org.vortikal.repository.store;

import org.vortikal.repository.Property;
import org.vortikal.repository.Resource;

public interface LazyDataAccessor {
	
	/**
	 * Loads the binary content for a given property of a resource
	 * 
	 * @param resource The resource to load the binary property for
	 * @return A binary property containing the binary content as a bytearray and
	 *    its mimetype, null if the resource has no binary content
	 */
	public Property loadBinaryContent(Resource resource);

}
