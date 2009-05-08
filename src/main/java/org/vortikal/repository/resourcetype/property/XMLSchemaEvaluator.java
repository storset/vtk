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

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertyEvaluationContext;
import org.vortikal.repository.resourcetype.PropertyEvaluator;

/**
 * Evaluate XML schema property on content modification. Gets the XML
 * schema URL from the 
 * <code>http://www.w3.org/2001/XMLSchema-instance:noNamespaceSchemaLocation</code>
 * attribute on the root element of the document.
 */
public class XMLSchemaEvaluator implements PropertyEvaluator {

    private String xmlSchemaAttributeNamespace = "http://www.w3.org/2001/XMLSchema-instance";
    private String xmlSchemaAttributeName = "noNamespaceSchemaLocation";

    public boolean evaluate(Property property, PropertyEvaluationContext ctx) throws PropertyEvaluationException {
        if (ctx.getContent() == null) {
            return false;
        }
        String schemaLocation = null;
        try {
            Document doc = (Document) ctx.getContent().getContentRepresentation(org.jdom.Document.class);
            Element root = doc.getRootElement();
            Namespace ns = Namespace.getNamespace(this.xmlSchemaAttributeNamespace);
            schemaLocation = root.getAttributeValue(this.xmlSchemaAttributeName, ns);
        } catch (Exception e) {
            return false;
        }

        if (schemaLocation != null) {
            property.setStringValue(schemaLocation);
            return true;
        } 
        return false;
    }
}
