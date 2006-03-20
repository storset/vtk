package org.vortikal.web.controller;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;
import org.springframework.mock.web.MockHttpServletRequest;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;

public class DisplayXmlResourceControllerTestCase extends MockObjectTestCase {

    private static Log logger = LogFactory.getLog(DisplayXmlResourceControllerTestCase.class);

    private Mock mockRepository;

    private HttpServletRequest request;

    private DisplayXmlResourceController controller;

    private String uri = "/hest.html";

    private String token;

    protected void setUp() throws Exception {
        super.setUp();
        BasicConfigurator.configure();
        request = new MockHttpServletRequest();
        controller = new DisplayXmlResourceController();
        RequestContext requestContext = new RequestContext(request, null, uri);
        RequestContext.setRequestContext(requestContext);
        token = "";
        SecurityContext securityContext = new SecurityContext(token, null);
        SecurityContext.setSecurityContext(securityContext);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testLastModified() {

        long lastModified;
        
        Date lastModifiedExpected = new Date();
        Mock mockResource = mock(Resource.class);
        mockResource.expects(atLeastOnce()).method("isCollection").withNoArguments().will(
                returnValue(false));
        mockResource.expects(atLeastOnce()).method("getLastModified").withNoArguments().will(
                returnValue(lastModifiedExpected));
        Resource resource = (Resource) mockResource.proxy();
        // Resource resource = new ResourceImpl(uri, null, null);

        mockRepository = mock(Repository.class);
        mockRepository.expects(atLeastOnce()).method("retrieve").with(eq(token), eq(uri), eq(true))
                .will(returnValue(resource));

        assertNotNull(mockRepository);
        controller.setRepository((Repository) mockRepository.proxy());

        lastModified = controller.getLastModified(request);
        assertEquals(-1, lastModified);
        controller.setHandleLastModified(true);
        lastModified = controller.getLastModified(request);
        assertEquals(lastModifiedExpected.getTime(), lastModified);
    }
}
