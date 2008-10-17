package org.vortikal.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.vortikal.context.BaseContext;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;

public abstract class AbstractControllerTest extends TestCase {
	
	protected Mockery context = new JUnit4Mockery();
	protected final HttpServletRequest mockRequest = context.mock(HttpServletRequest.class);
	protected final HttpServletResponse mockResponse = context.mock(HttpServletResponse.class);
	protected final Repository mockRepository = context.mock(Repository.class);
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
        BaseContext.pushContext();
        RequestContext requestContext = new RequestContext(null, null, getRequestPath());
        RequestContext.setRequestContext(requestContext);
        SecurityContext securityContext = new SecurityContext(null, null);
        SecurityContext.setSecurityContext(securityContext);
	}
	
	protected abstract Path getRequestPath();

}
