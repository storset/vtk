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
package org.vortikal.repository;

import java.util.Locale;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.vortikal.security.Principal;
import org.vortikal.security.Principal.Type;
import org.vortikal.security.PrincipalFactory;
import org.vortikal.text.html.HtmlUtil;

public class CommonApplicationContextTestIntegration extends AbstractBeanContextTestIntegration {

    public void testCommonConfiguration() {
        ApplicationContext ctx = getApplicationContext(new String[] {});

        checkForBeanInConfig(ctx, "defaultMessageSource");
        ResourceBundleMessageSource resourceBundleMessageSource = (ResourceBundleMessageSource) ctx
                .getBean("defaultMessageSource");
        String message = resourceBundleMessageSource.getMessage("title.admin", new String[] { "testfolder" },
                new Locale("en"));
        assertEquals("Managing: testfolder", message);

        checkForBeanInConfig(ctx, "htmlUtil");
        HtmlUtil htmlUtil = (HtmlUtil) ctx.getBean("htmlUtil");
        String html = "<html><body>TEST</body></html>";
        assertEquals("TEST", htmlUtil.flatten(html));

        checkForBeanInConfig(ctx, "principalFactory");
        PrincipalFactory principalFactory = (PrincipalFactory) ctx.getBean("principalFactory");
        Principal principal = principalFactory.getPrincipal(PrincipalFactory.NAME_ALL, Type.PSEUDO);
        assertNotNull("No principal returned", principal);
        assertEquals("Wrong principal returned", principal, PrincipalFactory.ALL);

    }

}
