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
package org.vortikal.web.service;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.util.net.NetUtils;
import org.vortikal.util.web.URLUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Default implementation of the LinkConstructor interface.
 *
 * TODO: Provide "URL rewrite" hooks that would work with the service
 * mapping framework (e.g. "http://<url>/users/foo --> http://<url>/usersearch?user=foo)
 */
public class DefaultLinkConstructor implements LinkConstructor {

    private static Log logger = LogFactory.getLog(DefaultLinkConstructor.class);
    


    public String construct(Resource resource, Principal principal,
                            Map parameters, List assertions, Service service,
                            boolean matchAssertions) {

        String requestProtocol = null;
        String requestHostName = null;
        int requestPort = -1;
        Map requestParameters = new HashMap();
        String requestUriPrefix = null;

        for (Iterator iter = assertions.iterator(); iter.hasNext();) {
            Assertion assertion = (Assertion) iter.next();

            if (assertion instanceof RequestHostNameAssertion) {
                requestHostName = ((RequestHostNameAssertion) assertion)
                    .getHostName();

            } else if (assertion instanceof RequestParameterAssertion) {
                RequestParameterAssertion parameterAssertion = 
                    (RequestParameterAssertion) assertion;
                String parameterName = parameterAssertion.getParameterName();
                String parameterValue = parameterAssertion.getParameterValue();
                requestParameters.put(parameterName, parameterValue);

            } else if (assertion instanceof RequestPortAssertion) {
                requestPort = ((RequestPortAssertion) assertion).getPort();

            } else if (assertion instanceof RequestProtocolAssertion) {
                requestProtocol = ((RequestProtocolAssertion) assertion).getProtocol();

            } else if (assertion instanceof RequestUriPrefixAssertion) {
                requestUriPrefix = ((RequestUriPrefixAssertion) assertion)
                    .getPrefix();

            } else if (assertion instanceof ResourceAssertion) {
                boolean match = (matchAssertions) ? 
                    ((ResourceAssertion) assertion) .matches(resource) : true;
                if (match == false) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Unable to construct link to resource: "
                                     + resource + ";principal: " + principal
                                     + ";service: " + service.getName() + ": "
                                     + "Unmatched assertion: " + assertion);
                    }

                    throw new ServiceUnlinkableException(
                        "Service " + service.getName() + " cannot be applied to resource "
                        + resource.getURI() + ". Assertion " + assertion
                        + "false for resource.");
                }

            } else if (assertion instanceof PrincipalAssertion) {
                boolean match = (matchAssertions) ? 
                    ((PrincipalAssertion) assertion) .matches(principal) : true;
                if (match == false) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Unable to construct link to resource: "
                                     + resource + ";principal: " + principal
                                     + ";service: " + service.getName() + ": "
                                     + "Unmatched assertion: " + assertion);
                    }

                    throw new ServiceUnlinkableException(
                        "Service " + service.getName() + " cannot be applied to principal "
                        + principal + ". Assertion " + assertion
                        + "false for principal.");
                }
            }
        }
		
        if (requestHostName == null) requestHostName = NetUtils.guessHostName();
        if (requestProtocol == null) requestProtocol = "http";
        if (requestUriPrefix == null) requestUriPrefix = "";
		
        String url = requestProtocol + "://" + requestHostName + 
            ((requestPort != -1) ? ":" + requestPort : "") +
            requestUriPrefix + URLUtil.urlEncode(resource.getURI());

        if (resource.isCollection() && !resource.getURI().equals("/")) url += "/";
        
        if (!requestParameters.isEmpty()) {
            url += "?";
            for (Iterator iter = requestParameters.keySet().iterator(); iter.hasNext();) {
                String name = (String) iter.next();
                String value = (String) requestParameters.get(name);
				
                url += name + "=" + value; 
					
                if (iter.hasNext()) url += "&";
            }
        }
		
        if (parameters != null && parameters.size() > 0) {
            for (Iterator i = parameters.keySet().iterator(); i.hasNext();) {
                Object key = i.next();
                Object value = parameters.get(key);
                if (url.indexOf('?') > 0) {
                    url += "&" + key + "=" + URLUtil.urlEncode(value.toString());
                } else {
                    url += "?" + key + "=" + URLUtil.urlEncode(value.toString());
                }
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Constructed URL: '" + url + "' (from resource: " +
                         resource + ";principal: " + principal +
                         ";service: " + service.getName() + ")");
        }


        return url;
    }

}
