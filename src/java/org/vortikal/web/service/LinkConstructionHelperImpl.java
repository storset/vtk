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
 * Default implementation of the LinkConstructionHelper interface.
 *
 * TODO: Provide "URL rewrite" hooks that would work with the service
 * mapping framework (e.g. "http://<url>/users/foo --> http://<url>/usersearch?user=foo)
 */
public class LinkConstructionHelperImpl implements LinkConstructionHelper {

    private static Log logger = LogFactory.getLog(LinkConstructionHelperImpl.class);
    


    public String construct(Resource resource, Principal principal,
                            Map parameters, List assertions, Service service,
                            boolean matchAssertions) {

        String path = resource.getURI();
        if (resource.isCollection()) {
            path += "/";
        }
        URL urlObject = new URL("http", NetUtils.guessHostName(), path);
        urlObject.setQuery(parameters);

        for (Iterator i = assertions.iterator(); i.hasNext();) {
            Assertion assertion = (Assertion) i.next();

            if (matchAssertions) {
                matchAssertion(service, assertion, resource, principal);
            }

            assertion.processURL(urlObject, resource, principal);
        }
        
        String url = urlObject.toString();

        if (logger.isDebugEnabled()) {
            logger.debug("Constructed URL: '" + url + "' (from resource: " +
                         resource + ";principal: " + principal +
                         ";service: " + service.getName() + ")");
        }
        return url;
    }


    private void matchAssertion(Service service, Assertion assertion,
                                   Resource resource, Principal principal)
        throws ServiceUnlinkableException {
        
        if (assertion instanceof ResourceAssertion) {
            boolean match = ((ResourceAssertion) assertion).matches(resource);
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
            boolean match = ((PrincipalAssertion) assertion).matches(principal);
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

}
