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
package org.vortikal.web.view;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.JDOMSource;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.support.RequestContext;

import org.vortikal.repository.Resource;
import org.vortikal.web.InvalidModelException;
import org.vortikal.xml.StylesheetCompilationException;
import org.vortikal.xml.TransformerManager;

/**
 * XSLT transformation view. Supports transformation of both input
 * streams and JDOM documents. Uses <code>TransformerManager</code> to
 * obtain XSL stylesheets.
 *
 * <p> The transformation works as follows:
 * <ul>
 * <li> First, the model is examined for a JDOM document having key
 * <code>jdomDocument</code>. If this object exists, it is used as the
 * XML document in the transformation.
 * <li> Secondly, if the model contains an <code>InputStream</code>
 * having key <code>resourceStream</code>, a JDOM document is built
 * from that stream and used in the transformation.
 * <li> The model is also required to contain a
 * <code>org.vortikal.repository.Resource</code> object of key
 * <code>resource</code>, used for metadata information and URI
 * resolving.
 * <li>If the model contains a <code>java.util.Map</code> of key
 * <code>xsltParameters</code>, the (key, value) pairs of that map are
 * set as parameters for the transformer.
 * <!--li> If the transformation fails for some reason, the raw XML
 * stream is written to the response.
 * </ul-->
 * 
 */
