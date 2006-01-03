package org.vortikal.edit.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;

/** Simple {@link org.xml.sax.XMLFilter XML filter} that prefixes any unqualified
 * space attributes with "xml:"
 * 
 * NOTE: This might be called a hack...
 */
public class XMLSpaceCorrectingXMLFilter extends XMLFilterImpl {

    public void startElement(String uri, String localName, String rawName,
            Attributes attributes) throws SAXException {

        Attributes attrs = attributes;
        for (int i = 0; i < attrs.getLength(); i++) {
            String qname = attrs.getQName(i);
            if ("space".equals(qname)) {
                AttributesImpl impl = new AttributesImpl(attrs);
                impl.setQName(i, "xml:space");
                attrs = impl;
            }
        }
        super.startElement(uri, localName, rawName, attrs);

    }

}