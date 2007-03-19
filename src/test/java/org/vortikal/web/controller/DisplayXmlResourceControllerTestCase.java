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

import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;

import org.springframework.mock.web.MockHttpServletRequest;

import org.vortikal.context.BaseContext;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repositoryimpl.PropertyImpl;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;


public class DisplayXmlResourceControllerTestCase extends MockObjectTestCase {

    private String schemaPropertyName = "schema";

    private Namespace schemaNamespace = Namespace.DEFAULT_NAMESPACE;

    private String faqSchema = "http://www.uio.no/xsd/uio/faq/v001/faq.xsd";

    private Mock mockRepository;

    private HttpServletRequest request;

    private DisplayXmlResourceController controller;

    private String uri = "/hest.xml";

    private String token;

    protected void setUp() throws Exception {
        super.setUp();
        this.request = new MockHttpServletRequest();
        this.controller = new DisplayXmlResourceController();
        BaseContext.pushContext();
        RequestContext requestContext = new RequestContext(this.request, null, this.uri);
        RequestContext.setRequestContext(requestContext);
        this.token = "";
        SecurityContext securityContext = new SecurityContext(this.token, null);
        SecurityContext.setSecurityContext(securityContext);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testLastModified() {

        long lastModified;

        PropertyImpl schemaProperty = new PropertyImpl();
        schemaProperty.setNamespace(this.schemaNamespace);
        schemaProperty.setName(this.schemaPropertyName);
        schemaProperty.setStringValue(this.faqSchema);

        Date lastModifiedExpected = new Date();
        Mock mockResource = mock(Resource.class);
        mockResource.expects(atLeastOnce()).method("isCollection").withNoArguments().will(
                returnValue(false));
        mockResource.expects(atLeastOnce()).method("getLastModified").withNoArguments().will(
                returnValue(lastModifiedExpected));
        Resource resource = (Resource) mockResource.proxy();

        this.mockRepository = mock(Repository.class);
        this.mockRepository.expects(atLeastOnce()).method("retrieve").
            with(eq(this.token), eq(this.uri), eq(true))
                .will(returnValue(resource));

        assertNotNull(this.mockRepository);
        this.controller.setRepository((Repository) this.mockRepository.proxy());

        lastModified = this.controller.getLastModified(this.request);
        assertEquals(-1, lastModified);

        this.controller.setHandleLastModified(true);
        lastModified = this.controller.getLastModified(this.request);
        assertEquals(lastModifiedExpected.getTime(), lastModified);

        List schemaList = new ArrayList();
        schemaList.add(faqSchema);
    }
}

