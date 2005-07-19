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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.web.servlet.view.AbstractView;

import org.vortikal.repository.Property;
import org.vortikal.repository.Resource;
import org.vortikal.util.repository.ContentTypeHelper;
import org.vortikal.util.web.HttpUtil;
import org.vortikal.web.InvalidModelException;
import org.vortikal.web.referencedata.ReferenceDataProvider;
import org.vortikal.web.referencedata.ReferenceDataProviding;


/**
 * "Web server" behaving view. Writes the contents of a
 * resource to the client.
 *
 * <p><a name="config">Configurable properties</a>
 * (and those defined by {@link AbstractView superclass}):
 * <ul>
 *   <li><code>includeLastModifiedHeader</code> - boolean deciding
 *   whether to set the <code>Last-Modified</code> response
 *   header. Default value is <code>true</code>.
 *   <li><code>includeExpiresHeader</code> - boolean deciding whether
 *   to attempt to set the <code>Expires</code> HTTP header
 *   <li><code>includeContentLanguageHeader</code> - whether or not to
 *   to attempt to set the <code>Content-Language</code> HTTP header
 *   to that of the resource (default <code>true</code>.)
 *   <li><code>streamBufferSize</code> - (int) the size of the buffer
 *   used when executing the (read from resource, write to response)
 *   loop. The default value is <code>5000</code>.
 * </ul>
 *
 * <p>Requires the following data to be present in the model:
 * <ul>
 *   <li><code>resource</code> - the {@link Resource} object requested
 *   <li><code>resourceStream</code> - the content {@link InputStream} to write to the
 *   client 
 * </ul>
 * 
 * <p>Sets the following HTTP headers, based on metadata in the
 * resource:
 * <ul>
 *   <li><code>Content-Type</code>
 *   <li><code>Content-Length</code>
 *   <li><code>Last-Modified</code> if the configuration property
 *   <code>includeLastModifiedHeader</code> is set to
 *   <code>true</code> (the default).
 *   <li><code>Expires</code> if the configuration property
 *   <code>includeExpiresHeader</code> is set to
 *   <code>true</code>, and the resource has a property
 *   <code>http://www.uio.no/vortex/custom-properties:expires-sec</code>
 *   set to an integer. The value of the header is the
 *   value of the property.
 *   <li><code>Cache-Control: no-cache</code> if the configuration
 *   property <code>includeExpiresHeader</code> is <code>false</code>,
 *   or it is set, but the <code>expires-sec</code> resource property
 *   (see above) is not set or is not an integer.
 *   <li><code>Content-Language</code> if the configuration property
 *   <code>includeContentLanguageHeader</code> is <code>true</code>
 *   and the resource has a content locale defined. (Note: a
 *   limitation in the Spring framework (<code>setLocale()</code> is
 *   always called on every response with the value of the resolved
 *   request locale) causes this view to always set this header. In
 *   cases where the resource has no content locale set, or this view
 *   is not configured to include the header, the value of the header
 *   is empty.
 * </ul>
 *
 */
