package org.vortikal.edit.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/** Simple validation helper
 *
 */
public class Validator {

    private static Log logger = LogFactory.getLog(Validator.class);
    
    /** Validates a JDOM {@link Document} throwing appropriate exceptions when failing
     * @param document
     * @throws IOException
     * @throws JDOMException
     */
    public void validate(Document document) throws IOException, XMLEditException {
        Format format = Format.getRawFormat();
        format.setLineSeparator("\n");

        XMLOutputter xmlOutputter = new XMLOutputter();
        xmlOutputter.setFormat(format);
        
        SAXBuilder builder = 
            new SAXBuilder("org.apache.xerces.parsers.SAXParser");
        builder.setValidation(true);

        /* turn on schema support */
        builder.setFeature("http://apache.org/xml/features/validation/schema",
                true);
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            xmlOutputter.output(document, outputStream);
            InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            builder.build(inputStream);
        } catch (JDOMException e) {
            logger.error("Document not validating:\n" + xmlOutputter.outputString(document));
            throw new XMLEditException("The document isn't valid", e);
        } 
    }
}
