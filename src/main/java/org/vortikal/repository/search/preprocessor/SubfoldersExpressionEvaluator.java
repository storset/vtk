package org.vortikal.repository.search.preprocessor;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Property;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;

public class SubfoldersExpressionEvaluator extends MultiValuePropertyInExpressionEvaluator {
	
    private final String variableName = "subfolders";
    private PropertyTypeDefinition subfolderPropDef;
    private PropertyTypeDefinition recursiveListingPropDef;

	@Override
	protected Property getMultiValueProperty(Resource resource) {
		Property recursiveListing = resource.getProperty(recursiveListingPropDef);
		if (recursiveListing != null && recursiveListing.getBooleanValue() == false) {
			return null;
		}
		Property subfolders = resource.getProperty(subfolderPropDef);
		
		String parent = resource.getURI().toString();
		List<Value> values = new ArrayList<Value>();
		for (Value value : subfolders.getValues()) {
			String subfolder = value.getStringValue();
			subfolder = subfolder.startsWith("/") ? subfolder : "/" + subfolder;
			if (!subfolder.startsWith(parent)) {
				subfolder = parent + subfolder;
			}
			subfolder = subfolder.endsWith("/") ? subfolder : subfolder + "/";
			values.add(new Value(subfolder, PropertyType.Type.STRING));
		}
		subfolders.setValues(values.toArray(new Value[values.size()]));
		
		return subfolders;
	}

	@Override
	protected String getVariableName() {
		return this.variableName;
	}
	
	@Required
    public void setSubfolderPropDef(PropertyTypeDefinition subfolderPropDef) {
        this.subfolderPropDef = subfolderPropDef;
    }
    
    @Required
    public void setRecursiveListingPropDef(PropertyTypeDefinition recursiveListingPropDef) {
        this.recursiveListingPropDef = recursiveListingPropDef;
    }

}