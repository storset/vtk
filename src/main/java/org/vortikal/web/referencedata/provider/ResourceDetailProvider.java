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
package org.vortikal.web.referencedata.provider;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.web.RequestContext;
import org.vortikal.web.referencedata.ReferenceDataProvider;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.ServiceUnlinkableException;

/**
 * Model builder that retrieves various resource detail about the
 * current resource.  The information is made available in the model
 * as a submodel of the name <code>resourceDetail</code>.
 * 
 * <p>Configurable properties:
 * <ul>
 *   <li><code>serviceMap</code> - a <code>java.util.Map</code>
 *       containing mappings between names and instances of {@link
 *       Service} objects, which will be used to construct links in
 *       the resulting model.
 * </ul>
 * 
 * <p>Model data provided:
 * <ul>
 *   <li>a map between the keys of the configured
 *   <code>serviceMap</code> parameter and the URLs that results from
 *   invoking <code>Service.constructLink()</code> with the target
 *   services on the current resource and principal.
 * </ul>
 *
 * <p>Example: when specifying the following <code>serviceMap</code>
 * configuration parameter: <code>{foo = A, bar = B}</code>, the
 * resulting map will be <code>{foo = A', bar = B'}</code>, where
 * <code>A'</code> and <code>B'</code> are the URLs constructed using
 * service <code>A</code> and <code>B</code>, respectively.
 * 
 */
public class ResourceDetailProvider implements InitializingBean, ReferenceDataProvider {

    private Map<String, Service> serviceMap = null;
        
    public void setServiceMap(Map<String, Service> serviceMap) {
        this.serviceMap = serviceMap;
    }
    
    public void afterPropertiesSet() throws Exception {
        if (this.serviceMap == null) {
            throw new BeanInitializationException(
                "Bean property 'serviceMap' must be set");
        }
    }

    @Override
    public void referenceData(Map<String, Object> model, HttpServletRequest request)
        throws Exception {
        Map<String, Object> resourceDetailModel = new HashMap<String, Object>();

        RequestContext requestContext = RequestContext.getRequestContext();
        Repository repository = requestContext.getRepository();
        
        Resource resource = null;
        try {
             resource = repository.retrieve(
                 requestContext.getSecurityToken(), requestContext.getResourceURI(), false);
        } catch (Throwable t) { }

        for (Map.Entry<String, Service> entry: this.serviceMap.entrySet()) {
            String key = entry.getKey();
            Service service = (Service) entry.getValue();

            String url = null;
            try {
                if (resource != null) {
                    url = service.constructLink(
                        resource, requestContext.getPrincipal());
                }
            } catch (ServiceUnlinkableException e) {
                // Ignore
            }
            resourceDetailModel.put(key, url);
        }
        model.put("resourceDetail", resourceDetailModel);
    }

}
