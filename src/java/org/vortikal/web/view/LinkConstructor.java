package org.vortikal.web.view;

/**
 * Utility wrapper for constructing URLs based on services/resources
 *
 */
public interface LinkConstructor {

	public String construct(String resourceURI, String parametersCSV, String serviceName);
	
}
