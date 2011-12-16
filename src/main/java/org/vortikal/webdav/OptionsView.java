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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.View;

/**
 * View for OPTIONS requests.
 */
public class OptionsView implements View {

    @SuppressWarnings("rawtypes")
    public void render(Map model, HttpServletRequest request,
                       HttpServletResponse response) throws Exception {

        response.setStatus(HttpServletResponse.SC_OK);
        StringBuffer optionsHeader = new StringBuffer();
        optionsHeader.append("1, 2");
        response.setHeader("DAV", optionsHeader.toString());
      
        StringBuffer allowHeader = new StringBuffer();
        allowHeader.append("GET, HEAD, POST, PUT, DELETE, CONNECT, OPTIONS, ");
        allowHeader.append("PATCH, PROPFIND, PROPPATCH, MKCOL, COPY, MOVE, ");
        allowHeader.append("LOCK, UNLOCK, TRACE");
        response.setHeader("Allow", allowHeader.toString());
        response.setHeader("MS-Author-Via", "DAV");
        
        String etag = (String) model.get(WebdavConstants.WEBDAVMODEL_ETAG);
        if (etag != null) {
            response.setHeader(WebdavConstants.WEBDAVMODEL_ETAG, etag);
        }
    }
    
    public String getContentType() {
        return null;
    }


}
