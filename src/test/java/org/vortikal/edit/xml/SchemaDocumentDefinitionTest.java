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
package org.vortikal.edit.xml;

import java.net.URL;

import junit.framework.TestCase;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.ProcessingInstruction;
import org.jdom.input.SAXBuilder;

public class SchemaDocumentDefinitionTest extends TestCase {

    Document testDocument;
    Document testIncludeDocument;
    Document fritekstOnTopLevelDocument;
    Document fritekst2OnTopLevelDocument;
    Document fritekst2newOnTopLevelDocument;
    SchemaDocumentDefinition definition;
    SchemaDocumentDefinition fritekstDefinition;
    SchemaDocumentDefinition fritekst2Definition;
    SchemaDocumentDefinition fritekstOnTopLevelDefinition;

    protected void setUp() throws Exception {
        super.setUp();

        SAXBuilder builder = new SAXBuilder(
                "org.apache.xerces.parsers.SAXParser");
        builder.setValidation(true);

        /* turn on schema support */

        builder.setFeature("http://apache.org/xml/features/validation/schema",
                true);

        try {

            this.testDocument = builder.build(this.getClass().getResource("test.xml"));
            this.testIncludeDocument = builder.build(this.getClass().getResource("testInclude.xml"));
            this.fritekstOnTopLevelDocument = builder.build(this.getClass().getResource("fritekst.xml"));
            this.fritekst2OnTopLevelDocument = builder.build(this.getClass().getResource("fritekst2.xml"));
                    
            URL url = this.getClass().getResource("test.xsd");
            this.definition = new SchemaDocumentDefinition("test", url);
            this.fritekstDefinition = new SchemaDocumentDefinition("onlyFritekst", url);
            this.fritekst2Definition = new SchemaDocumentDefinition("onlyFritekst2", url);
            
            this.fritekst2newOnTopLevelDocument = builder.build(this.getClass().getResource("fritekst2new.xml")); 

        } catch (Exception e) {
            fail("Couldn't instantiate test schema" + e.getMessage());
        }
    }

    public void testConstructor() {
        /* Test that the schema has included the 'fritekst' toplevel element from subschema */
        Element e = this.testIncludeDocument.getRootElement().getChild("fritekst");
        assertEquals("UNBOUNDED_ELEMENT", this.fritekstDefinition.elementType(e));

        
        try {
            new SchemaDocumentDefinition("test", 
                    this.getClass().getClassLoader().getResource("conflict.xsd"));
            
            fail("Should be conflict");
        } catch (RuntimeException re) {
            // probably OK, but should have a proper exception
        } catch (Exception ex) {
            fail("Couldn't instantiate test schema" + ex.getMessage());
        }
    }

    public void testOnlyFritekst() {
	Element ingress = this.fritekstOnTopLevelDocument.getRootElement()
	    .getChild("ingress");
        this.fritekstDefinition.translateToEditingElement(ingress);

        assertEquals("Even",
                this.fritekstOnTopLevelDocument.getRootElement().getChild("ingress").getText());

	this.fritekstDefinition.setElementContents(ingress, "*Eva*");
	assertNotNull(ingress.getChild("utheving"));
	assertEquals("Eva", ingress.getChild("utheving").getText());
    }

    public void testOnlyFritekst2() {
    	Element ingress = this.fritekst2OnTopLevelDocument.getRootElement().getChild("ingress");
            this.fritekst2Definition.translateToEditingElement(ingress);

            assertEquals("Even er veldig *kul*",
                    this.fritekst2OnTopLevelDocument.getRootElement().getChild("ingress").getText());

    	this.fritekst2Definition.setElementContents(ingress, "*Eva*");
    	assertNotNull(ingress.getChild("avsnitt"));
    	assertEquals("Eva", ingress.getChild("avsnitt").getChild("fet").getText());
        }