public class DisplayResourceView extends AbstractView
  implements ReferenceDataProviding {

    private static Log logger = LogFactory.getLog(DisplayResourceView.class);
    
    private int streamBufferSize = 5000;

    private boolean includeLastModifiedHeader = true;
    private boolean includeExpiresHeader = true;
    private boolean includeContentLanguageHeader = true;
    private ReferenceDataProvider[] referenceDataProviders;
    
    public ReferenceDataProvider[] getReferenceDataProviders() {
        return referenceDataProviders;
    }

    public void setReferenceDataProviders(ReferenceDataProvider[] referenceDataProviders) {
        this.referenceDataProviders = referenceDataProviders;
    }


    public void setStreamBufferSize(int streamBufferSize) {
        if (streamBufferSize <= 0) {
            throw new IllegalArgumentException(
                "The value of streamBufferSize must be a positive integer");
        }
        this.streamBufferSize = streamBufferSize;
    }
    

    public void setIncludeLastModifiedHeader(boolean includeLastModifiedHeader) {
        this.includeLastModifiedHeader = includeLastModifiedHeader;
    }


    public void setIncludeExpiresHeader(boolean includeExpiresHeader) {
        this.includeExpiresHeader = includeExpiresHeader;
    }
    

    public void setIncludeContentLanguageHeader(boolean includeContentLanguageHeader) {
        this.includeContentLanguageHeader = includeContentLanguageHeader;
    }
    

    public void renderMergedOutputModel(Map model, HttpServletRequest request,
                       HttpServletResponse response) throws Exception {

        Resource resource = (Resource) model.get("resource");
        if (resource == null) {
            throw new InvalidModelException(
                "Missing resource in model " +
                "(expected a Resource object having key 'resource')");
        }

        InputStream inStream = (InputStream) model.get("resourceStream");
        if (inStream == null) {
            throw new InvalidModelException(
                "Missing stream in model " +
                "(expected an InputStream object having key 'resourceStream')");
        }

        String contentType = resource.getContentType();
        
        if (ContentTypeHelper.isHTMLContentType(resource.getContentType()) &&
            resource.getCharacterEncoding() == null) {
            // FIXME: to prevent some servlet containers (resin) from
            // trying to be "smart" and append "charset=iso-8859-1" to
            // the Content-Type header when no character encoding has
            // been specified. According to RFC 2616, sec. 4.2,
            // preceding the header value with arbitrary amount of LWS
            // is perfectly legal, although a single space is
            // preferred.
            contentType = " " + resource.getContentType();
        } else if (ContentTypeHelper.isTextContentType(resource.getContentType())
                   && resource.getCharacterEncoding() != null) {
            contentType = resource.getContentType() + ";charset="
                + resource.getCharacterEncoding();
        }
        
        if (logger.isDebugEnabled()) {
            logger.debug("Setting header Content-Type: " + contentType);
        }
        response.setHeader("Content-Type", contentType);

        // Fix for Spring's DispatcherServlet's behavior (always sets
        // the response's locale to that of the request).
        response.setHeader("Content-Language", "");

        if (this.includeContentLanguageHeader) {
            Locale locale = resource.getContentLocale();
            if (locale != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Setting header Content-Language: " + locale.getLanguage());
                }
                response.setHeader("Content-Language", locale.getLanguage());
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Setting header Content-Length: " + resource.getContentLength());
        }
        response.setHeader("Content-Length", String.valueOf(resource.getContentLength()));
        if (this.includeExpiresHeader) {
            
            Property expiresProperty = resource.getProperty(
                Property.LOCAL_NAMESPACE, 
                "expires-sec");
            if (expiresProperty != null && expiresProperty.getValue() != null) {

                try {
                    long expiresMilliseconds = new Long(
                        expiresProperty.getValue().trim()).longValue() * 1000;
                    Date expires = new Date(new Date().getTime() + expiresMilliseconds);
                    response.setHeader("Expires", HttpUtil.getHttpDateString(expires));

                    if (logger.isDebugEnabled()) {
                        logger.debug("Setting header Expires: " + 
                                     HttpUtil.getHttpDateString(expires));
                    }

                } catch (NumberFormatException e) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(
                            "Resource " + resource + "has malformed " +
                            "\"expires-sec\" property: " + expiresProperty.getValue()
                            + ". No Expires header set.");
                    }
                }
            }
        }
        
        if (this.includeLastModifiedHeader) {
            if (logger.isDebugEnabled()) {
                logger.debug("Setting header Last-Modified: "
                             + HttpUtil.getHttpDateString(resource.getLastModified()));
            }
            response.setHeader("Last-Modified", 
                               HttpUtil.getHttpDateString(resource.getLastModified()));
        }
        
        if (logger.isDebugEnabled()) {
            logger.debug("Setting HTTP status code: " + HttpServletResponse.SC_OK);
        }
        response.setStatus(HttpServletResponse.SC_OK);
  
        OutputStream out = null;
        int bytesWritten = 0;
        try {
            if ("HEAD".equals(request.getMethod())) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Request is HEAD, not writing content");
                }
                response.flushBuffer();
            } else {

                out = response.getOutputStream();
                byte[] buffer = new byte[this.streamBufferSize];
                int n = 0;
                while (((n = inStream.read(buffer, 0, this.streamBufferSize)) > 0)) {
                    out.write(buffer, 0, n);
                    bytesWritten += n;
                }
            }

        } finally {
            if (logger.isDebugEnabled()) {
                logger.debug("Wrote a total of " + bytesWritten
                             + " bytes to response");
            }

            if (out != null) {
                out.flush();
                out.close();
            }
            if (inStream != null) inStream.close();
        }
    }
    


}
