/* Copyright (c) 2008, University of Oslo, Norway
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
package org.vortikal.web.controller.autocomplete;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.security.SecurityContext;

public class AutoCompleteController implements Controller {
    
    private final Log logger = LogFactory.getLog(getClass());
    private final String callback = "callback";
    
    private AutoCompleteDataProvider dataProvider;
    private String fieldName;

    public ModelAndView handleRequest(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        
        String token = SecurityContext.getSecurityContext().getToken();
        // Not using contextUri, yet.        
//      RequestContext requestContext = RequestContext.getRequestContext();
//      String contextUri = requestContext.getCurrentCollection();
        
        String query = request.getParameter(this.fieldName);
        if (query == null) {
            return null;
        }
        // XXX: Further input data validation of some sorts necessary ?
        
        Map<String, Object> resultSet = new HashMap<String, Object>();
        List<Object> completions = 
                     this.dataProvider.getPrefixCompletions(query, null, token);
        
        resultSet.put(this.fieldName, completions);
        
        if (logger.isDebugEnabled()) {
            logger.debug("Completion items for query: '" 
                    + query + "' on field '" + this.fieldName + "':");
            for (Object item: completions) {
                logger.debug(item);
            }
        }

        try {
            JSONObject completionList = (JSONObject) JSONSerializer.toJSON(resultSet);
            String jsonString = completionList.toString();
            
            // YUI requires that any service returning JSON for autocomplete be
            // wrapped by the 'callback' function if it's present on the request
            String callbackFunction = request.getParameter(this.callback);
            if (callbackFunction != null) {
               jsonString = callbackFunction + "(" + jsonString + ")";
            }
            
            // YUI prefers the contenttype to be text/javascript
            // as opposed to application/json
            response.setContentType("text/javascript;charset=utf-8");
            response.getWriter().print(jsonString);
        } catch (JSONException jse) {
            logger.warn(
              "Unable to serialize auto-complete data provider result to JSON", jse);
        }

        return null;
    }
    
    @Required
    public void setDataProvider(AutoCompleteDataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }
    
    @Required
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }
    
}
