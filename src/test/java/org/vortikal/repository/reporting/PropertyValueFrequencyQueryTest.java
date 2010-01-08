/* Copyright (c) 2008, University of Oslo, Norway
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
package org.vortikal.repository.reporting;

import junit.framework.TestCase;

import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinitionImpl;

public class PropertyValueFrequencyQueryTest extends TestCase {

    
    private PropertyTypeDefinitionImpl keywordsPropDef = null;
    private PropertyTypeDefinitionImpl docTypePropDef = null;
    
    public PropertyValueFrequencyQueryTest() {
        
        PropertyTypeDefinitionImpl def = new PropertyTypeDefinitionImpl();
        def.setName("keywords");
        def.setNamespace(new Namespace("content", "http://www.uio.no/content"));
        def.setMultiple(true);
        def.setType(PropertyType.Type.STRING);
        this.keywordsPropDef = def;

        
        def = new PropertyTypeDefinitionImpl();
        def.setName("docType");
        def.setNamespace(Namespace.DEFAULT_NAMESPACE);
        def.setMultiple(false);
        def.setType(PropertyType.Type.STRING);
        this.docTypePropDef = def;
    }
    
    
    public void testHashCode() {
        PropertyValueFrequencyQuery query1 = 
                new PropertyValueFrequencyQuery();
        
        query1.setPropertyTypeDefinition(this.keywordsPropDef);
        
        PropertyValueFrequencyQuery query2 = 
                new PropertyValueFrequencyQuery();
        
        query2.setPropertyTypeDefinition(this.docTypePropDef);
        
        assertTrue(query1.hashCode() != query2.hashCode());
        
        
        query1 = new PropertyValueFrequencyQuery();
        query1.setPropertyTypeDefinition(this.keywordsPropDef);
        
        query2 = new PropertyValueFrequencyQuery();
        query2.setPropertyTypeDefinition(this.keywordsPropDef);
        
        assertEquals(query1.hashCode(), query2.hashCode());

        query2.setLimit(2);
        query1.setLimit(2);
        assertEquals(query1.hashCode(), query2.hashCode());
        assertEquals(query1.hashCode(), query2.hashCode());
        assertEquals(query1.hashCode(), query2.hashCode());
        
        query1.setOrdering(PropertyValueFrequencyQuery.Ordering.DESCENDING_BY_FREQUENCY);
        query2.setOrdering(PropertyValueFrequencyQuery.Ordering.DESCENDING_BY_FREQUENCY);
        assertEquals(query1.hashCode(), query2.hashCode());
    }
    
    public void testEquals() {
        
        PropertyValueFrequencyQuery query1 = 
            new PropertyValueFrequencyQuery();
    
        query1.setPropertyTypeDefinition(this.keywordsPropDef);
        assertTrue(query1.equals(query1));
        assertFalse(query1.equals(null));
        query1.setLimit(99);
        assertTrue(query1.equals(query1));
        query1.setLimit(PropertyValueFrequencyQuery.LIMIT_UNLIMITED);
        
        PropertyValueFrequencyQuery query2 = 
                new PropertyValueFrequencyQuery();
        
        query2.setPropertyTypeDefinition(this.docTypePropDef);
        assertFalse(query1.equals(query2));

        query2.setPropertyTypeDefinition(this.keywordsPropDef);
        assertTrue(query1.equals(query2));

        query1.setLimit(1);
        assertFalse(query1.equals(query2));
        query2.setLimit(1);
        assertTrue(query1.equals(query2));

        query1.setOrdering(PropertyValueFrequencyQuery.Ordering.DESCENDING_BY_PROPERTY_VALUE);
        assertFalse(query1.equals(query2));
        query2.setOrdering(PropertyValueFrequencyQuery.Ordering.DESCENDING_BY_PROPERTY_VALUE);
        assertTrue(query1.equals(query2));


        query1.addScope(new UriPrefixScope(Path.fromString("/foo")));
        assertFalse(query1.equals(query2));
        query2.addScope(new UriPrefixScope(Path.fromString("/foo")));
        assertTrue(query1.equals(query2));

        query1.addScope(new UriPrefixScope(Path.fromString("/foo/bar")));
        assertFalse(query1.equals(query2));
        query2.addScope(new UriPrefixScope(Path.fromString("/foo/bar")));
        assertTrue(query1.equals(query2));
        
    }
    
    public void testClone() {
        PropertyValueFrequencyQuery query = 
            new PropertyValueFrequencyQuery();
    
        query.setPropertyTypeDefinition(this.keywordsPropDef);
        query.setLimit(10);
        query.setOrdering(PropertyValueFrequencyQuery.Ordering.DESCENDING_BY_PROPERTY_VALUE);
        query.addScope(new UriPrefixScope(Path.ROOT));
        
        PropertyValueFrequencyQuery clone = (PropertyValueFrequencyQuery)query.clone();
        assertFalse(query == clone);
        assertTrue(query.equals(clone));
        assertTrue(query.hashCode() == clone.hashCode());
        
        query.clearScoping();
        assertFalse(query.equals(clone));
    }
    
}
