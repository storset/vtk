/* Copyright (c) 2007, University of Oslo, Norway
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of the University of Oslo nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *      
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.vortikal.edit.editor;

import java.util.ArrayList;
import java.util.List;

import org.vortikal.repository.Resource;
import org.vortikal.repository.TypeInfo;
import org.vortikal.repository.resourcetype.MixinResourceTypeDefinition;
import org.vortikal.repository.resourcetype.PrimaryResourceTypeDefinition;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition.ContentRelation;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;

public class ResourceTypeEditablePropertyProvider implements EditablePropertyProvider {
	
	public List<PropertyTypeDefinition> getPreContentProperties(Resource resource, TypeInfo typeInfo) {
        ContentRelation type = PropertyTypeDefinition.ContentRelation.PRE_CONTENT;
	    return getPropertyDefinitionsOfType(typeInfo, type);
	}

    public List<PropertyTypeDefinition> getPostContentProperties(Resource resource, TypeInfo typeInfo) {
        ContentRelation type = PropertyTypeDefinition.ContentRelation.POST_CONTENT;
        return getPropertyDefinitionsOfType(typeInfo, type);
    }

	
	private List<PropertyTypeDefinition> getPropertyDefinitionsOfType(TypeInfo typeInfo, ContentRelation type) {
        List<ResourceTypeDefinition> resourceDefinitions = new ArrayList<ResourceTypeDefinition>();
	    populateDefinitions(resourceDefinitions, typeInfo.getResourceType());
	    List<PropertyTypeDefinition> defs = new ArrayList<PropertyTypeDefinition>();
	    for (ResourceTypeDefinition resourceDef : resourceDefinitions) {
	        for (PropertyTypeDefinition propDef: resourceDef.getPropertyTypeDefinitions()) {
                if (propDef.getContentRelation() == type) {
	                defs.add(propDef);
	            }
	        }
	    }
	    return defs;
    }

	private void populateDefinitions(List<ResourceTypeDefinition> definitions, PrimaryResourceTypeDefinition resourceTypeDefinition) {
	    if (resourceTypeDefinition != null) {
	        populateDefinitions(definitions, resourceTypeDefinition.getParentTypeDefinition());
	        definitions.add(resourceTypeDefinition);
	        List<MixinResourceTypeDefinition> mixins = resourceTypeDefinition.getMixinTypeDefinitions();
	        if (mixins != null) {
	            for (MixinResourceTypeDefinition mixin : mixins) {
                    definitions.add(mixin);
                }
	        }
	    }
    }

}
