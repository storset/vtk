/* Copyright (c) 2010, University of Oslo, Norway
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
package org.vortikal.web.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.search.PropertySortField;
import org.vortikal.repository.search.SortField;
import org.vortikal.repository.search.SortFieldDirection;
import org.vortikal.repository.search.TypedSortField;
import org.vortikal.web.service.URL;

/**
 * XXX This class needs fixing, it treats "name" as a common property, which is wrong.
 *     It happens to work because resource type tree cannot find any registered prop def with name "name",
 *     and so returns a generated fallback PropertyTypeDefinition instance. And the resulting property field name happens to
 *     match the index URI name field name. Lucky.
 */
public class SearchSorting implements InitializingBean {

    private SortFieldDirection defaultSortOrder;
    private Map<String, SortFieldDirection> sortOrderMapping;
    private PropertyTypeDefinition sortPropDef;
    private List<String> sortOrderPropDefPointers;
    private List<PropertyTypeDefinition> sortOrderPropDefs;

    private ResourceTypeTree resourceTypeTree;

    @Override
    public void afterPropertiesSet() {
        this.sortOrderPropDefs = new ArrayList<PropertyTypeDefinition>();
        if (this.sortOrderPropDefPointers != null) {
            for (String pointer : this.sortOrderPropDefPointers) {
                PropertyTypeDefinition prop = this.resourceTypeTree.getPropertyDefinitionByPointer(pointer);
                if (prop != null) {
                    this.sortOrderPropDefs.add(prop);
                }
            }
        }
    }

    public List<SortField> getSortFields(Resource collection) {
        PropertyTypeDefinition sortProp = null;
        SortFieldDirection sortFieldDirection = this.defaultSortOrder;
        if (this.sortPropDef != null && collection.getProperty(this.sortPropDef) != null) {
            String sortString = collection.getProperty(this.sortPropDef).getStringValue();
            sortProp = resourceTypeTree.getPropertyTypeDefinition(Namespace.DEFAULT_NAMESPACE, sortString);
            if (sortProp == null) {
                sortProp = resourceTypeTree.getPropertyTypeDefinition(Namespace.STRUCTURED_RESOURCE_NAMESPACE,
                        sortString);
            }
            if (this.sortOrderMapping != null && this.sortOrderMapping.containsKey(sortString)) {
                sortFieldDirection = this.sortOrderMapping.get(sortString);
            }
        }

        List<SortField> sortFields = new ArrayList<SortField>();
        if (sortProp != null) {
            // XXX: "name" is not a property, and should use TypedSortField, not PropertySortField.
            // Hack fix here, needs proper fix later:
            if ("name".equals(sortProp.getName()) && Namespace.DEFAULT_NAMESPACE == sortProp.getNamespace()) {
                sortFields.add(new TypedSortField(PropertySet.NAME_IDENTIFIER, sortFieldDirection));
            } else {
                sortFields.add(new PropertySortField(sortProp, sortFieldDirection));                
            }
        } else {
            if (sortOrderPropDefs != null) {
                for (PropertyTypeDefinition p : sortOrderPropDefs) {
                    SortFieldDirection sortOrder = defaultSortOrder;
                    if (sortOrderMapping != null) {
                        SortFieldDirection mappedDirection = sortOrderMapping.get(p.getName());
                        if (mappedDirection != null) {
                            sortOrder = mappedDirection;
                        }
                    }
                    
                    // XXX: "name" is treated as a property, fix me properly.
                    if ("name".equals(p.getName()) && Namespace.DEFAULT_NAMESPACE == p.getNamespace()) {
                        sortFields.add(new TypedSortField(PropertySet.NAME_IDENTIFIER, sortOrder));
                    } else {
                        sortFields.add(new PropertySortField(p, sortOrder));
                    }
                }
            }
        }
        return sortFields;
    }

    public List<SortField> getSortFieldsFromRequestParams(String[] sortingParams) {
        List<SortField> sortFields = new ArrayList<SortField>();
        for (String sortFieldParam : sortingParams) {
            sortFieldParam = URL.decode(sortFieldParam);
            String[] paramValues = sortFieldParam.split(Listing.SORTING_PARAM_DELIMITER);
            if (paramValues.length > 3) {
                // invalid, just ignore it
                continue;
            }
            PropertyTypeDefinition propDef = null;
            String sortDirectionPointer = null;
            if (paramValues.length == 3) {
                propDef = this.resourceTypeTree.getPropertyDefinitionByPrefix(paramValues[0], paramValues[1]);
                sortDirectionPointer = paramValues[2];
            } else if (paramValues.length == 2) {
                propDef = this.resourceTypeTree.getPropertyDefinitionByPrefix(null, paramValues[0]);
                sortDirectionPointer = paramValues[1];
            } else {
                propDef = this.resourceTypeTree.getPropertyDefinitionByPrefix(null, paramValues[0]);
            }
            if (propDef != null) {
                SortFieldDirection sortDirection = null;
                if (sortDirectionPointer != null) {
                    sortDirection = resolveSortOrderDirection(sortDirectionPointer);
                } else {
                    sortDirection = this.defaultSortOrder;
                }
                // XXX: "name" treated as property, hack fix here:
                if ("name".equals(propDef.getName()) && Namespace.DEFAULT_NAMESPACE == propDef.getNamespace()) {
                    sortFields.add(new TypedSortField(PropertySet.NAME_IDENTIFIER, sortDirection));
                } else {
                    sortFields.add(new PropertySortField(propDef, sortDirection));
                }
            }
        }
        return sortFields;
    }

    private SortFieldDirection resolveSortOrderDirection(String sortDirectionPointer) {
        try {
            return SortFieldDirection.valueOf(sortDirectionPointer.toUpperCase());
        } catch (Exception e) {
            return this.defaultSortOrder;
        }
    }

    @Required
    public void setDefaultSortOrder(SortFieldDirection defaultSortOrder) {
        this.defaultSortOrder = defaultSortOrder;
    }

    public void setSortOrderMapping(Map<String, SortFieldDirection> sortOrderMapping) {
        this.sortOrderMapping = sortOrderMapping;
    }

    public void setSortPropDef(PropertyTypeDefinition sortPropDef) {
        this.sortPropDef = sortPropDef;
    }

    public void setSortOrderPropDefPointers(List<String> sortOrderPropDefPointers) {
        this.sortOrderPropDefPointers = sortOrderPropDefPointers;
    }

    @Required
    public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
    }

}
