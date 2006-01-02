/* Copyright (c) 2005, University of Oslo, Norway
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
package org.vortikal.web.controller.index;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.index.dms.DMSXmlQuery;
import org.vortikal.security.SecurityContext;

/**
 * Perform DMS query via HTTP, get results as XML.
 * 
 * @author oyviste
 */
public class DMSXmlQueryController implements Controller, 
    InitializingBean {
    
    Log logger = LogFactory.getLog(DMSXmlQueryController.class);
    private String expressionParameterName = "query";
    private String multiFieldParameterName = "multifield";
    private String limitParameterName = "limit";
    private String sortParameterName = "sort";
    private String invertParameterName = "invert";

    private int defaultMaxLimit = 500;
    
    private DMSXmlQuery dmsXmlQueryHelper;
    
    public void afterPropertiesSet()
        throws BeanInitializationException {
        if (dmsXmlQueryHelper == null) {
            throw new BeanInitializationException("Property" +
                    " 'dmsXmlQueryHelper' not set.");
        }
    }
    
    public ModelAndView handleRequest(HttpServletRequest request, 
            HttpServletResponse response) throws IOException {

        String query = request.getParameter(expressionParameterName);
        if (query == null) 
            return null;

        String multiField = request.getParameter(multiFieldParameterName);

        int maxResults = defaultMaxLimit;
        String limitStr = request.getParameter(limitParameterName);
        if (limitStr != null) {
            try {
                maxResults = Integer.parseInt(limitStr);
            } catch (NumberFormatException e) { }
        }

        String sortStr = request.getParameter(sortParameterName);

        // Used for security filtering of search results.
        String securityToken = SecurityContext.getSecurityContext().getToken();
        
        Document xmlResult = dmsXmlQueryHelper.executeQuery(query, multiField != null, 
                                                            securityToken, maxResults,
                                                            sortStr);
        if (xmlResult == null) {
            logger.warn("Query result document null.");
            return null;
        }

        // Output XML using JDOM's XMLOutputter for a definite hole-in-one.
        response.setContentType("text/xml");
        // FIXME: pretty format only used for debugging and testing. 
        // Raw or compact should be faster.
        XMLOutputter xmlOut = new XMLOutputter(Format.getPrettyFormat());
        OutputStream outputStream = response.getOutputStream();
        xmlOut.output(xmlResult, outputStream);
        outputStream.close();
        
        return null; // No model or view for this controller.
    }
    
    public void setDmsXmlQueryHelper(DMSXmlQuery dmsXmlQueryHelper) {
        this.dmsXmlQueryHelper = dmsXmlQueryHelper;
    }

    public void setExpressionParameterName(String expressionParameterName) {
        this.expressionParameterName = expressionParameterName;
    }

    public void setMultiFieldParameterName(String multiFieldParameterName) {
        this.multiFieldParameterName = multiFieldParameterName;
    }

    public void setLimitParameterName(String limitParameterName) {
        this.limitParameterName = limitParameterName;
    }

    public void setSortParameterName(String sortParameterName) {
        this.sortParameterName = sortParameterName;
    }

    public void setInvertParameterName(String invertParameterName) {
        this.invertParameterName = invertParameterName;
    }
    
    public void setDefaultMaxLimit(int defaultMaxLimit) {
        this.defaultMaxLimit = defaultMaxLimit;
    }
    
}
