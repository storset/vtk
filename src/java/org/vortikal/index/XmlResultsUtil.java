/* Copyright (c) 2005, University of Oslo, Norway
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
package org.vortikal.index;

import java.beans.PropertyDescriptor;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.springframework.beans.BeanWrapperImpl;

/**
 * Convert result sets into XML document, looking something like this:
 * <results n="NUMBER_OF_HITS"
 *   <result uri="URI" score="SCORE" index="INDEX">
 *     <FIELDNAME1>FIELDVALUE1</FIELDNAME1>
 *     <FIELDNAME2>FIELDVALUE2</FIELDNAME2>
 *     ...
 *   </result>
 *   ...
 * </results>
 *  
 *  Note that this class will only be able to provide XML if the field names
 *  follow the XML naming conventions.
 *  Therefore, it might easily throw JDOM runtime exceptions.
 *  
 * @author oyviste
 *
 */
public final class XmlResultsUtil {
    
    public static final Document generateXMLResults(Results results) 
        throws Exception {
        Element rootElement = new Element("results");
        Document doc = new Document(rootElement);
        rootElement.setAttribute(new Attribute("n", Integer.toString(results.getSize())));
        
        BeanWrapperImpl wrapper = new BeanWrapperImpl(results.getResultClass());

        for (int i=0; i<results.getSize(); i++) {
            Object result = results.getResult(i);
            wrapper.setWrappedInstance(result);

            ResultMetadata meta = results.getResultMetadata(i);
            Element resultElement = new Element("result");
            resultElement.setAttribute(new Attribute("uri", meta.getUri()));
            resultElement.setAttribute(new Attribute("score", Float.toString(meta.getScore())));
            resultElement.setAttribute(new Attribute("index", Integer.toString(i)));

            PropertyDescriptor[] props = wrapper.getPropertyDescriptors();
            for (int u=0; u<props.length; u++) {
                String name = props[u].getName();
                Object value = wrapper.getPropertyValue(name);
                if (value == null) continue;
                
                Element fieldElement = new Element(name);
                fieldElement.addContent(value.toString());
                resultElement.addContent(fieldElement);
            }
            
            rootElement.addContent(resultElement);
        }
        
        return doc;
    }
}
