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

package org.vortikal.repositoryimpl.index.dms;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.xpath.XPath;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.BeanInitializationException;
import org.vortikal.repository.Resource;
import org.vortikal.repositoryimpl.index.ExtractorException;
import org.vortikal.util.repository.ContentTypeHelper;
import org.vortikal.util.repository.JDOMResource;


/**
 * Extractor that uses a set of XPath expressions to extract
 * information from XML resources.
 *

 * <p>Configurable JavaBean properties (and those defined in the
 * {@link DMSExtractor superclass}):
 * <ul>
 *   <li><code>extractedClass</code> - the class of the extracted
 *   objects (must be a subclass of {@link DMSExtractor}.
 *   <li><code>xpathExpressions</code> - a {@link Map} of
 *   <code>(fieldName, xpathExpression)</code> entries, where
 *   <code>fieldName</code> maps to a JavaBean property of the
 *   <code>extractedClass</code>, and <code>xpathExpression</code> is
 *   an XPath expression to apply to the XML document.
 * </ul>
 * 
 */
public class XPathExtractor extends DMSExtractor {

    private Log logger = LogFactory.getLog(this.getClass());

    private Class extractedClass;
    private Map xpathExpressions;
    private Map compiledExpressions;
    
    
    public void setExtractedClass(Class extractedClass) {
        this.extractedClass = extractedClass;
    }
    
    public Class getExtractedClass() {
        return this.extractedClass;
    }

    public void setXpathExpressions(Map xpathExpressions) {
        this.xpathExpressions = xpathExpressions;
    }
    

    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        
        if (this.extractedClass == null) {
            throw new BeanInitializationException(
                "JavaBean property 'extractedClass' not specified");
        }

        if (!DMSIndexBean.class.isAssignableFrom(this.extractedClass)) {
            throw new BeanInitializationException(
                "JavaBean property 'extractedClass' must be a subclass of "
                + DMSIndexBean.class.getName());
        }

        if (this.xpathExpressions == null) {
            throw new BeanInitializationException(
                "JavaBean property 'xpathExpressions' not specified");
        }

        this.compiledExpressions = new HashMap();

        for (Iterator i = this.xpathExpressions.keySet().iterator(); i.hasNext();) {
            String key = (String) i.next();
            String value = (String) this.xpathExpressions.get(key);

            try {
                XPath xPath = XPath.newInstance(value);
                this.compiledExpressions.put(key, xPath);
            } catch (Exception e) {
                throw new BeanInitializationException(
                    "Unable to compile XPath expression '" + value + "'");
            }
        }
    }
    


    public Object extract (String uri) {

        Resource resource = super.getResource(uri);

        if (resource == null) {
            throw new ExtractorException(
                "Unable to extract resource from repository: '"
                + uri + "'");
        }

        try {
            
            Object bean = this.extractedClass.newInstance();
            super.extractInternal((DMSIndexBean) bean, resource);
            
            if (! ContentTypeHelper.isXMLContentType(resource.getContentType())) {
                return bean;
            }

            processXPathSelections(bean, resource);

            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Extracted object '" + bean + "' from URI '" + uri + "'");
            }

            return bean;
        
        } catch (Exception e) {
            throw new ExtractorException(
                "Unable to extract resource from repository: '"
                + uri + "'", e);
        }
    }


    private void processXPathSelections(Object object, Resource resource) {
        
        JDOMResource document = new JDOMResource(
            getRepository(), resource.getURI(), getToken());
        
        BeanWrapperImpl beanWrapper = new BeanWrapperImpl(this.extractedClass);
        beanWrapper.setWrappedInstance(object);
        
        for (Iterator i = this.compiledExpressions.keySet().iterator(); i.hasNext();) {
            String fieldName = (String) i.next();
            XPath expr = (XPath) this.compiledExpressions.get(fieldName);

            if (!beanWrapper.isWritableProperty(fieldName)) {
                this.logger.warn("Not a writable JavaBean property for class "
                            + this.extractedClass.getName() + ": '" + fieldName + "'");
                continue;
            }
                
            String value = null;
            try {
                value = expr.valueOf(document);                
            } catch (Exception e) {
                this.logger.warn("Unable extract XPath expression '" + expr 
                            + "' from document " + resource.getURI(), e);
                continue;
            }

            try {
                beanWrapper.setPropertyValue(fieldName, value);
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug(
                        "Set property '" + fieldName + "' to  value '" + value
                        + "' on object '" + object + "' (from XPath '" + expr + "' )");
                }
            } catch (Exception e) {
                this.logger.warn("Unable to set JavaBean property '" + fieldName
                            + "' of class " + this.extractedClass.getName()
                            + " to value " + value);
            }
        }
    }
    
    

}
