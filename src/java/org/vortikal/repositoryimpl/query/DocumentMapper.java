/* Copyright (c) 2006, University of Oslo, Norway
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
package org.vortikal.repositoryimpl.query;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.vortikal.repository.Property;
import org.vortikal.repositoryimpl.PropertySetImpl;

/**
 * Simple mapping from Lucene 
 * {@link org.apache.lucene.document.Document} to 
 * {@link org.vortikal.repository.PropertySet} 
 * objects and vice-versa.
 * 
 * @author oyviste
 */
public class DocumentMapper {

    public static final String NAME_FIELD_NAME = "name";
    public static final String URI_FIELD_NAME = "uri";
    public static final String RESOURCETYPE_FIELD_NAME = "resourcetype";
    public static final String PARENTIDS_FIELD_NAME = "_PARENTIDS";
    public static final String ID_FIELD_NAME = "_ID";
    public static final String ACL_INHERITED_FROM_FIELD_NAME = "_ACL_INHERITED_FROM";
    
    
    private static final Set RESERVED_FIELD_NAMES;
    
    static {
        RESERVED_FIELD_NAMES = new HashSet();
        RESERVED_FIELD_NAMES.add(NAME_FIELD_NAME);
        RESERVED_FIELD_NAMES.add(URI_FIELD_NAME);
        RESERVED_FIELD_NAMES.add(RESOURCETYPE_FIELD_NAME);
        RESERVED_FIELD_NAMES.add(PARENTIDS_FIELD_NAME);
        RESERVED_FIELD_NAMES.add(ID_FIELD_NAME);
        RESERVED_FIELD_NAMES.add(ACL_INHERITED_FROM_FIELD_NAME);
    }
    
    
    public static Document getDocument(PropertySetImpl propSet,
                                       String[] parentIds,
                                       FieldMapper fieldMapper) {
        
        Document doc = new Document();
        
        // Special fields
        Field uriField = fieldMapper.getKeywordField(URI_FIELD_NAME, propSet.getURI());
        Field nameField = fieldMapper.getKeywordField("name", propSet.getName());
        Field resourceTypeField =
            fieldMapper.getKeywordField(NAME_FIELD_NAME, propSet.getResourceType());
        Field parentIdsField = fieldMapper.getMultiValueKeywordField(PARENTIDS_FIELD_NAME,
                parentIds);
        Field idField = fieldMapper.getKeywordField(ID_FIELD_NAME, propSet.getID());
        Field aclField = fieldMapper.getKeywordField(ACL_INHERITED_FROM_FIELD_NAME, propSet.getAclInheritedFrom());
        
        // Add special fields
        doc.add(uriField);
        doc.add(nameField);
        doc.add(resourceTypeField);
        doc.add(parentIdsField);
        doc.add(idField);
        doc.add(aclField);
        
        // Add all props
        for (Iterator i = propSet.getProperties().iterator(); i.hasNext();) {
            Property property = (Property) i.next();
            
            Field field = fieldMapper.getFieldFromProperty(property);
            doc.add(field);
        }
        
        
        return doc;
    }
    
    public static PropertySetImpl getPropertySet(Document doc, FieldMapper fieldMapper) {
        
        // XXX: exception handling
        PropertySetImpl propSet = new PropertySetImpl(doc.get(URI_FIELD_NAME));
        propSet.setAclInheritedFrom(Integer.parseInt(doc.get(ACL_INHERITED_FROM_FIELD_NAME)));
        propSet.setID(Integer.parseInt(doc.get(ID_FIELD_NAME)));
        propSet.setResourceType(doc.get(RESOURCETYPE_FIELD_NAME));
        
        Enumeration enum = doc.fields();
        while (enum.hasMoreElements()) {
            Field field = (Field)enum.nextElement();
            
            if (RESERVED_FIELD_NAMES.contains(field.name())) continue;
            
            Property property = fieldMapper.getPropertyFromField(field);
            
            propSet.addProperty(property);
            
        }
        
        return propSet;
    }
    
    
}
