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
package org.vortikal.edit.plaintext;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

import org.vortikal.repository.Lock;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.io.StreamUtil;
import org.vortikal.util.repository.ContentTypeHelper;
import org.vortikal.util.text.HtmlUtil;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;

/**
 * Controller that handles editing of plaintext resource content.
 *
 * <h3>A note on character encoding problems:</h3>
 *
 * If the resource is XML/HTML, there are some considerations: First,
 * the resource may have an encoding set in the repository (either
 * explicitly set using the 'contentType' property or inside the
 * document itself). This is called the 'stored encoding'. In addition
 * to that, there is the content that is posted from the form. This
 * content is assumed to be posted using UTF-8, producing a valid
 * String inside the Java code. The problem arises when the posted
 * content contains a charset declaration and that declaration differs
 * from the stored encoding. In such cases, the stored encoding should
 * be altered to reflect that of the posted content (in a best-effort
 * fashion).
 *
 * <p>Configurable JavaBean properties
 * (and those defined by {@link SimpleFormController superclass}):
 * <ul>
 *   <li><code>repository</code> - the content {@link Repository
 *   repository} (required)
 *   <li><code>cancelView</code> - the {@link String view name} to return
 *   when user (required) cancels the operation
 *   <li><code>lockTimeoutSeconds</code> - the number of seconds for
 *   which to request lock timeouts on every request (default is 300)
 *  <li><code>defaultCharacterEncoding</code> - defaults to
 *  <code>utf-8</code>, which encoding to enterpret the supplied
 *  resource content in, if unable to guess.
 *  <li><code>storeModifiedCharacterEncodings</code> - whether or not
 *  to also change the resource's <code>characterEncoding</code>
 *  property when modified in the XML declaration or in a HTML meta
 *  tag. Default is <code>false</code>.
 * </ul>
 */
