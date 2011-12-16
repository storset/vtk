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

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.view.AbstractView;
import org.vortikal.web.InvalidModelException;
import org.vortikal.webdav.WebdavConstants;

/**
 * Simple HTTP Status code view.
 * 
 * Expects the model to contain a property 
 * <code>WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE</code> containing an 
 * <code>Integer</code> object with the desired status code.
 * 
 * FIXME: some WebDAV status codes are not recognized by servlet container, and
 * so the message "Internal Server Error" is appended. An example of this is
 * "423 Locked", which is returned to clients as "423 Internal Server Error".
 * The method HttpServletResponse.setStatus(int sc, String msg) is deprecated, 
 * and the API suggests using HttpServletResponse.sendError(int sc, String msg) 
 * instead. A solution might be to check if status code is >= 400, and if so, then
 * use HttpServletResponse.sendError(sc, "Our WebDAV status message from HttpUtil").
 *
 */
public class SimpleHttpStatusView extends AbstractView {
    
    private String body;

    @SuppressWarnings("rawtypes")
    protected void renderMergedOutputModel(Map model, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        Integer status = (Integer) model.get(
            WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE);
        if (status == null) {
            throw new InvalidModelException(
                "No status code set in model. Expected an integer with key " +
                "`" + WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE + "'");
        }
        
        response.setStatus(status.intValue());
        String etag = (String) model.get(WebdavConstants.WEBDAVMODEL_ETAG);
        if (etag != null) {
            response.setHeader(WebdavConstants.WEBDAVMODEL_ETAG, etag);
        }
        if (this.body != null) {
            response.setContentType(getContentType());
            PrintWriter writer = response.getWriter();
            writer.write(this.body);
            writer.flush();
            writer.close();
        }
    }

    public void setBody(String body) {
        this.body = body;
    }
}
