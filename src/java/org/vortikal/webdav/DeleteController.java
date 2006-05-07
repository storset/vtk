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
import org.vortikal.repository.FailedDependencyException;
import org.vortikal.repository.IllegalOperationException;
import org.vortikal.repository.ReadOnlyException;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceLockedException;
import org.vortikal.repository.ResourceNotFoundException;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.web.HttpUtil;
import org.vortikal.web.RequestContext;
import org.vortikal.webdav.ifheader.IfHeaderImpl;


/**
 * Handler for DELETE requests
 *
 */
public class DeleteController extends AbstractWebdavController {


    /**
     * Performs the WebDAV 'DELETE' method.
     *
     * @param request the HTTP request object
     * @param response the <code>HttpServletResponse</code> response object
     */
    public ModelAndView handleRequest(HttpServletRequest request,
                                      HttpServletResponse response) {

        
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        String token = securityContext.getToken();
        RequestContext requestContext = RequestContext.getRequestContext();
        String uri = requestContext.getResourceURI();
        Map model = new HashMap();
        try {
            ifHeader = new IfHeaderImpl(request);
            Resource resource = repository.retrieve(token, uri, false);
            
            logger.debug("resource.getLock(): " + resource.getLock());
            logger.debug("ifHeader.hasTokens(): " + ifHeader.hasTokens());
            if (!ignoreIfHeader && resource.getLock() != null && !ifHeader.hasTokens()) {
                logger.debug("resource locked and if-header hasn't any locktokens");
                throw new ResourceLockedException();
            }
            
            if (!matchesIfHeader(resource, true)) {
                logger.debug("handleRequest: matchesIfHeader false");
                throw new ResourceLockedException();
            } else {
                logger.debug("handleRequest: matchesIfHeader true");
            }
            

            
            if (logger.isDebugEnabled()) {
                logger.debug("Attempting to delete resource " + uri);
            }

            // Delete the document or collection:
            repository.delete(token, uri);

            if (logger.isDebugEnabled()) {
                logger.debug("Resource " + uri + " deleted");
            }

            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpServletResponse.SC_OK));
            return new ModelAndView("DELETE", model);
         
        } catch (ResourceNotFoundException e) {
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpServletResponse.SC_NOT_FOUND));

        } catch (ResourceLockedException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Caught ResourceLockedException for URI "
                             + uri);
            }
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpUtil.SC_LOCKED));

        } catch (IllegalOperationException e) {
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpServletResponse.SC_FORBIDDEN));

        } catch (ReadOnlyException e) {
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpServletResponse.SC_FORBIDDEN));

        } catch (FailedDependencyException e) {
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpUtil.SC_MULTI_STATUS));

        } catch (IOException e) {
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
        }

        return new ModelAndView("DELETE", model);
    }
   
   
}
