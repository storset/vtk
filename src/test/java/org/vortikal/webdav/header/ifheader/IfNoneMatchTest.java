/* Copyright (c) 2005, University of Oslo, Norway
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
package org.vortikal.webdav.header.ifheader;

import junit.framework.TestCase;

import org.apache.log4j.BasicConfigurator;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.springframework.mock.web.MockHttpServletRequest;
import org.vortikal.repository.Resource;
import org.vortikal.webdav.ifheader.IfNoneMatchHeader;

public class IfNoneMatchTest extends TestCase {

    private Resource resource;
    
    private final String etag = "\"I am an ETag\"";
    private final String anotherEtag = "\"I am another ETag\"";
    
    private Mockery context = new JUnit4Mockery();
    private final Resource mockResource = context.mock(Resource.class);

    static {
        BasicConfigurator.configure();
    }
    
    protected void setUp() throws Exception {
        super.setUp();
        context.checking(new Expectations() {{ one(mockResource).getEtag(); will(returnValue(etag)); }});
        this.resource = mockResource;
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testCorrectEtag() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("If-None-Match", this.etag);
        IfNoneMatchHeader ifNoneMatchHeader = new IfNoneMatchHeader(request);
        assertFalse(ifNoneMatchHeader.matches(this.resource));
    }
    
    public void testWrongEtag() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("If-None-Match", this.anotherEtag);
        IfNoneMatchHeader ifNoneMatchHeader = new IfNoneMatchHeader(request);
        assertTrue(ifNoneMatchHeader.matches(this.resource));
    }
    
    public void testAllEtag() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("If-None-Match", "*");
        IfNoneMatchHeader ifNoneMatchHeader = new IfNoneMatchHeader(request);
        assertFalse(ifNoneMatchHeader.matches(this.resource));
        this.resource.getEtag(); //to make to mock object happy
    }
    
    public void testNoEtag() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        IfNoneMatchHeader ifNoneMatchHeader = new IfNoneMatchHeader(request);
        assertTrue(ifNoneMatchHeader.matches(this.resource));
        this.resource.getEtag(); //to make to mock object happy
    }
    
}