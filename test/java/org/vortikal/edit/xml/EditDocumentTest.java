package org.vortikal.edit.xml;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.vortikal.repository.Resource;

public class EditDocumentTest extends TestCase {

    private static final String TEST_XML = 
        "org/vortikal/edit/xml/test.xml";
    private static final String TEST_XSD = 
        "org/vortikal/edit/xml/test.xsd";
    
	Document schemaDocument = null;
	EditDocument testDocument = null;
	SchemaDocumentDefinition definition = null;

	protected void setUp() throws Exception {
            super.setUp();

            SAXBuilder builder =
                new SAXBuilder("org.apache.xerces.parsers.SAXParser");
            builder.setValidation(true);

            /* turn on schema support */
            builder.setFeature(
                "http://apache.org/xml/features/validation/schema", true);

            try {
                URL testXML = this.getClass().getClassLoader().getResource(TEST_XML);

                Document d = builder.build(testXML);

                testDocument = EditDocument.createEditDocument(
                    d.getRootElement(), d.getDocType(), new Resource());

                URL testXSD = this.getClass().getClassLoader().getResource(
                    TEST_XSD);
                definition = new SchemaDocumentDefinition("test", testXSD);
	
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
        
        testDocument.getRootElement().addContent(0, e);
        testDocument.addContentsToElement(e, map, definition);
        
        assertEquals("1", e.getAttributeValue("type"));
        assertEquals("Lala", e.getText());

        
        /* Unbounded element with attribute */
        e = new Element("attributeTest3");
        e.setAttribute("type", "2");
        
        testDocument.getRootElement().addContent(0, e);
        testDocument.addContentsToElement(e, map, definition);
        
        assertEquals("1", e.getAttributeValue("type"));
        assertEquals("Lala", e.getChild("tekstblokk").getText());

    }
}
