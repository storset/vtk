/* Copyright (c) 2009, University of Oslo, Norway
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
package org.vortikal.beans.vhost;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Random;

import org.springframework.context.ApplicationContext;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceNotFoundException;
import org.vortikal.repository.Repository.Depth;
import org.vortikal.security.PrincipalImpl;
import org.vortikal.security.Principal.Type;
import org.vortikal.security.token.TokenManager;

public class Repo2TestIntegration extends AbstractBeanContextTestIntegration {

    private ApplicationContext ctx;
    private Repository repo2;
    private TokenManager tokenManager;
    private String rootToken;

    private static final Path testFolderPath = Path.fromString("/IntegrationTestFolder"
            + Integer.toString(Math.abs(new Random().nextInt()), 12));
    private static final Path testDocumentPath = testFolderPath.extend("testDocument.txt");
    private static final Path copiedTestDocumentPath = testFolderPath.extend("testDocument(1).txt");

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (ctx == null) {
            ctx = getApplicationContext(false, "backend/resource/resource.xml",
                    "backend/repository/repository.xml", "../standard-extensions/repo2/repo2.xml");
            repo2 = (Repository) ctx.getBean("repository");
            tokenManager = (TokenManager) ctx.getBean("tokenManager");
            rootToken = tokenManager.getRegisteredToken(new PrincipalImpl("root@localhost",
                    Type.PSEUDO));
        }
    }

    public void testRetrieveRoot() throws Exception {
        Resource root = repo2.retrieve(rootToken, Path.ROOT, true);
        assertNotNull("No root object exists", root);
        List<Property> properties = root.getProperties();
        assertTrue("Root object has no properties", properties != null && properties.size() > 0);
        assertNotNull("Root object has no acl associated with it", root.getAcl());
    }

    public void testCreateCollection() throws Exception {
        Resource testFolder = repo2.createCollection(rootToken, testFolderPath);
        assertNotNull("No testfolder created", testFolder);
    }

    public void testCreateDocument() throws Exception {
        Resource testDocument = repo2.createDocument(rootToken, testDocumentPath);
        String testDocumentContents = "This is a test document";
        repo2.storeContent(rootToken, testDocumentPath, new ByteArrayInputStream(testDocumentContents.getBytes()));
        assertNotNull("No testdocument created", testDocument);
    }

    public void testCopy() throws Exception {
        repo2.copy(rootToken, testDocumentPath, copiedTestDocumentPath, Depth.ZERO, false, false);
        Resource copiedResource = repo2.retrieve(rootToken, copiedTestDocumentPath, true);
        assertNotNull("Resource was not copied", copiedResource);
    }

    public void testDelete() throws Exception {
        repo2.delete(rootToken, testFolderPath);
        Resource shouldBeDeleted = null;
        try {
            shouldBeDeleted = repo2.retrieve(rootToken, testFolderPath, true);
            fail();
        } catch (ResourceNotFoundException e) {
            assertNull("What the ...", shouldBeDeleted);
        }
    }

}
