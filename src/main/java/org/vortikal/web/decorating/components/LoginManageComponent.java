/* Copyright (c) 2012, University of Oslo, Norway
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
package org.vortikal.web.decorating.components;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.web.RequestContext;
import org.vortikal.web.decorating.DecoratorRequest;
import org.vortikal.web.decorating.DecoratorResponse;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

public class LoginManageComponent extends ViewRenderingDecoratorComponent {

	private static final String DESCRIPTION = "Displays an authentication and manage component";

	private Service defaultLoginService;
	private Map<String, Service> alternativeLoginServices;
	private Service logoutService;

	protected void processModel(Map<String, Object> model,
			DecoratorRequest request, DecoratorResponse response)
			throws Exception {

		super.processModel(model, request, response);
		RequestContext requestContext = RequestContext.getRequestContext();
		Repository repository = requestContext.getRepository();
		Path uri = requestContext.getResourceURI();
		Principal principal = requestContext.getPrincipal();
		String token = requestContext.getSecurityToken();
		Resource resource = repository.retrieve(token, uri, true);

		// VTK-2460
		if (requestContext.isViewUnauthenticated()) {
			principal = null;
		}
		
		model.put("principal", principal);

		String displayOnlyIfAuthReq = request.getStringParameter("display-only-if-auth");
		String displayAuthUserReq = request.getStringParameter("display-auth-user");
		boolean displayOnlyIfAuth = displayOnlyIfAuthReq != null && "true".equals(displayOnlyIfAuthReq);
		boolean displayAuthUser = displayAuthUserReq != null && "true".equals(displayAuthUserReq);

		Map<String, URL> options = new LinkedHashMap<String, URL>();

		try {
			if (principal == null && !displayOnlyIfAuth) { // Not logged in (unauthenticated)
				URL loginURL = this.defaultLoginService.constructURL(resource, principal);
			    loginURL.addParameter("authTarget", "http");
				options.put("login", loginURL);
				this.putAdminURL(options, resource, false);
			} else if(principal != null) { // Logged in (authenticated)
				if (displayAuthUser) {
					options.put("principal-desc", null);
				}
				this.putAdminURL(options, resource, true);
				options.put("logout", this.logoutService.constructURL(resource, principal));
			}
		} catch (Exception e) {}
		
		model.put("options", options);
	}
	
	private void putAdminURL(Map<String, URL> options, Resource resource, boolean hasPrincipal) throws Exception {
		Service adminService = this.alternativeLoginServices.get("admin");
		if (adminService != null) {
			if (resource.isCollection()) {
				URL adminCollectionURL = adminService.constructURL(resource.getURI());
				if(!hasPrincipal) {
					adminCollectionURL.addParameter("authTarget", "http");
				}
				options.put("admin-collection", adminCollectionURL);
			} else {
				URL adminURL = adminService.constructURL(resource.getURI());
				if(!hasPrincipal) {
					adminURL.addParameter("authTarget", "http");
				}
				options.put("admin", adminURL);
			}
		}
	}

	public void setAlternativeLoginServices(Map<String, Service> alternativeLoginServices) {
		this.alternativeLoginServices = alternativeLoginServices;
	}

	public Map<String, Service> getAlternativeLoginServices() {
		return alternativeLoginServices;
	}

	@Required
	public void setDefaultLoginService(Service loginService) {
		if (loginService == null) {
			throw new IllegalArgumentException("Argument cannot be null");
		}
		this.defaultLoginService = loginService;
	}

	@Required
	public void setLogoutService(Service logoutService) {
		if (logoutService == null) {
			throw new IllegalArgumentException("Argument cannot be null");
		}
		this.logoutService = logoutService;
	}

	protected String getDescriptionInternal() {
		return DESCRIPTION;
	}

	protected Map<String, String> getParameterDescriptionsInternal() {
		Map<String, String> map = new HashMap<String, String>();
		return map;
	}

}