public class ResourceXsltView
  extends AbstractReferenceDataProvidingView  implements InitializingBean {

    private static Log logger = LogFactory.getLog(ResourceXsltView.class);
    private TransformerManager transformerManager = null;


 

    /**
     * Sets the value of transformerManager
     *
     * @param transformerManager Value to assign to this.transformerManager
     */
    public void setTransformerManager(TransformerManager transformerManager)  {
        this.transformerManager = transformerManager;
    }



    
    public final void afterPropertiesSet() throws Exception {
        if (transformerManager == null) {
            throw new BeanInitializationException(
                "Property 'transformerManager' must be set");
        }
    }


    protected void renderMergedOutputModel(Map model, HttpServletRequest request,
                                           HttpServletResponse response)
        throws TransformerException, IOException {

        Resource resource = (Resource) model.get("resource");
        if (resource == null) {
            throw new InvalidModelException(
                "Missing resource in model " +
                "(expected a Resource object having key 'resource')");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Requested resource: " + resource);
        }

        Document document = (Document) model.get("jdomDocument");

        if (document == null) {
            InputStream inStream = (InputStream) model.get("resourceStream");
            if (inStream == null) {
                throw new InvalidModelException(
                    "Missing stream in model " +
                    "(expected an InputStream object having key 'resourceStream')");
            }

            // Build a JDOM tree of the input stream:
            try {
                
                SAXBuilder builder = new SAXBuilder();
                document = builder.build(inStream);
            
            } catch (Exception e) {            
                // FIXME: error handling
                throw new InvalidModelException(
                    "Unable to build JDOM document from input stream", e);
            }            
        }

        ByteArrayOutputStream resultBuffer = new ByteArrayOutputStream();
        ErrorListener err = new ErrorListener();
        Transformer transformer = null;

        transformer = getTransformer(resource, document);
        transformer.setErrorListener(err);
        setParameters(model, transformer);
        transformer.setParameter(
            "{http://www.uio.no/vortex/xsl-parameters}RequestContext",
            new RequestContext(request));

        // do the transformation
        JDOMSource source = new JDOMSource(document);
        source.setSystemId(
            org.vortikal.xml.AbstractPathBasedURIResolver.PROTOCOL_PREFIX +
            document.getBaseURI());
        transformer.transform(
            source, new StreamResult(resultBuffer));

        if (err.getError() != null) {
            throw err.getError();
        }

        InputStream resultStream = new ByteArrayInputStream(resultBuffer.toByteArray());
             
        OutputStream out = null;
        try {
            out = response.getOutputStream();
            String contentType = getTransformedContentType(transformer);

            if (logger.isDebugEnabled()) {
                logger.debug("Transformed Content-Type is: " + contentType);
            }
            response.setContentType(contentType);
            response.setHeader("Content-Length", "" + resultBuffer.toByteArray().length);
            if (resource.getProperty(
                    "http://www.uio.no/vortex/custom-properties", 
                    "expires-sec") == null) {
                response.setHeader("Cache-Control", "no-cache");
            }
            byte[] buffer = new byte[5000];
            int n = 0;
            while (((n = resultStream.read(buffer, 0, 5000)) > 0)) {
                out.write(buffer, 0, n);
            }
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
            if (resultStream != null) resultStream.close();
        }
    }




    /**
     * Gets the content type (MIME type + character encoding) of the
     * transformed result based on output properties of the
     * transformer.
     *
     * @param transformer a <code>Transformer</code> value
     * @return a <code>String</code>
     */
    private String getTransformedContentType(Transformer transformer) {
        String method = transformer.getOutputProperty("method");
        String encoding = transformer.getOutputProperty("encoding");
        if (encoding == null)
            encoding = "utf-8"; // FIXME: what to do if encoding == null?

        encoding = encoding.toLowerCase();
        
        String contentType = null;
        if ("text".equals(method)) {
            contentType = "text/plain;charset=" + encoding;
        } else if ("html".equals(method)) {
            contentType = "text/html;charset=" + encoding;
        } else if ("xml".equals(method)) {
            contentType = "text/xml;charset=" + encoding;
        } else {
            contentType = "application/octet-stream";
        }
        return contentType;
    }


    protected Transformer getTransformer(Resource resource, Document document)
        throws TransformerConfigurationException, TransformerException,
            StylesheetCompilationException, IOException {
        return transformerManager.getTransformer(resource, document);
    }

    protected void setParameters(Map model, Transformer transformer) {

        if (!model.containsKey("xsltParameters")) {
            return;
        }

        Object o = model.get("xsltParameters");
        if (!(o instanceof Map)) {
            logger.info("Model contains entry of name 'xsltParameters', " +
                        "but not of expected class java.util.Map. Actual class is " +
                        o.getClass().getName());
            return;
        }

        Map map = (Map) o;
        if (logger.isDebugEnabled()) {
            logger.debug("Will provide model to XSLT transformation: " + o);
        }

        for (Iterator i = map.keySet().iterator(); i.hasNext();) {
            Object key = i.next();
            Object value = map.get(key);

            if (logger.isDebugEnabled()) {
                logger.debug("Setting XSLT parameter " + key.toString() + " = "
                             + value);
            }


            transformer.setParameter(key.toString(), value);
        }

    }
    

    private void writeDocumentToResponse(HttpServletResponse response,
                                         Document document)
        throws IOException {
        Format format = Format.getPrettyFormat();
        format.setEncoding("utf-8");
        //format.setLineSeparator("\r\n");
        //format.setIndent("");

        XMLOutputter xmlOutputter = new XMLOutputter(format);
        String xml = xmlOutputter.outputString(document);
        byte[] buffer = null;
        try {
            buffer = xml.getBytes("utf-8");
        } catch (UnsupportedEncodingException ex) {
            logger.warn("Warning: UTF-8 encoding not supported", ex);
            throw new RuntimeException("UTF-8 encoding not supported");
        }

        OutputStream out = null;
        try {
            out = response.getOutputStream();
            response.setHeader("Content-Type", "text/xml");
            response.setIntHeader("Content-Length", buffer.length);
            out.write(buffer, 0, buffer.length);

        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }
    


    private class ErrorListener implements javax.xml.transform.ErrorListener {

        TransformerException error = null;
        
        
        public void error(TransformerException e) {
            error = e;
        }
        
        public void fatalError(TransformerException e) {
            error = e;
        }

        public void warning(TransformerException e) {
        }

        public TransformerException getError() {
            return error;
        }
    }
}
