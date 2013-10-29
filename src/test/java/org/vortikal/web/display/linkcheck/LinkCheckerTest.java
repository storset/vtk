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
package org.vortikal.web.display.linkcheck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;
import org.vortikal.web.display.linkcheck.LinkChecker.LinkCheckResult;
import org.vortikal.web.display.linkcheck.LinkChecker.Status;
import org.vortikal.web.service.URL;

public class LinkCheckerTest {

    private LinkChecker linkChecker = new LinkChecker();
    private Mockery context = new JUnit4Mockery();
    private Ehcache mockCache;

    @Before
    public void setUp() {
        mockCache = context.mock(Ehcache.class);
        linkChecker.setCache(mockCache);
    }

    @Test
    public void testValidateStatusOk() {

        URL base = URL.parse("http://www.uio.no/index.html");
        Status expectedStatus = Status.OK;
        String expectedMessage = null;

        List<TestLinkCheckObject> testOkLinks = new ArrayList<TestLinkCheckObject>();
        testOkLinks.add(new TestLinkCheckObject("http://httpstat.us/200", base, expectedStatus, expectedMessage));
        testOkLinks.add(new TestLinkCheckObject("http://httpstat.us/301", base, expectedStatus, expectedMessage));
        testOkLinks.add(new TestLinkCheckObject("http://httpstat.us/302", base, expectedStatus, expectedMessage));
        testOkLinks.add(new TestLinkCheckObject("http://httpstat.us/303", base, expectedStatus, expectedMessage));
        testOkLinks.add(new TestLinkCheckObject("https://www.uio.no/om/index.html", base, expectedStatus,
                expectedMessage));
        testValidation(testOkLinks);

    }

    @Test
    public void testValidateStatusNotFound() {

        URL base = URL.parse("http://www.usit.uio.no/index.html");
        Status expectedStatus = Status.NOT_FOUND;
        String expectedMessage = null;

        List<TestLinkCheckObject> testNotFoundLinks = new ArrayList<TestLinkCheckObject>();
        testNotFoundLinks.add(new TestLinkCheckObject("http://httpstat.us/404", base, expectedStatus, expectedMessage));
        testNotFoundLinks.add(new TestLinkCheckObject("http://httpstat.us/410", base, expectedStatus, expectedMessage));
        testNotFoundLinks.add(new TestLinkCheckObject("https://www.usit.uio.no/not-found.html", base, expectedStatus,
                expectedMessage));
        testValidation(testNotFoundLinks);
    }

    @Test
    public void testValidateStatusMalformed() {
        testValidation(new TestLinkCheckObject("http://foo.com:NaN", 
                URL.parse("http://foo.com/bar"), Status.MALFORMED_URL, null));
    }

    @Test
    public void testIDN() throws java.net.MalformedURLException {
        java.net.URL url = new java.net.URL("http://www.øl.com/#/BedsteBryggeprocess");
        assertEquals(new java.net.URL("http://www.xn--l-4ga.com/#/BedsteBryggeprocess"), 
                LinkChecker.toIDN(url));
        url = new java.net.URL("http://plain-ascii.com/foo/bar");
        assertEquals(url, LinkChecker.toIDN(url));
    }

    private void testValidation(List<TestLinkCheckObject> testLinks) {

        for (TestLinkCheckObject testLink : testLinks) {
            testValidation(testLink);
        }

    }

    private void testValidation(TestLinkCheckObject testLink) {

        final String href = testLink.testHref;
        final LinkCheckResult expected = new LinkCheckResult(testLink.testHref, testLink.expectedStatus,
                testLink.expectedReason);

        if (testLink.expectedStatus != Status.MALFORMED_URL) {
            context.checking(new Expectations() {
                {
                    one(mockCache).get(href);
                    will(returnValue(null));
                }
            });

            context.checking(new Expectations() {
                {
                    one(mockCache).put(new Element(href, expected));
                }
            });
        }
        LinkCheckResult actual = linkChecker.validate(href, testLink.testBase);
        assertNotNull("Did not return expected link check result for '" + testLink.testHref + "'", actual);
        assertEquals("Did not return expected status for '" + testLink.testHref + "'", expected.getStatus(),
                actual.getStatus());

    }
    
    private class TestLinkCheckObject {

        String testHref;
        URL testBase;
        Status expectedStatus;
        String expectedReason;

        public TestLinkCheckObject(String testHref, URL testBase, Status expectedStatus, String expectedReason) {
            this.testHref = testHref;
            this.testBase = testBase;
            this.expectedStatus = expectedStatus;
            this.expectedReason = expectedReason;
        }

    }

}
