package org.vortikal.web.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;
import org.springframework.mock.web.MockHttpServletRequest;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repositoryimpl.PropertyImpl;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;

public class DisplayXmlResourceControllerTestCase extends MockObjectTestCase {

    private static Log logger = LogFactory.getLog(DisplayXmlResourceControllerTestCase.class);

    private String schemaPropertyName = "schema";

    private String schemaNamespace = " http://www.uio.no/vortex/custom-properties";

    private String faqSchema = "http://www.uio.no/xsd/uio/faq/v001/faq.xsd";

    private Mock mockRepository;

    private HttpServletRequest request;

    private DisplayXmlResourceController controller;

    private String uri = "/hest.xml";

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

        PropertyImpl schemaProperty = new PropertyImpl();
        schemaProperty.setNamespace(schemaPropertyName);
        schemaProperty.setName(schemaPropertyName);
        schemaProperty.setStringValue(faqSchema);

        Date lastModifiedExpected = new Date();
        Mock mockResource = mock(Resource.class);
        mockResource.expects(atLeastOnce()).method("isCollection").withNoArguments().will(
                returnValue(false));
        mockResource.expects(atLeastOnce()).method("getLastModified").withNoArguments().will(
                returnValue(lastModifiedExpected));
        mockResource.expects(atLeastOnce()).method("getProperty").with(eq(schemaNamespace),
                eq(schemaPropertyName)).will(returnValue(schemaProperty));
        Resource resource = (Resource) mockResource.proxy();

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

        List schemaList = new ArrayList();
        schemaList.add(faqSchema);
        controller.setSchemasForHandleLastModified(schemaList);
        controller.setHandleLastModifiedForSchemasInList(true);
        lastModified = controller.getLastModified(request);
        assertEquals(lastModifiedExpected.getTime(), lastModified);
        
        controller.setSchemasForHandleLastModified(schemaList);
        controller.setHandleLastModifiedForSchemasInList(false);
        lastModified = controller.getLastModified(request);
        assertEquals(-1, lastModified);

    }
}

