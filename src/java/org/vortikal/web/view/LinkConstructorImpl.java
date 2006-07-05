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

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.repository.URIUtil;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;

public class LinkConstructorImpl implements LinkConstructor,
		ApplicationContextAware, InitializingBean {

    private Log logger = LogFactory.getLog(this.getClass());
    
	private Repository repository;
	private ApplicationContext context;
	
	// FIXME: Expand '../'
    public String construct(String resourceURI, String parametersCSV, String serviceName) {

        this.logger.debug("About to construct link to service '" + serviceName + "' for resource '"
                + resourceURI + "' with parameters '" + parametersCSV + "'");
        
        String token = SecurityContext.getSecurityContext().getToken();
        Principal principal = SecurityContext.getSecurityContext().getPrincipal();
        
        Service service = null;
		Resource resource = null;
		Map parameters = null;

        String currentUri = RequestContext.getRequestContext().getResourceURI();
        
		try {
            if (resourceURI == null || resourceURI.equals("")) {
                resourceURI = currentUri;
            } else {
                resourceURI = URIUtil.getAbsolutePath(resourceURI, currentUri);
            }

            if (serviceName == null || serviceName.equals(""))
                service = RequestContext.getRequestContext().getService();
            else {
                service = getService(serviceName);
            }
            
            if (parametersCSV != null && !parametersCSV.trim().equals("")) {
                this.logger.debug("Trying to get parameters");
                parameters = getParametersMap(parametersCSV);
            }
            
            resource = this.repository.retrieve(token, resourceURI, true);
            
            this.logger.debug("Trying to construct link to service '" + service.getName()
                    + "' for resource '" + resource.getURI() + "'");
            
            return service.constructLink(resource, principal, parameters, false);
		
		} catch (Exception e) {
            this.logger.warn("Caught exception on link construction", e);
		}
        return "";
	}

	private Service getService(String serviceName) {
        return (Service) this.context.getBean(serviceName, Service.class);
    }

	private Map getParametersMap(String parametersCSV) {
		Map parameters = new LinkedHashMap();
		String[] mappings = parametersCSV.split(",");
		for (int i = 0; i < mappings.length; i++) {
			if (mappings[i].indexOf("=") == -1) {
				throw new IllegalArgumentException(
						"Each entry in the parameters string must be in the format "
						+ "'<paramname>=<paramvalue>'");
			}	

			String parameterName = mappings[i].substring(0, mappings[i].indexOf("=")).trim();
			
			String parameterValue = mappings[i].substring(mappings[i].lastIndexOf("=") + 1).trim();
			parameters.put(parameterName, parameterValue);
		}
		return parameters;
	}

	public void afterPropertiesSet() throws Exception {
		if (this.context == null) throw new BeanInitializationException(
				"Property 'applicationContext' must be set");
		if (this.repository == null) throw new BeanInitializationException(
		"Property 'repository' must be set");
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}
	
	public void setApplicationContext(ApplicationContext context)
	throws BeansException {
		this.context = context;	
	}


}
