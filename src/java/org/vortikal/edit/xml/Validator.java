package org.vortikal.edit.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/** Simple validation helper
 *
 */
public class Validator {

    /** Validates a JDOM {@link Document} throwing appropriate exceptions when failing
     * @param document
     * @throws IOException
     * @throws JDOMException
     */
    public void validate(Document document) throws IOException, JDOMException {
        Format format = Format.getRawFormat();
        format.setLineSeparator("\n");

        XMLOutputter xmlOutputter = new XMLOutputter();
        xmlOutputter.setFormat(format);

        String xml = xmlOutputter.outputString(document);

        // FIXME: God help us:
        xml = xml.replaceAll(" space=\"preserve\">", " xml:space=\"preserve\">");

        // FIXME: Replace space for empty elements
        xml = xml.replaceAll(" space=\"preserve\" />",
                " xml:space=\"preserve\" />");

        byte[] buf = xml.getBytes("utf-8");

        InputStream inputStream = new ByteArrayInputStream(buf);
        
        SAXBuilder builder = 
            new SAXBuilder("org.apache.xerces.parsers.SAXParser");
        builder.setValidation(true);

        /* turn on schema support */
        builder.setFeature("http://apache.org/xml/features/validation/schema",
                true);

        builder.build(inputStream);

    }
}
