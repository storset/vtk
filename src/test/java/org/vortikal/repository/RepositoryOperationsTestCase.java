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

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalImpl;
import org.vortikal.util.io.StreamUtil;


public class RepositoryOperationsTestCase extends AbstractRepositoryTestCase {

    public void testRetrieve() throws Exception {
        Path uri = Path.fromString("/");
        Principal root = new PrincipalImpl("root@localhost", Principal.Type.USER);
        String token = getTokenManager().getRegisteredToken(root);
        Resource res = getRepository().retrieve(token, uri, true);
        assertNotNull(res);
    }
    

    public void testCreateDocumentNoParent() throws Exception {
        Path uri = Path.fromString("/non-existing-collection/create-document-test.txt");
        Principal root = new PrincipalImpl("root@localhost", Principal.Type.USER);
        String token = getTokenManager().getRegisteredToken(root);
        
        try {
            getRepository().createDocument(token, uri);
            fail("Should not be allowed to create a resource in a non-existing collection");
        } catch (Exception e) {
            
        }
    }

    public void testCreateDocument() throws Exception {
        Path uri = Path.fromString("/create-document-test.txt");
        Principal root = new PrincipalImpl("root@localhost", Principal.Type.USER);
        String token = getTokenManager().getRegisteredToken(root);
        
        getRepository().createDocument(token, uri);
        String content = "This is my content";
        InputStream inStream = StreamUtil.stringToStream(content, "utf-8");
        getRepository().storeContent(token, uri, inStream);
        
        InputStream resultStream = getRepository().getInputStream(token, uri, true);
        String result = new String(StreamUtil.readInputStream(resultStream), "utf-8");

        assertEquals(content, result);
    }


    public void testCreateCollectionNoParent() throws Exception {
        Path uri = Path.fromString("/non-existing-collection/create-collection-test");
        Principal root = new PrincipalImpl("root@localhost", Principal.Type.USER);
        String token = getTokenManager().getRegisteredToken(root);
        
        try {
            getRepository().createCollection(token, uri);
            fail("Should not be allowed to create a resource in a non-existing collection");
        } catch (Exception e) {
            
        }
    }


    public void testCreateCollection() throws Exception {
        Principal root = new PrincipalImpl("root@localhost", Principal.Type.USER);
        String token = getTokenManager().getRegisteredToken(root);
        Path uri = Path.fromString("/create-collection-test");
        
        Resource res = getRepository().createCollection(token, uri);
        assertNotNull(res);
        Resource retrieved = getRepository().retrieve(token, uri, true);
        assertNotNull(retrieved);
        assertEquals(res, retrieved);
    }


    public void testListChildren() throws Exception {
        Principal root = new PrincipalImpl("root@localhost", Principal.Type.USER);
        String token = getTokenManager().getRegisteredToken(root);
        Path uri = Path.fromString("/create-collection-test");
        int numChildren = 10;
        Set<Path> childUriSet = new HashSet<Path>();
        getRepository().createCollection(token, uri);
        for (int i = 0; i < numChildren; i++) {
            Path childUri = uri.extend("child-" + i);
            childUriSet.add(childUri);
            getRepository().createCollection(token, childUri);
        }
        
        Resource[] children = getRepository().listChildren(token, uri, true);
        assertNotNull(children);
        assertEquals(children.length, numChildren);
        
        for (int i = 0; i < numChildren; i++) {
            assertTrue(childUriSet.contains(children[i].getURI()));
        }
    }
    

    public void testChangeAclInheritance() throws Exception {
        Path parentURI = Path.fromString("/parent");
        Path childURI = parentURI.extend("child.txt");

        Repository repo = getRepository();
        Principal root = new PrincipalImpl("root@localhost", Principal.Type.USER);
        String token = getTokenManager().getRegisteredToken(root);

        Resource parent = repo.createCollection(token, parentURI);
        assertTrue(parent.isInheritedAcl());

        Resource child = repo.createDocument(token, childURI);
        assertTrue(child.isInheritedAcl());

        parent.setInheritedAcl(false);
        repo.storeACL(token, parent);
        
        parent = repo.retrieve(token, parentURI, true);
        parent.getAcl();
        assertFalse(parent.isInheritedAcl());

        child = repo.retrieve(token, childURI, true);
    }
}

