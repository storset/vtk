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
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.vortikal.repository.Resource;
import org.vortikal.util.web.HttpUtil;
import org.vortikal.web.InvalidModelException;





/**
 * "Web server" behaving view. Writes the contents of a
 * resource to the client.
 *
 * <p>Requires the following data to be present in the model:
 * <ul>
 *   <li>resource - the resource object being requested (of class
 *   org.vortikal.repository.Resource)
 *   <li>resourceStream - the content input stream to write to the
 *   client (java.io.InputStream)
 * </ul>
 * 
 * <p>Sets the following HTTP headers, based on metadata in the
 * resource:
 * <ul>
 *   <li>Content-Type
 *   <li>Content-Length
 *   <li>Last-Modified
 * </ul>
 *
 */
public class DisplayResourceView extends AbstractReferenceDataProvidingView {



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

        if ("text/html".equals(resource.getContentType()) &&
            resource.getCharacterEncoding() == null) {
            // FIXME: to prevent some servlet containers (resin) from
            // trying to be "smart" and append "charset=iso-8859-1" to
            // the Content-Type header when no character encoding has
            // been specified. According to RFC 2616, sec. 4.2,
            // preceding the header value with amount of LWS is
            // perfectly legal, although a single space is preferred.
            response.setHeader("Content-Type", " " + resource.getContentType());
        } else if (resource.getContentType().startsWith("text/") &&
                   resource.getCharacterEncoding() != null) {
            response.setHeader("Content-Type", resource.getContentType() +
                               ";charset=" + resource.getCharacterEncoding());
        } else {
            response.setHeader("Content-Type", resource.getContentType());
        }
        response.setHeader("Content-Length", String.valueOf(resource.getContentLength()));
        response.setHeader("Last-Modified", 
                HttpUtil.getHttpDateString(resource.getLastModified()));
        response.setStatus(HttpServletResponse.SC_OK);
  
        OutputStream out = null;
        try {
            out = response.getOutputStream();
            byte[] buffer = new byte[5000];
            int n = 0;
            while (((n = inStream.read(buffer, 0, 5000)) > 0)) {
                out.write(buffer, 0, n);
            }
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
            if (inStream != null) inStream.close();
        }
    }
    


}
