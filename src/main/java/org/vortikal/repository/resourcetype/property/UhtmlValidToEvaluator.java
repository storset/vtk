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

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertyEvaluationContext;
import org.vortikal.repository.resourcetype.PropertyEvaluator;
import org.vortikal.repository.resourcetype.PropertyType.Type;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class UhtmlValidToEvaluator implements PropertyEvaluator {

    private ValueFactory valueFactory;

    public boolean evaluate(Property property, PropertyEvaluationContext ctx) throws PropertyEvaluationException {
        if (ctx.getContent() == null) {
            return false;
        }
        Document document = null;
        
        try {
            document = (Document) ctx.getContent().getContentRepresentation(Document.class);
        } catch (Exception e) {
            throw new PropertyEvaluationException(
                    "Unable to get DOM representation of content", e);
        }

        String validTo = getMetadata(document.getDocumentElement());
        if (validTo == null) {
            return false;
        }                
        
        Value value = null;

        Type type = property.getDefinition().getType();
        try {
            value = this.valueFactory.createValue(validTo, type);
        } catch (Exception e) {
            return false;
        }

        property.setValue(value);
        return true;
    }


    private String getMetadata(Node node) {
        if (node == null || node.getNodeType() != Node.ELEMENT_NODE) {
            return null;
        }
        
        String nodeName = node.getNodeName();

        if (nodeName.equals("meta")) {
            if (!node.hasAttributes())
                return null;
            
            NamedNodeMap nnm = node.getAttributes();
            Node name = nnm.getNamedItem("name");
            Node value = nnm.getNamedItem("value");
            
            if (name == null || value == null || !"dato.holdbarhet".equals(name.getNodeValue()))
                return null;

            return value.getNodeValue();
        } else if (!"body".equals(nodeName) && !"link".equals(nodeName) && !"title".equals(nodeName)) {
            // Invoke the recursive method on the children (if any).
            NodeList children = node.getChildNodes();
            if (children != null) {
                int len = children.getLength();
                for (int i = 0; i < len; i++) {
                    String s = getMetadata(children.item(i));
                    if (s != null)
                        return s;
                }
            }
        }
        
        return null;

    }

    @Required
    public void setValueFactory(ValueFactory valueFactory) {
        this.valueFactory = valueFactory;
    }

}
