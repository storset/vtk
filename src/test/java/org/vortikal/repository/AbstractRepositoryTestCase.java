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
package org.vortikal.repository;

import junit.framework.TestCase;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.vortikal.security.token.TokenManager;


public abstract class AbstractRepositoryTestCase extends TestCase {

    private ClassPathXmlApplicationContext context;
    private Repository repository;
    private TokenManager tokenManager;
    

    protected void setUp() throws Exception {
        super.setUp();
        this.context = new ClassPathXmlApplicationContext(
            new String[] {
                "/vortikal/beans/vhost/security.xml",
                "/vortikal/beans/vhost/resource-types/main.xml",
                "/vortikal/beans/vhost/actions.xml",
                "/vortikal/beans/vhost/repository.xml",
                "/org/vortikal/repository/in-memory-repository.xml"
            });
        this.repository = (Repository) this.context.getBean("repository");
        this.tokenManager = (TokenManager) this.context.getBean("tokenManager");
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        this.context.destroy();
    }

    protected ApplicationContext getApplicationContext() {
        return this.context;
    }

    protected Repository getRepository() {
        return this.repository;
    }

    protected TokenManager getTokenManager() {
        return this.tokenManager;
    }
    
}

