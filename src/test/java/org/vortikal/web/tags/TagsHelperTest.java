/* Copyright (c) 2008, University of Oslo, Norway
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
package org.vortikal.web.tags;

import javax.servlet.http.HttpServletRequest;

import junit.framework.TestCase;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.vortikal.repository.Path;
import org.vortikal.web.tags.TagsHelper;

public class TagsHelperTest extends TestCase {

    private Mockery context = new JUnit4Mockery();
    private final HttpServletRequest mockRequest = context.mock(HttpServletRequest.class);


    // TODO mock requestcontext to test current collection (no scope or empty
    // scope parameter)

    public void testGetScopePath() {
        final String requesteScopePath = "/test";
        prepareRequest(requesteScopePath);
        Path scopePath = TagsHelper.getScopePath(mockRequest);
        assertNotNull(scopePath);
        assertEquals(scopePath, Path.fromString(requesteScopePath));
    }


    public void testGetScopePathRoot() {
        prepareRequest("/");
        Path scopePath = TagsHelper.getScopePath(mockRequest);
        assertNotNull(scopePath);
        assertEquals(scopePath, Path.ROOT);
    }


    public void testGetScopePathInvalid() {
        final String requesteScopePath = "test";
        Path scopePath = null;
        prepareRequest(requesteScopePath);
        try {
            scopePath = TagsHelper.getScopePath(mockRequest);
            fail();
        } catch (IllegalArgumentException e) {
            assertNull(scopePath);
        }
    }


    private void prepareRequest(final String requestedScopePath) {
        context.checking(new Expectations() {
            {
                one(mockRequest).getParameter(TagsHelper.SCOPE_PARAMETER);
                will(returnValue(requestedScopePath));
            }
        });
    }

}
