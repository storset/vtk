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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.vortikal.repository.FailedDependencyException;
import org.vortikal.repository.IllegalOperationException;
import org.vortikal.repository.Path;
import org.vortikal.repository.ReadOnlyException;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceLockedException;
import org.vortikal.repository.ResourceNotFoundException;
import org.vortikal.repository.ResourceOverwriteException;
import org.vortikal.util.web.HttpUtil;
import org.vortikal.web.InvalidRequestException;
import org.vortikal.web.RequestContext;
import org.vortikal.webdav.ifheader.IfHeaderImpl;

/**
 * Handler for MOVE requests
 *
 */
public class MoveController extends AbstractWebdavController {
    
    private String trustedToken = null;


	/**
     * Performs the WebDAV 'MOVE' method.      
     *
     * @param request the HTTP servlet request
     * @param response the HTTP servlet response
     * @return a <code>ModelAndView</code> containing the HTTP status code.
     */
    public ModelAndView handleRequest(HttpServletRequest request,
                                      HttpServletResponse response) throws Exception {

        RequestContext requestContext = RequestContext.getRequestContext();
        Repository repository = requestContext.getRepository();
        String token = requestContext.getSecurityToken();
        Path uri = requestContext.getResourceURI();
        String destHeader = request.getHeader("Destination");
        Map<String, Object> model = new HashMap<String, Object>();

        try {
            Resource resource = repository.retrieve(trustedToken, uri, false);
            this.ifHeader = new IfHeaderImpl(request);
            verifyIfHeader(resource, true);
            
            if (destHeader == null || destHeader.trim().equals("")) {
                throw new InvalidRequestException(
                    "Missing `Destination' request header");
            }
            Path destURI = mapToResourceURI(destHeader);
            String depth = request.getHeader("Depth");
            if (depth == null) {
                depth = "infinity";
            }
         
            boolean overwrite = true;
            String overwriteHeader = request.getHeader("Overwrite");
            if (overwriteHeader != null && overwriteHeader.equals("F")) {
                overwrite = false;
            } 

            boolean existed = repository.exists(token, destURI);
            
            if (existed) {
                Resource destination = repository.retrieve(token, destURI, false);
                verifyIfHeader(destination, true);
            }
            repository.move(token, uri, destURI, overwrite);

            if (existed) {
                model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                          new Integer(HttpServletResponse.SC_NO_CONTENT));
            } else {
                model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                          new Integer(HttpServletResponse.SC_CREATED));
            }

            return new ModelAndView("MOVE", model);

        } catch (InvalidRequestException e) {
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpServletResponse.SC_BAD_REQUEST));

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
                      new Integer(HttpServletResponse.SC_PRECONDITION_FAILED));

        } catch (ResourceOverwriteException e) {
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpServletResponse.SC_PRECONDITION_FAILED));

        } catch (ResourceLockedException e) {
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpUtil.SC_LOCKED));

        } catch (ResourceNotFoundException e) {
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpServletResponse.SC_NOT_FOUND));
        }

        return new ModelAndView("MOVE", model);
    }
   
    public void setTrustedToken(String trustedToken) {
        this.trustedToken = trustedToken;
    }

}
