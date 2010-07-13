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
package org.vortikal.webdav;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.vortikal.repository.IllegalOperationException;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.ReadOnlyException;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceLockedException;
import org.vortikal.repository.ResourceNotFoundException;
import org.vortikal.repository.TypeInfo;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.web.HttpUtil;
import org.vortikal.web.RequestContext;
import org.vortikal.web.filter.UploadLimitInputStreamFilter;
import org.vortikal.webdav.ifheader.IfHeaderImpl;


/**
 * Handler for PUT requests.
 *
 * <p>Configurable JavaBean properties (in addition to those defined
 * by the {@link AbstractWebdavController superclass}):
 * <ul>
 *   <li><code>maxUploadSize</code> - optional long value specifying
 *   the maximum upload size in bytes. Default is <code>-1</code> (a
 *   negative number means no limit).
 *   <li><code>viewName</code> - the view name (default is
 *   <code>PUT</code>).
 * </ul>
 *
 */
public class PutController extends AbstractWebdavController {

    //supportIfHeaders defaults to true since we are in the webdav package
    private boolean supportIfHeaders = true;
    private long maxUploadSize = -1;
    private String viewName = "PUT";
    private boolean obeyClientCharacterEncoding = true;
    private boolean removeUserSpecifiedCharacterEncoding = false;
    

    public void setMaxUploadSize(long maxUploadSize) {
        if (maxUploadSize == 0) {
            throw new IllegalArgumentException(
                "Invalid upload size: " + maxUploadSize
                + " (must be a number != 0)");
        }

        this.maxUploadSize = maxUploadSize;
    }
    

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }
    

    public void setSupportIfHeaders(boolean supportIfHeaders) {
        this.supportIfHeaders = supportIfHeaders;
    }

    public void setObeyClientCharacterEncoding(boolean obeyClientCharacterEncoding) {
        this.obeyClientCharacterEncoding = obeyClientCharacterEncoding;
    }
    
    public void setRemoveUserSpecifiedCharacterEncoding(boolean removeUserSpecifiedCharacterEncoding) {
        this.removeUserSpecifiedCharacterEncoding = removeUserSpecifiedCharacterEncoding;
    }
    

    public ModelAndView handleRequest(HttpServletRequest request,
                                      HttpServletResponse response) throws Exception {
         
        if (this.maxUploadSize > 0) {
            request = new UploadLimitInputStreamFilter(this.maxUploadSize).
                filterRequest(request);
        }

        SecurityContext securityContext = SecurityContext.getSecurityContext();
        String token = securityContext.getToken();
        RequestContext requestContext = RequestContext.getRequestContext();
        Path uri = requestContext.getResourceURI();

        Map<String, Object> model = new HashMap<String, Object>();

        try {

            /* Get the document or collection: */

            Resource resource = null;
            boolean exists = this.repository.exists(token, uri);

            if (exists) {
                this.logger.debug("Resource '" + uri + "' already exists");
                resource = this.repository.retrieve(token, uri, false);
                this.ifHeader = new IfHeaderImpl(request);

                if (this.supportIfHeaders) {
                    // XXX: Special compatibility cases and workarounds should be handled in a more elegant way. 
                    String userAgent = request.getHeader("User-Agent");
                    if (userAgent != null && userAgent.startsWith("Contribute")) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Contribute client detected, will not verify If-headers");
                        }
                        // Contribute does not send if-header if already locked, don't require it.
                        verifyIfHeader(resource, false);
                    } else {
                        verifyIfHeader(resource, true);
                    }
                }
                
                if (resource.isCollection()) {
                    if (this.logger.isDebugEnabled()) {
                        this.logger.debug("PUT to collection: CONFLICT");
                    }
                    throw new WebdavConflictException(
                        "Trying to PUT to collection resource '" + uri + "'");
                }
                InputStream inStream = request.getInputStream();
                this.repository.storeContent(token, resource.getURI(), inStream);

            } else {

                /* check for parent: */
                Path parentURI = uri.getParent();                

                if (!this.repository.exists(token, parentURI)) {
                    if (this.logger.isDebugEnabled()) {
                        this.logger.debug("Parent " + parentURI +
                                     " does not exist. CONFLICT.");
                    }
                    throw new WebdavConflictException(
                        "Trying to PUT to non-existing resource. PUT URI was `" +
                        uri + "', parent resource '" + parentURI + "' does not exist.");
                }
                if (!allowedResourceName(uri)) {
                    model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                              new Integer(HttpServletResponse.SC_OK));
                    throw new IllegalOperationException("Rejecting resource creation: '"
                                                        + uri + "'");
                }

                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Resource does not exist (creating)");
                }
                InputStream inStream = request.getInputStream();
                resource = this.repository.createDocument(token, uri, inStream);
            }

            resource = this.repository.retrieve(token, resource.getURI(), false);
            TypeInfo typeInfo = this.repository.getTypeInfo(resource);
            
            boolean store = false;
            
            // XXX: userSpecifiedCharacterEncoding is a separate issue
            if (this.obeyClientCharacterEncoding) {
                String characterEncoding = request.getCharacterEncoding();
                if (characterEncoding != null) {
                    if (this.logger.isDebugEnabled()) {
                        this.logger.debug("Setting character encoding: " + characterEncoding);
                    }
                    Property prop = typeInfo.createProperty(
                            Namespace.DEFAULT_NAMESPACE, 
                            PropertyType.CHARACTERENCODING_USER_SPECIFIED_PROP_NAME);
                    prop.setStringValue(characterEncoding);
                    resource.addProperty(prop);
                    store = true;
                }
            } else if (this.removeUserSpecifiedCharacterEncoding) {
                resource.removeProperty(
                        Namespace.DEFAULT_NAMESPACE, 
                        PropertyType.CHARACTERENCODING_USER_SPECIFIED_PROP_NAME);
                store = true;
            }

            if (store) {
                resource = this.repository.store(token, resource);
            }

            if (exists) {
                model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                          new Integer(HttpServletResponse.SC_OK));
            } else {
                model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                          new Integer(HttpServletResponse.SC_CREATED));
            }

            model.put(WebdavConstants.WEBDAVMODEL_ETAG, resource.getEtag());
            return new ModelAndView(this.viewName, model);

        } catch (ResourceNotFoundException e) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Caught ResourceNotFoundException for URI " + uri);
            }
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpServletResponse.SC_NOT_FOUND));

        } catch (ResourceLockedException e) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Caught ResourceLockedException for URI " + uri);
            }
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpUtil.SC_LOCKED));
            
        } catch (IllegalOperationException e) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Caught IllegalOperationException for URI " + uri, e);
            }
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpServletResponse.SC_FORBIDDEN));

        } catch (WebdavConflictException e) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Caught WebdavConflictException for URI " + uri, e);
            }
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpServletResponse.SC_CONFLICT));

        } catch (ReadOnlyException e) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Caught ReadOnlyException for URI " + uri);
            }
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpServletResponse.SC_FORBIDDEN));

        }
        return new ModelAndView("PUT", model);
    }
}
