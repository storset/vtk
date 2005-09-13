package org.vortikal.edit.xml;

import java.net.URL;

import junit.framework.TestCase;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.ProcessingInstruction;
import org.jdom.input.SAXBuilder;

public class SchemaDocumentDefinitionTest extends TestCase {

    private static final String TEST_XML = 
        "org/vortikal/edit/xml/test.xml";
    private static final String TEST_INCLUDE_XML = 
        "org/vortikal/edit/xml/testInclude.xml";
    private static final String FRITEKST_XML = 
        "org/vortikal/edit/xml/fritekst.xml";
    private static final String FRITEKST2_XML = 
        "org/vortikal/edit/xml/fritekst2.xml";
    private static final String TEST_XSD = 
        "org/vortikal/edit/xml/test.xsd";
    private static final String CONFLICT_XSD = 
        "org/vortikal/edit/xml/conflict.xsd";
    
   Document testDocument = null;
    Document testIncludeDocument = null;
    Document fritekstOnTopLevelDocument;
    Document fritekst2OnTopLevelDocument;
    SchemaDocumentDefinition definition = null;
    SchemaDocumentDefinition fritekstDefinition = null;
    SchemaDocumentDefinition fritekst2Definition = null;
    SchemaDocumentDefinition fritekstOnTopLevelDefinition = null;

    protected void setUp() throws Exception {
        super.setUp();

        SAXBuilder builder = new SAXBuilder(
                "org.apache.xerces.parsers.SAXParser");
        builder.setValidation(true);

        /* turn on schema support */

        builder.setFeature("http://apache.org/xml/features/validation/schema",
                true);

        try {

            URL testXML = this.getClass().getClassLoader().getResource(
                TEST_XML);
            testDocument = builder.build(testXML);

            URL testIncludeXML = this.getClass().getClassLoader().getResource(
            TEST_INCLUDE_XML);
            testIncludeDocument = builder.build(testIncludeXML);

            URL fritekstOnTopLevelXML = this.getClass().getClassLoader().getResource(
            FRITEKST_XML);
            fritekstOnTopLevelDocument = builder.build(fritekstOnTopLevelXML);

            URL fritekst2OnTopLevelXML = this.getClass().getClassLoader().getResource(
                    FRITEKST2_XML);
                    fritekst2OnTopLevelDocument = builder.build(fritekst2OnTopLevelXML);
                    
            URL testXSD = this.getClass().getClassLoader().getResource(
               TEST_XSD);
            definition = new SchemaDocumentDefinition("test", testXSD);

            fritekstDefinition = new SchemaDocumentDefinition("onlyFritekst", testXSD);
            
            fritekst2Definition = new SchemaDocumentDefinition("onlyFritekst2", testXSD);

        } catch (Exception e) {
            fail("Couldn't instantiate test schema" + e.getMessage());
        }
    }

    public void testConstructor() {
        /* Test that the schema has included the 'fritekst' toplevel element from subschema */
        Element e = testIncludeDocument.getRootElement().getChild("fritekst");
        assertEquals("UNBOUNDED_ELEMENT", fritekstDefinition.elementType(e));

        
        try {
            URL conflictXSD = this.getClass().getClassLoader().getResource(
            CONFLICT_XSD);
            new SchemaDocumentDefinition("test", conflictXSD);
            fail("Should be conflict");
        } catch (RuntimeException re) {
            // probably OK, but should have a proper exception
        } catch (Exception ex) {
            fail("Couldn't instantiate test schema" + ex.getMessage());
        }
    }

    public void testOnlyFritekst() {
	Element ingress = fritekstOnTopLevelDocument.getRootElement()
	    .getChild("ingress");
        fritekstDefinition.translateToEditingElement(ingress);

        assertEquals("Even",
                fritekstOnTopLevelDocument.getRootElement().getChild("ingress").getText());

	fritekstDefinition.setElementContents(ingress, "*Eva*");
	assertNotNull(ingress.getChild("utheving"));
	assertEquals("Eva", ingress.getChild("utheving").getText());
    }