    public void testElementType() {

        assertTrue(this.testDocument.getRootElement().getName().equals("test"));
        assertTrue(this.definition.getDocType().equals("test"));

        assertEquals(
            "UNBOUNDED_CHOICE_ELEMENT",
            this.definition.elementType(this.testDocument.getRootElement().getChild("grupper")));
        

        assertEquals("SEQUENCE_ELEMENT", this.definition.elementType(this.testDocument
                .getRootElement().getChild("pensumpunkt")));
        assertEquals("UNBOUNDED_CHOICE_ELEMENT", this.definition
                .elementType(this.testDocument.getRootElement().getChild(
                        "unboundedChoiceTest")));
        assertEquals("UNBOUNDED_ELEMENT", this.definition.elementType(this.testDocument
                .getRootElement().getChild("fritekst")));
        assertEquals("REQUIRED_STRING_ELEMENT", this.definition
                .elementType(this.testDocument.getRootElement().getChild(
                        "overskrift").getChild("overskrifttekst")));
        assertEquals("OPTIONAL_STRING_ELEMENT", this.definition
                .elementType(this.testDocument.getRootElement().getChild(
                        "pensumpunkt").getChild("forfattere")));

        assertEquals("plaintext", this.definition.elementType(this.testDocument
                .getRootElement().getChild("fritekst").getChild("tekstblokk")));

        Element tjall = this.testDocument.getRootElement().getChild("testEnumeration").getChild("tjall");
        assertEquals("REQUIRED_STRING_ELEMENT", this.definition.elementType(tjall));

        try {
            this.definition.elementType(new Element("tekstblokk"));
            fail("Should throw exception");
        } catch (RuntimeException e) {
            // Ok
        }
    }



    public void testTranslateToEditingElement() {
        this.definition.translateToEditingElement(this.testDocument.getRootElement()
                .getChild("fritekst"));

        assertEquals("* Masterkopi tilgjengelig i instituttets ekspedisjon",
                this.testDocument.getRootElement().getChild("fritekst").getText());

        this.definition.translateToEditingElement(this.testDocument.getRootElement()
                .getChild("attributeTest3"));

        assertNotNull(this.testDocument.getRootElement().getChild("attributeTest3").getAttributeValue("type"));
        
        
        Element overskrift = this.testDocument.getRootElement().getChild(
                "overskrift");
        overskrift.addContent(0, new ProcessingInstruction("expanded", "true"));
        this.definition.translateToEditingElement(overskrift);
        assertEquals("Lala", overskrift.getChild("overskrifttekst").getText());
        assertNotNull(overskrift.getContent(0));

        Element element = this.testDocument.getRootElement().getChild(
                "attributeTest2");
        this.definition.translateToEditingElement(element);

        assertNotNull(element.getAttribute("id"));

    }



    public void testBuildElement() {
        Element element = new Element("overskrift");
        Element attributeTest1 = new Element("attributeTest1");
        Element attributeTest2 = new Element("attributeTest2");

        try {
            this.testDocument.getRootElement().addContent(element);
            this.definition.buildElement(element);

            this.testDocument.getRootElement().addContent(attributeTest1);
            this.definition.buildElement(attributeTest1);
            this.testDocument.getRootElement().addContent(attributeTest2);
            this.definition.buildElement(attributeTest2);

            assertNotNull(attributeTest1.getAttribute("type"));
            assertNotNull(attributeTest2.getAttribute("id"));
            assertNotNull(attributeTest2.getChild("lastname"));

        } finally {
            element.detach();
            attributeTest1.detach();
        }
    }
    
    // testing additions in fritekst-v002 Schema
    
    public void testOnlyFritekst2new() {
        // <fritekst> as top level element
        Element ingress = this.fritekst2newOnTopLevelDocument.getRootElement().getChild("ingress");
        this.fritekst2Definition.translateToEditingElement(ingress);
            
        assertEquals("Er Even like *kul* som tidligere?",
                this.fritekst2newOnTopLevelDocument.getRootElement().getChild("ingress").getText());

        this.fritekst2Definition.setElementContents(ingress, "*Eva*");
        assertNotNull(ingress.getChild("avsnitt"));
        assertEquals("Eva", ingress.getChild("avsnitt").getChild("fet").getText());
        
        // <fritekst> as element, containing <sub>, <sup>, <linjeskift> and escape characters
        Element fritekst = this.fritekst2newOnTopLevelDocument.getRootElement().getChild("fritekst");
        this.fritekst2Definition.translateToEditingElement(fritekst);
        
        assertEquals("[sub:subscript] og [super:superscript]\\\nescaped newline(not implemented)\n*fet \\* stjerne* " +
                        "og _kursiv \\_ underscore_\n\\# escaped ol\n\\- escaped ul",
                this.fritekst2newOnTopLevelDocument.getRootElement().getChild("fritekst").getText());
        
        this.fritekst2Definition.setElementContents(fritekst, "*escaped \\* bold* _escaped \\_ underscore_" +
                        "[super:superscript] [sub:subscript]");
        assertNotNull(fritekst.getChild("avsnitt"));
        
        assertEquals("escaped * bold", fritekst.getChild("avsnitt").getChild("fet").getText());
        assertEquals("escaped _ underscore", fritekst.getChild("avsnitt").getChild("kursiv").getText());
        assertEquals("superscript", fritekst.getChild("avsnitt").getChild("sup").getText());
        assertEquals("subscript", fritekst.getChild("avsnitt").getChild("sub").getText());
    }
}
