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
package org.vortikal.repository.resourcetype.property;

import java.nio.charset.Charset;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.resourcetype.Content;
import org.vortikal.repository.resourcetype.ContentModificationPropertyEvaluator;
import org.vortikal.security.Principal;
import org.vortikal.util.text.TextUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;



public class HtmlCharacterEncodingEvaluator implements ContentModificationPropertyEvaluator {

    private Log logger = LogFactory.getLog(this.getClass());

    public boolean contentModification(Principal principal, Property property,
            PropertySet ancestorPropertySet, Content content, Date time)
            throws PropertyEvaluationException {

        Document doc = null;

        try {
            doc = (Document) content.getContentRepresentation(Document.class);
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Unable to get DOM representation of content", e);
            }
            return false;
        }

        String encoding = findEncodingFromMetaElement(doc);

        if (encoding == null) {
            return false;
        }

        try {
            Charset.forName(encoding);
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Unknown character encoding " + encoding, e);
            }
            return false;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Setting property value: " + encoding);
        }
        property.setStringValue(encoding);
        return true;
    }


    private String findEncodingFromMetaElement(Document doc) {
        String encoding = null;

        NodeList metaElements = doc.getElementsByTagName("meta");
        for (int i = 0; i < metaElements.getLength(); i++) {
            Node meta = metaElements.item(i);

            if (meta.hasAttributes()) {
                NamedNodeMap attrMap = meta.getAttributes();
                Node httpEquivAttr = attrMap.getNamedItem("http-equiv");

                if (httpEquivAttr != null) {
                    String httpEquiv = httpEquivAttr.getNodeValue();

                    if (httpEquiv!= null && "Content-Type".toLowerCase().equals(httpEquiv.toLowerCase())) {
                        Node contentAttr = attrMap.getNamedItem("content");

                        if (contentAttr != null) {
                            String content = contentAttr.getNodeValue();
                            if (content != null) {
                                content = content.toLowerCase();
                                encoding = TextUtils.extractField(content, "charset", ";");
                                if (encoding != null) {
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        return encoding;
    }
    
}
