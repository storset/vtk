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

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;

import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.resourcetype.Content;
import org.vortikal.repository.resourcetype.ContentModificationPropertyEvaluator;
import org.vortikal.security.Principal;
import org.vortikal.xml.StylesheetReferenceResolver;




/**
 * Evaluate XML stylesheet reference. Stylesheet references are
 * obtained through the {@link StylesheetReferenceResolver} API.
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>stylesheetReferenceResolvers</code> - an array of
 *   {@link StylesheetReferenceResolver} objects. These resolvers are
 *   queried in succession, and the first one to match decides the
 *   stylesheet reference.
 * </ul>
 */
public class XMLStylesheetEvaluator implements ContentModificationPropertyEvaluator,
                                               InitializingBean {

    private Log logger = LogFactory.getLog(this.getClass());

    private StylesheetReferenceResolver[] stylesheetReferenceResolvers;


    public void setStylesheetReferenceResolvers(
        StylesheetReferenceResolver[] stylesheetReferenceResolvers) {
        this.stylesheetReferenceResolvers = stylesheetReferenceResolvers;
    }
    

    public void afterPropertiesSet() {
        if (this.stylesheetReferenceResolvers == null) {
            throw new BeanInitializationException(
                "JavaBean property 'stylesheetReferenceResolvers' not set");
        }
    }
    

    public boolean contentModification(Principal principal, 
                                       Property property, 
                                       PropertySet ancestorPropertySet, 
                                       Content content, 
                                       Date time)
            throws PropertyEvaluationException {

        try {
            Document doc = (Document) content.getContentRepresentation(org.jdom.Document.class);
            if (doc == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Content representation '"
                                 + org.jdom.Document.class.getName() + "' not available");
                }
                return false;
            }
            
            String stylesheetReference = resolveTemplateReference(ancestorPropertySet, doc);
            
            if (stylesheetReference == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Did not find stylesheet identifier for resource '" +
                                 ancestorPropertySet + "'");
                }
                return false;
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Found stylesheet identifier for resource '" +
                             ancestorPropertySet + "': '" + stylesheetReference + "'");
            }
            property.setStringValue(stylesheetReference);
            return true;
            

        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Unable to find stylesheet reference for resource "
                             + ancestorPropertySet, e);
            }
            return false;
        }
    }

    private String resolveTemplateReference(PropertySet resource, Document document) {
        for (int i = 0; i < stylesheetReferenceResolvers.length; i++) {
            StylesheetReferenceResolver resolver = stylesheetReferenceResolvers[i];
            String reference = resolver.getStylesheetIdentifier(resource, document);
            
            if (reference != null) {
                return reference;
            }
        }
        return null;
    }


}
