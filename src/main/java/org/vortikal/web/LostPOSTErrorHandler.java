/* Copyright (c) 2010, University of Oslo, Norway
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

package org.vortikal.web;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.security.SecurityContext;
import org.vortikal.security.UnsupportedRequestMethodAPE;

/**
 * VTK-1896
 *
 * Not a subclass of DefaultErrorHandler, since that seems to require a Spring
 * web application context, and we don't have that luxury when an authentication
 * exception occurs in VortikalServlet (because the normal Spring service dispatch isn't called).
 * So handle everything on our own here, writing directly to the response..
 */
public final class LostPOSTErrorHandler {

    private Log logger = LogFactory.getLog(LostPOSTErrorHandler.class);

    public void handleLostPOSTError(HttpServletRequest request, HttpServletResponse response,
                UnsupportedRequestMethodAPE urmape)
        throws ServletException {

        try {
            String incidentId = UUID.randomUUID().toString();
            logPOST(request, incidentId, urmape);

            String errorPageHTML = getErrorHTMLPage(request, incidentId);
            response.setContentType("text/html; charset=utf-8");
            response.setStatus(483);
            response.getOutputStream().write(errorPageHTML.getBytes("UTF-8"));
            response.getOutputStream().close();
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private void logPOST(HttpServletRequest request, String incidentId, Throwable error) {
        this.logger.info(getErrorLogEntry(request, incidentId, error));
    }

    private String getErrorLogEntry(HttpServletRequest req, String incidentId, Throwable error) {
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        String httpMethod = req.getMethod();

        Map requestParameters = req.getParameterMap();
        StringBuilder params = new StringBuilder("{");
        for (Iterator iter = requestParameters.keySet().iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            params.append(name).append("=[");
            String[] values = req.getParameterValues(name);
            for (int i = 0; i < values.length; i++) {
                params.append(values[i]);
                if (i < values.length - 1) {
                    params.append(",");
                }
            }
            params.append("]");
            if (iter.hasNext()) {
                params.append(",");
            }
        }
        params.append("}");

        StringBuffer requestURL = req.getRequestURL();
        String queryString = req.getQueryString();
        if (queryString != null) {
            requestURL.append("?").append(queryString);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Incident-ID: ").append(incidentId).append(" - ");
        sb.append("Message: ").append(error.getMessage()).append(" - ");
        sb.append("Full request URL: [").append(requestURL).append("], ");
        sb.append("Request context: [").append(requestContext).append("], ");
        sb.append("security context: [").append(securityContext).append("], ");
        sb.append("method: [").append(httpMethod).append("], ");
        sb.append("request parameters: [").append(params).append("], ");
        sb.append("user agent: [").append(req.getHeader("User-Agent")).append("], ");
        sb.append("host: [").append(req.getServerName()).append("], ");
        sb.append("remote host: [").append(req.getRemoteHost()).append("]");
        return sb.toString();
    }

    private String getErrorHTMLPage(HttpServletRequest request, String incidentId) {
        StringBuffer requestURL = request.getRequestURL();
        String queryString = request.getQueryString();
        if (queryString != null) {
            queryString = queryString.replaceAll("&", "&amp;");
            queryString = queryString.replaceAll("\"", "&quot;");
            queryString = queryString.replaceAll("<", "&lt;");
            requestURL.append("?").append(queryString);
        }

        // Just hard-code, won't bother with any private Freemarker setup for this particular case
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n"
                + "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n"
                + "<head>\n"
                + "  <title>483 - Unable to process POST request due to invalid or expired session</title>\n"
                + "</head>\n"
                + "<body>\n"
                + "\n"
                + "<h1>483 - Unable to process POST request due to invalid or expired session</h1>\n"
                + "\n"
                + "<p>A problem has occured while processing your posted data. Your login session\n"
                + "was not found or was invalid.</p>"
                + "<p>This can typically happen in the following situations:</p>\n"
                + "<ul>\n"
                + "  <li>Logging out in a different browser window/tab while editing a document.</li>\n"
                + "  <li>Switching internet connection while editing a document.</li>\n"
                + "  <li>Leaving the editor open without any activity for a long time.</li>\n"
                + "</ul>\n"
                + "\n"
                + "<p><em>If you had unsaved changes in your document, please contact: <a href=\"mailto:vortex-hjelp@usit.uio.no\">vortex-hjelp@usit.uio.no</a><br/>\n"
                + "and include the following incident ID in your email: <strong>" + incidentId + "</strong><br/>\n"
                + "We might be able to help you get it back.</em></p>\n"
                + "<p>Work is being done to avoid this from happening and improve the situation when it does.<br/>\n"
                + "In the mean time, please try to avoid the situations described above.</p>\n"
                + "<p><a href=\"" + requestURL + "\">Click here</a> to go back (you will get a new login session).</p>\n"
                + "</body>\n"
                + "</html>\n";
    }

}
