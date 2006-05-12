/* Copyright (c) 2004, University of Oslo, Norway
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
package org.vortikal.xml;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jdom.Document;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.Resource;



/**
 * XSLT transformer manager. This class simplifies the creation of
 * transformers for use in XSLT processing. 
 *
 * TODO: URIResolver supplied by manager instead of by stylesheet resolver?
 * Clarify URI resolving behavior.
 */
public class TransformerManager implements InitializingBean {

    private static Log logger = LogFactory.getLog(TransformerManager.class);

    private boolean alwaysCompile = false;
    private StylesheetTemplatesRegistry stylesheetRegistry = new StylesheetTemplatesRegistry();
    private List stylesheetReferenceResolvers;
    private List compilationURIResolvers = null;
    private List transformationURIResolvers = null;

    private ChainedURIResolver compilationURIResolver = null;
    private ChainedURIResolver transformationURIResolver = null;



    /**
     * Sets the value of alwaysCompile
     *
     * @param alwaysCompile 
     */
    public void setAlwaysCompile(boolean alwaysCompile) {
        this.alwaysCompile = alwaysCompile;
    }
    


    /**
     * Sets the value of stylesheetRegistry
     *
     * @param stylesheetRegistry Value to assign to this.stylesheetRegistry
     */
    public void setStylesheetRegistry(StylesheetTemplatesRegistry stylesheetRegistry)  {
        this.stylesheetRegistry = stylesheetRegistry;
    }


    /**
     * Gets the value of stylesheetRegistry
     *
     */
    public StylesheetTemplatesRegistry  getStylesheetRegistry()  {
        return this.stylesheetRegistry;
    }


    /**
     * Sets the value of stylesheetReferenceResolvers
     *
     * @param stylesheetReferenceResolvers Value to assign to this.stylesheetReferenceResolvers
     */
    public void setStylesheetReferenceResolvers(List stylesheetReferenceResolvers)  {
        this.stylesheetReferenceResolvers = stylesheetReferenceResolvers;
    }


    /**
     * Sets the URI resolvers used for stylesheet compilation.
     * Specifically, this involves the XSLT <code>import</code> and
     * <code>include</code> constructs.
     *
     * @param compilationURIResolvers an <code>URIResolver[]</code> value
     */
    public void setCompilationURIResolvers(List compilationURIResolvers) {
        this.compilationURIResolvers = compilationURIResolvers;
        StylesheetURIResolver[] resolverArray =
            new StylesheetURIResolver[compilationURIResolvers.size()];
        int n = 0;
        for (Iterator i = compilationURIResolvers.iterator(); i.hasNext();) {
            resolverArray[n++] = (StylesheetURIResolver) i.next();
        }
        this.compilationURIResolver = new ChainedURIResolver(resolverArray);
    }
    

    /**
     * Sets the URI resolvers that are used during transformation.
     * The transformation URI resolvers are responsible for resolving
     * URIs generated by the XSLT <code>document()</code> function.
     *
     * @param transformationURIResolvers an <code>URIResolver[]</code> value
     */
    public void setTransformationURIResolvers(List transformationURIResolvers) {
        this.transformationURIResolvers = transformationURIResolvers;
        URIResolver[] resolverArray = (URIResolver[])
            transformationURIResolvers.toArray(
                new URIResolver[transformationURIResolvers.size()]);
        this.transformationURIResolver = new ChainedURIResolver(resolverArray);
    }
    

