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
package org.vortikal.web.servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.web.servlet.HttpServletBean;
import org.vortikal.util.web.URLUtil;


public class HostNameDelegatingServlet extends HttpServletBean {

    private Log logger = LogFactory.getLog(this.getClass());
    private Map hostMap = null;
    private ServletContext servletContext = null;


    public HostNameDelegatingServlet() {
        this.addRequiredProperty("hostMappings");
    }
    

    protected void initServletBean() throws ServletException {
        this.servletContext = getServletContext();
    }
    

    public void setHostMappings(String hostMappings) {
        this.hostMap = new HashMap();
        String[] mappings = hostMappings.split(",");
        for (int i = 0; i < mappings.length; i++) {
            if (mappings[i].indexOf("=") == -1) {
                throw new IllegalArgumentException(
                    "Each entry in the hostMappings string must be in the format "
                    + "'<hostname>=<servlet name>'");
            }
            String hostName = mappings[i].substring(0, mappings[i].indexOf("=")).trim();
            String servletName = mappings[i].substring(mappings[i].lastIndexOf("=") + 1).trim();
            logger.info("Adding mapping: " + hostName + " --> " + servletName);
            this.hostMap.put(hostName, servletName);
        }
    }


    protected void service(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        String hostName = URLUtil.getHostName(request);
        String servletName = (String) this.hostMap.get(hostName);
        
        if (servletName == null) {
            throw new ServletException(
                "No servlet mapping for host name '" + hostName + "'");
        }
        
        RequestDispatcher rd = this.servletContext.getNamedDispatcher(servletName);
        if (rd == null) {
            throw new ServletException(
                "No request dispatcher available for servlet '" + servletName + "'");
        }
        
        if (logger.isDebugEnabled()) {
            logger.debug("Forwarding request '" + request.getRequestURL()
                         + "' to servlet '" + servletName + "'");
        }


        rd.forward(request, response);
    }
    
    
}

