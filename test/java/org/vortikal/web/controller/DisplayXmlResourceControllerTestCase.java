/* Copyright (c) 2006, University of Oslo, Norway
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
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repositoryimpl.PropertyImpl;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;

public class DisplayXmlResourceControllerTestCase extends MockObjectTestCase {

    private static Log logger = LogFactory.getLog(DisplayXmlResourceControllerTestCase.class);

    private String schemaPropertyName = "schema";

    private Namespace schemaNamespace = Namespace.CUSTOM_NAMESPACE;

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
        schemaProperty.setNamespace(schemaNamespace);
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