    public void afterPropertiesSet() throws Exception {
        if (stylesheetReferenceResolvers == null) {
            throw new BeanInitializationException(
                "Bean property 'stylesheetReferenceResolvers' must be set");
        }
        if (compilationURIResolvers == null) {
            throw new BeanInitializationException(
                "Bean property 'compilationURIResolvers' must be set");
        }
        if (transformationURIResolvers == null) {
            throw new BeanInitializationException(
                "Bean property 'transformationURIResolvers' must be set");
        }

        logger.info("Using compilation style sheet URI resolvers: "
                    + this.compilationURIResolvers);

        logger.info("Using transformation style sheet URI resolvers: "
                    + this.transformationURIResolvers);
    }
    
    
    /**
     * Get a transformer for the given stylesheetIdentifier.
     * 
     * @param stylesheetIdentifier an abstract stylesheet identifier,
     * e.g. a URL or some other reference.
     * @return a transformer for the (compiled) stylesheet.
     * @throws IOException if an I/O error occurs
     * @throws TransformerConfigurationException if the transformer
     * could not be instantiated.
     * @throws StylesheetCompilationException if an error occurred
     * while compiling the stylesheet.
     */
    public Transformer getTransformer(String stylesheetIdentifier) throws 
        IOException, TransformerException, TransformerConfigurationException,
        StylesheetCompilationException {
        
        StylesheetURIResolver resolver = getStylesheetResolver(stylesheetIdentifier);
        
        if (resolver == null) {
            throw new StylesheetCompilationException(
                "Unable to compile XSLT stylesheet '" + stylesheetIdentifier +
                "': No matching stylesheet resolvers");
        }


        Date lastCompile = stylesheetRegistry.getLastModified(stylesheetIdentifier);
        Date lastModified = resolver.getLastModified(stylesheetIdentifier);
        if (logger.isDebugEnabled()) {
            logger.debug("Stylesheet '" + stylesheetIdentifier
                         + "' was last compiled: " + lastCompile
                         + ", last modified " + lastModified);
        }

        Templates templates = null;

        if (alwaysCompile || lastCompile == null || lastModified == null ||
            (lastModified.getTime() > lastCompile.getTime())) {

            if (logger.isDebugEnabled()) {
                logger.debug("Compiling stylesheet '" + stylesheetIdentifier);
            }
            templates = stylesheetRegistry.compile(
                stylesheetIdentifier, this.compilationURIResolver, lastModified);
        }
        
        templates = stylesheetRegistry.getTemplates(stylesheetIdentifier);

        if (templates == null) {
            throw new StylesheetCompilationException(
                "Unable to compile XSLT stylesheet '" +
                stylesheetIdentifier + "'");
        }

        Transformer transformer = templates.newTransformer();
        transformer.setURIResolver(this.transformationURIResolver);
        if (logger.isDebugEnabled()) {
            logger.debug(
                "Returning [transformer: " + transformer + " URI resolver: "
                + this.transformationURIResolver
                + "] for style sheet identifier " + stylesheetIdentifier + "");
        }

        return transformer;
    }
    
    /**
     * Gets a transformer for a given XML resource. 
     *
     * @param resource the XML resource
     * @param document a JDOM Document representation of the XML resource
     * @return a transformer representing a compiled stylesheet with
     * URI resolvers set.
     * @exception IOException if an error occurs
     * @exception TransformerConfigurationException if an error occurs
     * @exception StylesheetCompilationException if an error occurs
     */
    public Transformer getTransformer(Resource resource, Document document)
        throws IOException, TransformerException, TransformerConfigurationException,
        StylesheetCompilationException {

        String stylesheetIdentifier = resolveTemplateReference(resource, document);

        if (stylesheetIdentifier == null) {
            throw new StylesheetCompilationException(
                "Unable to find XSLT stylesheet identifier for resource " +
                resource);
        }
        return getTransformer(stylesheetIdentifier);
    }



    private String resolveTemplateReference(Resource resource, Document document) {
        for (Iterator i = this.stylesheetReferenceResolvers.iterator(); i.hasNext();) {
            StylesheetReferenceResolver resolver = (StylesheetReferenceResolver)i.next();
            // Obtain the stylesheet identifier:
            String reference = resolver.getStylesheetIdentifier(resource, document);
            
            if (reference != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Found stylesheet identifier for resource '" +
                                 resource + "': '" + reference + "'");
                }
                return reference;
            }
        }
        return null;
    }
    


    private StylesheetURIResolver getStylesheetResolver(String stylesheetIdentifier) {
        for (Iterator i = compilationURIResolvers.iterator(); i.hasNext();) {
            StylesheetURIResolver resolver = (StylesheetURIResolver) i.next();
            // Map the identifier to a physical resource:
            if (resolver.matches(stylesheetIdentifier)) {
                if (logger.isDebugEnabled())
                    logger.debug("Using stylesheet resolver " + resolver +
                                 " for stylesheet identifier: '" + stylesheetIdentifier + "'");
                return resolver;
            }
        }
        return null;
    }
    


    private class ChainedURIResolver implements URIResolver {
        private URIResolver[] chain = null;

        public ChainedURIResolver(URIResolver[] chain) {
            this.chain = chain;
        }

        public Source resolve(String href, String base) throws TransformerException {
            String uri = "[href = " + href + ", base = " + base + "]";

            for (int i = 0; i < this.chain.length; i++) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Attempting to resolve URI " + uri
                                 + " using resolver " + this.chain[i]);
                }

                Source s = this.chain[i].resolve(href, base);
                if (s != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Resolved URI " + uri + " to source " + s
                                     + " using resolver " + this.chain[i]);
                    }
                    return s;
                }                
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Unable to resolve URI " + uri + " using resolvers "
                             + java.util.Arrays.asList(this.chain));
            }
            // FIXME: return empty Source
            return null;
        }

        public String toString() {
            return "Chain: " + java.util.Arrays.asList(this.chain);
        }
        
    }
}
