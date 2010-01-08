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
package org.vortikal.edit.editor;

import junit.framework.TestCase;

import org.apache.commons.lang.StringUtils;
import org.vortikal.repository.PropertyImpl;
import org.vortikal.repository.resourcetype.PropertyTypeDefinitionImpl;
import org.vortikal.repository.resourcetype.StringValueFormatter;
import org.vortikal.repository.resourcetype.ValueFormatter;
import org.vortikal.repository.resourcetype.PropertyType.Type;

public class ResourceEditDataBinderTest extends TestCase {

    private ResourceEditDataBinder resourceEditDataBinder = new ResourceEditDataBinder(null, null, null, null, null);


    public void testSetPropValueMultiple() {
        PropertyImpl prop = getProperty(true, Type.STRING, new StringValueFormatter());
        assertNull("Value should be null", prop.getValues());
        resourceEditDataBinder.setPropValue("test, ,test2, ,,, , ", prop);
        assertNotNull("Value is null", prop.getValues());
        assertEquals("Wrong length", prop.getValues().length, 2);
    }


    public void testSetPropValueString() {
        PropertyImpl prop = getProperty(false, Type.STRING, new StringValueFormatter());
        assertNull("Value should be null", prop.getValue());
        String testValue = "test";
        resourceEditDataBinder.setPropValue(testValue, prop);
        assertNotNull("Value is null", prop.getValue());
        assertTrue("Missing content in value", StringUtils.isNotBlank(prop.getValue().getStringValue()));
        assertEquals("Wrong value", testValue, prop.getValue().getStringValue());
    }


    private PropertyImpl getProperty(boolean isMultiple, Type type, ValueFormatter valueFormatter) {
        PropertyImpl prop = new PropertyImpl();
        PropertyTypeDefinitionImpl propDef = new PropertyTypeDefinitionImpl();
        propDef.setMultiple(isMultiple);
        propDef.setType(type);
        propDef.setValueFormatter(valueFormatter);
        prop.setDefinition(propDef);
        return prop;
    }

}
