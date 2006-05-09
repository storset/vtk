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
import org.vortikal.repository.ResourceOverwriteException;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.web.HttpUtil;
import org.vortikal.util.web.URLUtil;
import org.vortikal.web.RequestContext;
import org.vortikal.webdav.ifheader.IfHeaderImpl;



/**
 * Handler for MOVE requests
 *
 */
public class MoveController extends AbstractWebdavController {



    /**
     * Performs the WebDAV 'MOVE' method.      
     *
     * @param request the HTTP servlet request
     * @param response the HTTP servlet response
     * @return a <code>ModelAndView</code> containing the HTTP status code.
     */
    public ModelAndView handleRequest(HttpServletRequest request,
                                      HttpServletResponse response) {

        SecurityContext securityContext = SecurityContext.getSecurityContext();
        String token = securityContext.getToken();
        RequestContext requestContext = RequestContext.getRequestContext();
        String uri = requestContext.getResourceURI();
        String destURI = request.getHeader("Destination");
        Map model = new HashMap();

        try {
            Resource resource = repository.retrieve(token, uri, false);
            ifHeader = new IfHeaderImpl(request);
            verifyIfHeader(resource, true);
            
            if (destURI == null || destURI.trim().equals("")) {
                throw new InvalidRequestException(
                    "Missing `Destination' request header");
            }
            

            destURI = mapToResourceURI(destURI);

            try {

                destURI = URLUtil.urlDecode(destURI, "utf-8");

            } catch (Exception e) {
                throw new InvalidRequestException(
                    "Error trying to URL decode uri" + destURI, e);
            }
            

            String depth = request.getHeader("Depth");
            if (depth == null) {
                depth = "infinity";
            }
         
            boolean overwrite = false;
            String overwriteHeader = request.getHeader("Overwrite");
            if (overwriteHeader != null && overwriteHeader.equals("T")) {
                overwrite = true;
            }

            boolean existed = repository.exists(token, destURI);

            if (logger.isDebugEnabled()) {
                logger.debug("Moving " + uri + " to " + destURI + ", depth = "
                             + depth + ", overwrite = " + overwrite
                             + ", existed = " + existed);
            }
            repository.move(token, uri, destURI, overwrite);

            if (logger.isDebugEnabled()) {
                logger.debug("Moving " + uri + " to " + destURI + " succeeded");
            }

            if (existed) {
                model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                          new Integer(HttpServletResponse.SC_NO_CONTENT));
            } else {
                model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                          new Integer(HttpServletResponse.SC_CREATED));
            }

            return new ModelAndView("MOVE", model);

        } catch (InvalidRequestException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Caught InvalidRequestException for URI "
                             + uri);
            }            
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpServletResponse.SC_BAD_REQUEST));

        } catch (IllegalOperationException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Caught IllegalOperationException for URI "
                             + uri);
            }
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpServletResponse.SC_FORBIDDEN));

        } catch (ReadOnlyException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Caught ReadOnlyException for URI "
                             + uri);
            }
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpServletResponse.SC_FORBIDDEN));

        } catch (FailedDependencyException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Caught FailedDependencyException for URI "
                             + uri);
            }
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpServletResponse.SC_PRECONDITION_FAILED));

        } catch (ResourceOverwriteException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Caught ResourceOverwriteException for URI "
                             + uri);
            }
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpServletResponse.SC_PRECONDITION_FAILED));

        } catch (ResourceLockedException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Caught ResourceLockedException for URI "
                             + uri);
            }
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpUtil.SC_LOCKED));

        } catch (ResourceNotFoundException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Caught ResourceNotFoundException for URI "
                             + uri);
            }
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpServletResponse.SC_NOT_FOUND));

        } catch (IOException e) {
            logger.info("Caught IOException for URI " + uri, e);
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
        }

        return new ModelAndView("MOVE", model);
    }
   
   
}
