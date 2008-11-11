package org.vortikal.repository.search.preprocessor;

import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyType;

public class SubfoldersExpressionEvaluator extends MultiValuePropertyInExpressionEvaluator {
	
    private final String variableName = "subfolders";
    private final Namespace namespace_al = Namespace.getNamespace("http://www.uio.no/resource-types/article-listing");

	@Override
	protected Property getMultiValueProperty(Resource resource) {
		Property recursiveListing = resource.getProperty(namespace_al, PropertyType.RECURSIVE_LISTING_PROP_NAME);
		if (recursiveListing != null && recursiveListing.getBooleanValue() == false) {
			return null;
		}
		return resource.getProperty(namespace_al, PropertyType.SUBFOLDERS_PROP_NAME);
	}

	@Override
	protected String getVariableName() {
		return this.variableName;
	}
	

}