    public void testOnlyFritekst2() {
    	Element ingress = fritekst2OnTopLevelDocument.getRootElement().getChild("ingress");
            fritekst2Definition.translateToEditingElement(ingress);

            assertEquals("Even er veldig *kul*",
                    fritekst2OnTopLevelDocument.getRootElement().getChild("ingress").getText());

    	fritekst2Definition.setElementContents(ingress, "*Eva*");
    	assertNotNull(ingress.getChild("avsnitt"));
    	assertEquals("Eva", ingress.getChild("avsnitt").getChild("fet").getText());
        }


    public void testElementType() {

        assertTrue(testDocument.getRootElement().getName().equals("test"));
        assertTrue(definition.getDocType().equals("test"));

        assertEquals(
            "UNBOUNDED_CHOICE_ELEMENT",
            definition.elementType(testDocument.getRootElement().getChild("grupper")));
        

        assertEquals("SEQUENCE_ELEMENT", definition.elementType(testDocument
                .getRootElement().getChild("pensumpunkt")));
        assertEquals("UNBOUNDED_CHOICE_ELEMENT", definition
                .elementType(testDocument.getRootElement().getChild(
                        "unboundedChoiceTest")));
        assertEquals("UNBOUNDED_ELEMENT", definition.elementType(testDocument
                .getRootElement().getChild("fritekst")));
        assertEquals("REQUIRED_STRING_ELEMENT", definition
                .elementType(testDocument.getRootElement().getChild(
                        "overskrift").getChild("overskrifttekst")));
        assertEquals("OPTIONAL_STRING_ELEMENT", definition
                .elementType(testDocument.getRootElement().getChild(
                        "pensumpunkt").getChild("forfattere")));

        assertEquals("plaintext", definition.elementType(testDocument
                .getRootElement().getChild("fritekst").getChild("tekstblokk")));

        Element tjall = testDocument.getRootElement().getChild("testEnumeration").getChild("tjall");
        assertEquals("REQUIRED_STRING_ELEMENT", definition.elementType(tjall));

        try {
            definition.elementType(new Element("tekstblokk"));
            fail("Should throw exception");
        } catch (RuntimeException e) {
            // Ok
        }
    }



    public void testTranslateToEditingElement() {
        definition.translateToEditingElement(testDocument.getRootElement()
                .getChild("fritekst"));

        assertEquals("* Masterkopi tilgjengelig i instituttets ekspedisjon",
                testDocument.getRootElement().getChild("fritekst").getText());

        definition.translateToEditingElement(testDocument.getRootElement()
                .getChild("attributeTest3"));

        assertNotNull(testDocument.getRootElement().getChild("attributeTest3").getAttributeValue("type"));
        
        
        Element overskrift = testDocument.getRootElement().getChild(
                "overskrift");
        overskrift.addContent(0, new ProcessingInstruction("expanded", "true"));
        definition.translateToEditingElement(overskrift);
        assertEquals("Lala", overskrift.getChild("overskrifttekst").getText());
        assertNotNull(overskrift.getContent(0));

        Element element = testDocument.getRootElement().getChild(
                "attributeTest2");
        definition.translateToEditingElement(element);

        assertNotNull(element.getAttribute("id"));

    }



    public void testBuildElement() {
        Element element = new Element("overskrift");
        Element attributeTest1 = new Element("attributeTest1");
        Element attributeTest2 = new Element("attributeTest2");

        try {
            testDocument.getRootElement().addContent(element);
            definition.buildElement(element);

            testDocument.getRootElement().addContent(attributeTest1);
            definition.buildElement(attributeTest1);
            testDocument.getRootElement().addContent(attributeTest2);
            definition.buildElement(attributeTest2);

            assertNotNull(attributeTest1.getAttribute("type"));
            assertNotNull(attributeTest2.getAttribute("id"));
            assertNotNull(attributeTest2.getChild("lastname"));

        } finally {
            element.detach();
            attributeTest1.detach();
        }
    }
}
