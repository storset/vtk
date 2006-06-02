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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceLockedException;
import org.vortikal.repository.ResourceNotFoundException;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.web.HttpUtil;
import org.vortikal.web.RequestContext;
import org.vortikal.webdav.ifheader.IfMatchHeader;
import org.vortikal.webdav.ifheader.IfNoneMatchHeader;



/**
 * Handler for HEAD requests
 *
 */
public class HeadController extends AbstractWebdavController {


    /**
     * Performs the HTTP/WebDAV 'HEAD' method.
     *
     */
    public ModelAndView handleRequest(HttpServletRequest request,
                                      HttpServletResponse response) {
         
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        String token = securityContext.getToken();
        RequestContext requestContext = RequestContext.getRequestContext();
        String uri = requestContext.getResourceURI();
        Map model = new HashMap();

        try {
            
            Resource resource =
                repository.retrieve(token, uri, false);

            if (resource.isCollection()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("HEAD on collection: setting status 404");
                }
                throw new ResourceNotFoundException(uri);
            }
            
            IfMatchHeader ifMatchHeader = new IfMatchHeader(request);
            if (!ifMatchHeader.matches(resource)) {
                throw new PreconditionFailedException();
            }
                
            IfNoneMatchHeader ifNoneMatchHeader = new IfNoneMatchHeader(request);
            if (!ifNoneMatchHeader.matches(resource)) {
                throw new PreconditionFailedException();
            }
            
            model.put(WebdavConstants.WEBDAVMODEL_REQUESTED_RESOURCE, resource);
            return new ModelAndView("HEAD", model);
            
        } catch (ResourceNotFoundException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Caught ResourceNotFoundException for URI "
                             + uri);
            }

            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpServletResponse.SC_NOT_FOUND));
            return new ModelAndView("HTTP_STATUS_VIEW", model);

        } catch (ResourceLockedException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Caught ResourceLockedException for URI "
                             + uri);
            }
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpUtil.SC_LOCKED));
            return new ModelAndView("HTTP_STATUS_VIEW", model);

        } catch (PreconditionFailedException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Caught PreconditionFailedException for URI " + uri);
            }
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpServletResponse.SC_PRECONDITION_FAILED));
            return new ModelAndView("HTTP_STATUS_VIEW", model);
            
        } catch (IOException e) {
            logger.info("Caught IOException for URI " + uri);
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
            return new ModelAndView("HTTP_STATUS_VIEW", model);
        }

    }
    
}
