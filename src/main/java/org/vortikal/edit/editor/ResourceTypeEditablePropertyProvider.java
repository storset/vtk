package org.vortikal.edit.editor;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.PrimaryResourceTypeDefinition;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;

public class ResourceTypeEditablePropertyProvider implements
		EditablePropertyProvider {
	
	private ResourceTypeDefinition targetResourceType;
	private ResourceTypeTree resourceTypeTree;
	
	public List<PropertyTypeDefinition> getEditableProperties(Resource resource) {
		List<PropertyTypeDefinition> result = new ArrayList<PropertyTypeDefinition>();
		if (this.resourceTypeTree.isContainedType(this.targetResourceType, resource.getResourceType())) {
			addPropDefs(resource, result, resource.getResourceTypeDefinition());
		}
		
		return result;
	}

	private void addPropDefs(Resource resource, List<PropertyTypeDefinition> propList, 
			ResourceTypeDefinition currentType) {

		if (!this.resourceTypeTree.isContainedType(currentType, resource.getResourceTypeDefinition().getName())) {
			return;
		}
		
		for (PropertyTypeDefinition propDef: currentType.getPropertyTypeDefinitions()) {
			propList.add(propDef);
		}
		if (currentType instanceof PrimaryResourceTypeDefinition) {
			PrimaryResourceTypeDefinition primaryType = (PrimaryResourceTypeDefinition) currentType;
			List<PrimaryResourceTypeDefinition> children = 
				this.resourceTypeTree.getResourceTypeDefinitionChildren(primaryType);
			
			for (PrimaryResourceTypeDefinition child: children) {
				addPropDefs(resource, propList, child);
			}
		}
	}
	
	@Required public void setTargetResourceType(ResourceTypeDefinition targetResourceType) {
		this.targetResourceType = targetResourceType;
	}

	public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
		this.resourceTypeTree = resourceTypeTree;
	}

}
