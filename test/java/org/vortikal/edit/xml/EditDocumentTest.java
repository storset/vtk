/* Copyright (c) 2004, University of Oslo, Norway
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
package org.vortikal.edit.xml;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

public class EditDocumentTest extends TestCase {

    private static final String TEST_XML = "org/vortikal/edit/xml/test.xml";

    private static final String TEST_XSD = "org/vortikal/edit/xml/test.xsd";

    Document schemaDocument = null;

    EditDocument testDocument = null;

    SchemaDocumentDefinition definition = null;

    protected void setUp() throws Exception {
        super.setUp();

        SAXBuilder builder = new SAXBuilder(
                "org.apache.xerces.parsers.SAXParser");
        builder.setValidation(true);

        /* turn on schema support */
        builder.setFeature("http://apache.org/xml/features/validation/schema",
                true);

        try {
            URL testXML = 
                this.getClass().getClassLoader().getResource(TEST_XML);

            Document d = builder.build(testXML);
            Element root = d.getRootElement();
            root.detach();
            // XXX: will tests run without a resource?
            this.testDocument = new EditDocument(root, d.getDocType(), null);

            URL testXSD = 
                this.getClass().getClassLoader().getResource(TEST_XSD);
            this.definition = new SchemaDocumentDefinition("test", testXSD);

        } catch (Exception e) {
            System.out.println("Caught exception:");
            e.printStackTrace();
            fail("Couldn't instantiate test schema" + e.getMessage());
        }

    }

    public void testAddContentsToElement() {
        Map map = new HashMap();
        map.put("1.1:type", "1");
        map.put("1.1", "Lala");

        Element e = new Element("attributeTest1");
        e.setAttribute("type", "");

        this.testDocument.getRootElement().addContent(0, e);
        this.testDocument.addContentsToElement(e, map, this.definition);

        assertEquals("1", e.getAttributeValue("type"));
        assertEquals("Lala", e.getText());

        /* Unbounded element with attribute */
        e = new Element("attributeTest3");
        e.setAttribute("type", "2");

        this.testDocument.getRootElement().addContent(0, e);
        this.testDocument.addContentsToElement(e, map, this.definition);

        assertEquals("1", e.getAttributeValue("type"));
        assertEquals("Lala", e.getChild("tekstblokk").getText());

    }
}