public class PlaintextEditController extends SimpleFormController
  implements InitializingBean {

    private static final int MAX_XML_DECLARATION_SIZE = 500;

    private static Log logger = LogFactory.getLog(
        PlaintextEditController.class);
    
    private String cancelView;
    private Repository repository;
    private int lockTimeoutSeconds = 300;
    private String defaultCharacterEncoding = "utf-8";
    private boolean storeModifiedCharacterEncodings = false;
    

    /**
     * Sets the repository.
     */
    public void setRepository(Repository repository) {
        this.repository = repository;
    }


    /**
     * Sets the requested number of seconds for lock timeout.
     */
    public void setLockTimeoutMinutes(int lockTimeoutSeconds) {
        this.lockTimeoutSeconds = lockTimeoutSeconds;
    }
    

    /**
     * Sets the cancel view name (will be returned when user cancelles
     * the edit operation).
     */
    public void setCancelView(String cancelView) {
        this.cancelView = cancelView;
    }
    

    /**
     * Sets the default character encoding.
     */
    public void setDefaultCharacterEncoding(String defaultCharacterEncoding) {
        this.defaultCharacterEncoding = defaultCharacterEncoding;
    }


    /**
     * Sets whether to store modified character encodings on resources
     * when altered in markup meta headers.
     */
    public void setStoreModifiedCharacterEncodings(
        boolean storeModifiedCharacterEncodings) {
        this.storeModifiedCharacterEncodings = storeModifiedCharacterEncodings;
    }
    

    public void afterPropertiesSet() {
        if (this.repository == null) {
            throw new BeanInitializationException(
                "Bean property 'repository' must be set");
        }
        if (this.cancelView == null) {
            throw new BeanInitializationException(
                "Bean property 'cancelView' must be set");
        }

    }
    

    protected Object formBackingObject(HttpServletRequest request)
        throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        Service service = requestContext.getService();
        
        String uri = requestContext.getResourceURI();
        String token = securityContext.getToken();
        Principal principal = securityContext.getPrincipal();
        
        String type = Lock.LOCKTYPE_EXCLUSIVE_WRITE;
        repository.lock(token, uri, type, principal.getQualifiedName(), "0",
                        this.lockTimeoutSeconds, null);

        Resource resource = repository.retrieve(token, uri, false);

        String url = service.constructLink(resource, principal);
        String content = getResourceContent(resource, token);
        
        PlaintextEditCommand command =
            new PlaintextEditCommand(content, url);

        if (ContentTypeHelper.isHTMLContentType(resource.getContentType())) {
            command.setHtml(true);
        }
        
        return command;
    }


    protected ModelAndView onSubmit(Object command, BindException errors)
        throws Exception {

        PlaintextEditCommand plaintextEditCommand =
            (PlaintextEditCommand) command;

        if (plaintextEditCommand.getCancelAction() == null) {
            return super.onSubmit(command, errors);
        }
        
        /** The user has selected "cancel". Unlock resource, return
         *  the cancelView. */

        SecurityContext securityContext = SecurityContext.getSecurityContext();
        String token = securityContext.getToken();
        RequestContext requestContext = RequestContext.getRequestContext();
        String uri = requestContext.getResourceURI();
        repository.unlock(token, uri, null);
        
        return new ModelAndView(this.cancelView);    
    }
    

    protected void doSubmitAction(Object command) throws Exception {        
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        String uri = requestContext.getResourceURI();
        String token = securityContext.getToken();

        PlaintextEditCommand plaintextEditCommand =
            (PlaintextEditCommand) command;

        Resource resource = repository.retrieve(token, uri, false);

        String storedEncoding = resource.getCharacterEncoding();
        if (storedEncoding == null) {

            if (ContentTypeHelper.isXMLContentType(resource.getContentType())) {
                storedEncoding = getXMLCharacterEncoding(resource, token);

            } else if (ContentTypeHelper.isHTMLContentType(resource.getContentType())) {
                storedEncoding = getHTMLCharacterEncoding(resource, token);
            }
        }

        String postedEncoding = null;
        if (ContentTypeHelper.isXMLContentType(resource.getContentType())) {

            postedEncoding = getXMLCharacterEncoding(
                plaintextEditCommand.getContent());
            
        } else if (ContentTypeHelper.isHTMLContentType(resource.getContentType())) {

            postedEncoding = HtmlUtil.getCharacterEncodingFromBody(
                plaintextEditCommand.getContent().getBytes());
        } 
        
        
        try {
            if (storedEncoding != null) Charset.forName(storedEncoding);
        } catch (Exception e) {
            if (logger.isDebugEnabled())
                logger.debug(
                    "Invalid character encoding '" + storedEncoding);
                    
            storedEncoding = null;
        }

        try {
            if (postedEncoding != null) Charset.forName(postedEncoding);
        } catch (Exception e) {
            if (logger.isDebugEnabled())
                logger.debug(
                    "Invalid character encoding '" + postedEncoding);
            postedEncoding = null;
        }

        
        /** 
         * When storing content, it has to be written as a byte
         * sequence, produced from the posted content using an
         * encoding decided by the following:
         * 
         * 1. storedEncoding == null, postedEncoding == null --> use defaultEncoding
         * 2. storedEncoding == null, postedEncoding != null --> use postedEncoding
         * 3. storedEncoding != null, postedEncoding == null --> keep storedEncoding
         * 4. storedEncoding != null, postedEncoding != null --> use postedEncoding
         */
        String characterEncoding = this.defaultCharacterEncoding;
        boolean storeEncoding = false;

        if (storedEncoding == null && postedEncoding != null) {
            characterEncoding = postedEncoding;
            storeEncoding = true;

        } else if (storedEncoding != null && postedEncoding == null) {
            characterEncoding = storedEncoding;

        } else if (storedEncoding != null && postedEncoding != null) {
            characterEncoding = postedEncoding;
            storeEncoding = true;
        }

        if (!this.storeModifiedCharacterEncodings) {
            storeEncoding = false;
        }

        if (storeEncoding) {
            if (logger.isDebugEnabled())
                logger.debug("New character encoding for document "
                             + resource + " resolved to: " + characterEncoding);
            resource.setCharacterEncoding(characterEncoding);
            repository.store(token, resource);
        }

        String content = plaintextEditCommand.getContent();

        repository.storeContent(token, uri, 
                new ByteArrayInputStream(content.getBytes(characterEncoding)));
    }
    



    private String getResourceContent(Resource resource, String token)
        throws IOException {

        /**
         * if character encoding is set on the resource, just read it
         * as a plain stream using that encoding, regardless of
         * whether it is an XML resource. Otherwise, let the XML
         * parser handle the job */

        if (resource.getCharacterEncoding() != null) {
            return getPlainTextContent(resource, token);
        }

        if (ContentTypeHelper.isXMLContentType(resource.getContentType())) {
            return getXMLContent(resource, token);

        } else if (ContentTypeHelper.isHTMLContentType(resource.getContentType())) {
            return getHTMLContent(resource, token);
        }

        return getPlainTextContent(resource, token);
    }
    

    private String getXMLContent(Resource resource, String token)
        throws IOException {
        SAXBuilder builder = new SAXBuilder();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int n = 0;
        byte[] buf = new byte[1024];
        InputStream in = repository.getInputStream(token, resource.getURI(), false);
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
        }
        in.close();

        try {
            
            Document doc = builder.build(new ByteArrayInputStream(out.toByteArray()));

            Format format = Format.getRawFormat();
            if (resource.getCharacterEncoding() != null) {
                format.setEncoding(resource.getCharacterEncoding());
            }

            XMLOutputter xmlOutputter = new XMLOutputter(format);
            String xml = xmlOutputter.outputString(doc);
            return xml;
        
        } catch (JDOMException e) {

            /**
             * Parsing the XML content did not work, so return the
             * content converted to a string in a "best-effort"
             * fashion: */
            String characterEncoding = "utf-8";
            if (resource.getCharacterEncoding() != null) {
                characterEncoding = resource.getCharacterEncoding();
            }

            if (logger.isDebugEnabled()) {
                logger.debug(
                    "Unable to build DOM tree for resource " 
                    + resource + ": " + e.getMessage() + ", converting "
                    + "byte stream to string using character encoding "
                    + characterEncoding);
            }
            return new String(out.toByteArray(), characterEncoding);
        }
    }
    

    private String getHTMLContent(Resource resource, String token)
        throws IOException {
        InputStream is = repository.getInputStream(
            token, resource.getURI(), false);
        byte[] bytes = StreamUtil.readInputStream(is);
        String encoding = resource.getCharacterEncoding();
        if (encoding == null) encoding = HtmlUtil.getCharacterEncodingFromBody(bytes);
        if (encoding == null) encoding = this.defaultCharacterEncoding;
        String content = new String(bytes, encoding);
        return content;
    }
    


    private String getPlainTextContent(Resource resource, String token)
        throws IOException {

        InputStream is = repository.getInputStream(token, resource.getURI(),
                                                   false);
        byte[] bytes = StreamUtil.readInputStream(is);
        String encoding = resource.getCharacterEncoding();
        if (encoding == null) encoding = this.defaultCharacterEncoding;
        String content = new String(bytes, encoding);
        return content;
    }




    private String getXMLCharacterEncoding(String xmlContent) {
        // FIXME: more accurate regexp:
        Pattern charsetPattern = Pattern.compile(
            "^\\s*<\\?xml.*\\s+encoding=[\"']"
            + "([A-Za-z0-9._\\-]+)[\"'][^>]*\\?>.*",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

        String characterEncoding = this.defaultCharacterEncoding;
      
        Matcher m = charsetPattern.matcher(xmlContent);

        if (!m.matches()) {
            if (logger.isDebugEnabled())
                logger.debug("No regexp match in XML declaration for pattern "
                             + charsetPattern.pattern());
            return this.defaultCharacterEncoding;
        }
            
        if (logger.isDebugEnabled())
            logger.debug("Regexp match in XML declaration for pattern "
                         + charsetPattern.pattern());
        characterEncoding = m.group(1);

        try {
            Charset.forName(characterEncoding);
        } catch (Exception e) {
            if (logger.isDebugEnabled())
                logger.debug(
                    "Invalid character encoding '" + characterEncoding
                    + "' for XML document <string>, using utf-8");
            return this.defaultCharacterEncoding;
        }
                        
        return characterEncoding;

    }


    private String getXMLCharacterEncoding(Resource resource, String token)
        throws IOException {

        int len = MAX_XML_DECLARATION_SIZE;
        BufferedReader reader = null;
        InputStream inStream = null;
        String characterEncoding = this.defaultCharacterEncoding;
      
        try {
            if (logger.isDebugEnabled())
                logger.debug("Opening document " + resource.getURI());
            inStream = repository.getInputStream(
                token, resource.getURI(), false);
            
            reader = new BufferedReader(new InputStreamReader(
                                            inStream, "utf-8"));

            char[] chars = new char[len];
            int read = reader.read(chars, 0, len);
            
            // resource didn't have content;
            if (read == -1)
                return this.defaultCharacterEncoding;
                
            String string = new String(chars, 0, read);
            return getXMLCharacterEncoding(string);
                        
        } catch (IOException e) {
            logger.warn("Unexpected IO exception while performing "
                        + "XML charset regexp matching on resource "
                        + resource, e);
            return this.defaultCharacterEncoding;
        }
    }


    private String getHTMLCharacterEncoding(Resource resource, String token) {
        if (!ContentTypeHelper.isHTMLContentType(resource.getContentType())) {
            return null;
        }

        int len = MAX_XML_DECLARATION_SIZE;
        InputStream inStream = null;
      
        try {
            if (logger.isDebugEnabled())
                logger.debug("Opening document " + resource.getURI());
            inStream = repository.getInputStream(
                token, resource.getURI(), false);
            
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            int n = 0, total = 0;
            byte[] buffer = new byte[1024];
            while (total <= len && (n = inStream.read(buffer, 0, 1024)) > 0) {
                total += n;
                out.write(buffer, 0, n);
            }

            String storedEncoding = HtmlUtil.getCharacterEncodingFromBody(
                out.toByteArray());
            return storedEncoding;
        
        } catch (IOException e) {
            logger.warn("Unexpected IO exception while finding "
                        + "HTML character encoding on resource "
                        + resource, e);
            return null;
        }
    }
    



}

