package org.vortikal.repository.search.preprocessor;

import java.util.ArrayList;
import java.util.List;

import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.Value;

public class SubfoldersExpressionEvaluator extends MultiValuePropertyInExpressionEvaluator {
	
    private final String variableName = "subfolders";
    private final Namespace namespace_al = Namespace.getNamespace("http://www.uio.no/resource-types/article-listing");

	@Override
	protected Property getMultiValueProperty(Resource resource) {
		Property recursiveListing = resource.getProperty(namespace_al, PropertyType.RECURSIVE_LISTING_PROP_NAME);
		if (recursiveListing != null && recursiveListing.getBooleanValue() == false) {
			return null;
		}
		Property subfolders = resource.getProperty(namespace_al, PropertyType.SUBFOLDERS_PROP_NAME);
		
		String parent = resource.getURI().toString();
		List<Value> values = new ArrayList<Value>();
		for (Value value : subfolders.getValues()) {
			String subfolder = value.getStringValue();
			subfolder = subfolder.startsWith("/") ? subfolder : "/" + subfolder;
			if (!subfolder.startsWith(parent)) {
				subfolder = parent + subfolder;
			}
			subfolder = subfolder.endsWith("/") ? subfolder : subfolder + "/";
			values.add(new Value(subfolder));
		}
		subfolders.setValues(values.toArray(new Value[values.size()]));
		
		return subfolders;
	}

	@Override
	protected String getVariableName() {
		return this.variableName;
	}
	

}