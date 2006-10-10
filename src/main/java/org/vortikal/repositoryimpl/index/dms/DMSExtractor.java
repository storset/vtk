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

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.Resource;
import org.vortikal.repositoryimpl.index.AbstractRepositoryExtractor;
import org.vortikal.repositoryimpl.index.ExtractorException;
import org.vortikal.security.Principal;
import org.vortikal.util.repository.ContentTypeHelper;

/**
 * DMS <code>Extractor</code>
 * 
 * @author oyviste
 */
public class DMSExtractor extends AbstractRepositoryExtractor {

    private static Log logger = LogFactory.getLog(DMSExtractor.class);
    
    private final org.jdom.Namespace XSI_NAMESPACE = 
        org.jdom.Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");

    private SimpleDateFormat dateFormatter = 
            new SimpleDateFormat(DMSIndexBean.DATEFORMAT);
    
    public Object extract (String uri) {
        DMSIndexBean bean = new DMSIndexBean();
        Resource resource = getResource(uri);
        
        if (resource == null) {
            throw new ExtractorException(
                "Unable to extract resource from repository: '"
                + uri + "'");
        }

        extractInternal(bean, resource);
        return bean;
    }
    
    protected void extractInternal(DMSIndexBean bean, Resource resource) {
        
        Principal owner = resource.getOwner();
        Principal lastModifiedBy = resource.getModifiedBy();
        String encoding = resource.getCharacterEncoding();

        bean.setCreationDate(this.dateFormatter.format(resource.getCreationTime()));
        bean.setLastModified(this.dateFormatter.format(resource.getLastModified()));
        bean.setContentLength(Long.toString(resource.getContentLength()));
        bean.setContentType(resource.getContentType());
        bean.setEncoding(encoding != null ? encoding : "");
        bean.setDavResourceType(resource.isCollection() ? "collection" : "");
        bean.setSchemaId(getXMLResourceSchemaId(resource));
        bean.setOwner(owner.getQualifiedName()); 
        bean.setLastModifiedBy(lastModifiedBy.getQualifiedName());
        bean.setVortexResourceType(getVortexCustomPropertyValue(resource, "resource-type"));
        
    }
    

    protected String getPropertyValue(Resource resource, 
            Namespace namespace, String name) {
        Property prop = resource.getProperty(namespace, name);

        if (prop != null && prop.getValue() != null)
            return prop.getStringValue();
        
        return "";
    }
    
    protected String getVortexCustomPropertyValue(Resource resource, String name) {
        return getPropertyValue(resource, Namespace.CUSTOM_NAMESPACE, name);
    }
    
    protected String getXMLResourceSchemaId(Resource resource) {
        // TODO: Get as property fram resource instead ?
        if (! ContentTypeHelper.isXMLContentType(resource.getContentType())) {
            return "";
        }
        
        InputStream inputStream = getResourceInputStream(resource.getURI());
        
        if (inputStream == null) {
            logger.warn("Unable to get input stream for resource " 
                        + resource.getURI());
            return "";
        }
        
        SAXBuilder builder = new SAXBuilder();
        String schemaId = null;
        try {
            org.jdom.Document document = builder.build(inputStream);
            schemaId = document.getRootElement().getAttributeValue(
            "noNamespaceSchemaLocation", this.XSI_NAMESPACE);

        } catch (JDOMException je) {
            logger.warn("Got JDOMException when trying to extract schema location " +
                        "for XML resource " + resource.getURI());
        } catch (IOException io) {
            logger.warn("Got IOException when trying to extract schema location " +
                        "for XML resource " + resource.getURI() + ": " + io.getMessage());
        } catch(Exception e) { // Extractors should handle all exceptions and generally 
                               // be robust.
            logger.warn("Exception while trying to extract schema location for XML " +
                        "resource " + resource.getURI() + ": " + e.getMessage());
        }
        finally {
            try {
                inputStream.close();
            } catch (IOException io) {
                logger.warn("Exception when closing input stream for resource " +
                            resource.getURI());
            }
        }
        
        return schemaId != null ? schemaId : "";
    }

    public Class getExtractedClass() {
        return DMSIndexBean.class;
    }
}
