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
package org.vortikal.util.repository;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.vortikal.repository.Property;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;

/**
 * Compare and sort resources based on propertydefs in a list. For each propertydef in
 * the list, if it is set by both resources, use it (sequentially in the order provided).
 * If one or both of the resources miss the given property or the propertyvalues are 
 * equal, move to the next propertydef in the list.
 */
public class ResourcePropertyComparator implements Comparator<Resource> {

    // Default list of props to use when sorting
    private List<PropertyTypeDefinition> propDefs;
    // Alternative list of props to check if a resource to sort does not have default prop set
    private Map<PropertyTypeDefinition, List<PropertyTypeDefinition>> alternativePropDefs;
    private boolean invert = false;
    private Locale locale = null;
    

    public ResourcePropertyComparator(List<PropertyTypeDefinition> propDefs) {
        this(propDefs, false);
    }

    public ResourcePropertyComparator(List<PropertyTypeDefinition> propDefs, boolean invert) {
        this(propDefs, invert, null);
    }
    
    public ResourcePropertyComparator(List<PropertyTypeDefinition> propDefs, boolean invert, Locale locale) {
        if (propDefs == null || propDefs.size() < 1) {
            throw new IllegalArgumentException("No property type definition is supplied: " + propDefs);
        }
        this.propDefs = propDefs;
        this.invert = invert;
        this.locale = locale;
    }

    public ResourcePropertyComparator(List<PropertyTypeDefinition> sortPropDefs,
            Map<PropertyTypeDefinition, List<PropertyTypeDefinition>> overridingSortPropDefs,
            boolean invert, Locale locale) {
        this(sortPropDefs, invert, locale);
        this.alternativePropDefs = overridingSortPropDefs;
    }

    public int compare(Resource r1, Resource r2) {
        
        if (this.invert) {
            Resource tmp = r1; r1 = r2; r2 = tmp;
        }
        
        for (PropertyTypeDefinition propDef : this.propDefs) {
            
            Property p1 = getSortProp(r1, propDef);
            Property p2 = getSortProp(r2, propDef);
            
            int result = 0;
            
            if (p1 != null && p2 != null) {
                switch (p1.getType()) {
                case STRING:
                case HTML:
                case IMAGE_REF:
                case DATE:
                case TIMESTAMP:
                    result = compare(p1, p2);
                    break;
                case INT:
                    result = compareAsInt(p1, p2);
                    break;
                case LONG:
                    result = compareAsLong(p1, p2);
                    break;
                case BOOLEAN:
                    result = compareAsBoolean(p1, p2);
                    break;
                case PRINCIPAL:
                    result = compareAsPrincipal(p1, p2);
                    break;
                default:
                    break;
                }
            }
            
            if (result != 0) {
                return result;
            }
            
        }
        
        return 0;
    }

    private Property getSortProp(Resource resource, PropertyTypeDefinition propDef) {
        Property prop = resource.getProperty(propDef);
        return prop == null ? getAlternativeSortProp(resource, propDef) : prop;
    }

    private Property getAlternativeSortProp(Resource resource, PropertyTypeDefinition propDef) {
        if (this.alternativePropDefs != null && this.alternativePropDefs.containsKey(propDef)) {
            List<PropertyTypeDefinition> alternativeProps = this.alternativePropDefs.get(propDef);
            for (PropertyTypeDefinition propTypeDef : alternativeProps) {
                Property prop = resource.getProperty(propTypeDef);
                if (prop != null) {
                    return prop;
                }
            }
        }
        return null;
    }

    private int compare(Property p1, Property p2) {
        if (this.locale != null) {
            Collator collator = Collator.getInstance(this.locale);
            return collator.compare(p1.getValue().getStringValue(), p2.getValue().getStringValue());
        }
        return p1.getValue().compareTo(p2.getValue());
    }
    
    private int compareAsInt(Property p1, Property p2) {
        int i1 = p1.getIntValue();
        int i2 = p2.getIntValue();
        return i1 != i2 ? i2 - i1 : 0;
    }
    
    private int compareAsLong(Property p1, Property p2) {
        // TODO Implement as needed
        return 0;
    }
    
    private int compareAsBoolean(Property p1, Property p2) {
        // TODO Implement as needed
        return 0;
    }
    
    private int compareAsPrincipal(Property p1, Property p2) {
        // TODO Implement as needed
        return 0;
    }
    
}

