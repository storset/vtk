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

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;

import org.springframework.beans.factory.BeanInitializationException;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.resourcetype.Content;
import org.vortikal.repository.resourcetype.ContentModificationPropertyEvaluator;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFactory;
import org.vortikal.security.Principal;



/**
 * Evaluates XPath expressions on an XML document.
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>expression</code> - the XPath expression
 *   <li><code>valueFactory</code> - an optional {@link ValueFactory}
 *   to apply to the extracted value(s). If not specified, all
 *   extracted values are treated as string values.
 * </ul>
 *
 * <p>TODO: Handle list values.
 */
public class XPathEvaluator implements ContentModificationPropertyEvaluator {

    private Log logger = LogFactory.getLog(this.getClass());

    private ValueFactory valueFactory;
    private XPath xPath;


    public void setValueFactory(ValueFactory valueFactory) {
        this.valueFactory = valueFactory;
    }      
    

    public void setExpression(String value) throws JDOMException {
        this.xPath = XPath.newInstance(value);
    }
    

    public void afterPropertiesSet() {
        
        if (this.xPath == null) {
            throw new BeanInitializationException(
                "JavaBean property 'xPath' not specified");
        }
    }
    

    public boolean contentModification(Principal principal, 
                                       Property property, 
                                       PropertySet ancestorPropertySet, 
                                       Content content, 
                                       Date time) {
        Document doc = null;

        try {
            doc = (Document) content.getContentRepresentation(Document.class);
            String stringVal = this.xPath.valueOf(doc);
            
            Value value = null;
            if (this.valueFactory != null) {
                int type = property.getDefinition().getType();
                value = this.valueFactory.createValue(stringVal, type);
            } else {
                value = new Value();
                value.setValue(stringVal);
            }
            property.setValue(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
