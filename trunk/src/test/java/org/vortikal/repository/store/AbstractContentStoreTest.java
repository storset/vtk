/* Copyright (c) 2006, 2007, University of Oslo, Norway
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
package org.vortikal.repository.store;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import junit.framework.TestCase;

import org.vortikal.repository.IllegalOperationException;
import org.vortikal.repository.Path;

/**
 * Test case for <code>org.vortikal.repositoryimpl.dao.MemoryContentStore</code> 
 * implementation.
 * 
 * @author oyviste
 *
 */
public abstract class AbstractContentStoreTest extends TestCase {

    //private MemoryContentStore store;

//    protected void setUp() throws Exception {
//        super.setUp();
//        store = new MemoryContentStore();
//    }
//
//    protected void tearDown() throws Exception {
//        super.tearDown();
//    }

    public abstract ContentStore getStore();

    /*
     * Test method for
     * 'org.vortikal.repositoryimpl.dao.MemoryContentStore.createResource(String,
     * boolean)'
     */
    public void testCreateResource() throws IOException {

        // Create a test directory
        getStore().createResource(Path.fromString("/test"), true);

        // Now create a valid content resource
        getStore().createResource(Path.fromString("/test/empty-file.txt"), false);

        // Create a new resource under invalid parent
        try {
            getStore().createResource(Path.fromString("/non-existant-parent/new-resource.txt"),
                            false);
            fail("Expected Exception when creating new node under non-existing parent.");
        } catch (Exception e) {
            // OK
        }
    }

    /*
     * Test method for
     * 'org.vortikal.repositoryimpl.dao.MemoryContentStore.getContentLength(String)'
     */
    public void testGetContentLength() throws IOException {
        String testString = "HELLO WORLD, THIS IS CONTENT";
        byte[] content = testString.getBytes();

        getStore().createResource(Path.fromString("/file.txt"), false);
        getStore().storeContent(Path.fromString("/file.txt"), new ByteArrayInputStream(content));

        // Test that content length is correct
        assertEquals(content.length, getStore().getContentLength(Path.fromString("/file.txt")));

        // Test that content length on directory throws
        // IllegalOperationException
        getStore().createResource(Path.fromString("/dir"), true);
        try {
            getStore().getContentLength(Path.fromString("/dir"));
            fail("Expected IllegalOperationException when trying to get content length of directory");
        } catch (IllegalOperationException ioe) {
            // OK
        }

    }

    /*
     * Test method for
     * 'org.vortikal.repositoryimpl.dao.MemoryContentStore.deleteResource(String)'
     */
    public void testDeleteResource() throws IOException {
        // Test delete of simple file
        getStore().createResource(Path.fromString("/short-lived-file.txt"), false);

//         assertEquals(true, getStore().exists("/short-lived-file.txt")
//                 && !getStore().isCollection("/short-lived-file.txt"));

//         // Now delete file ..
//         getStore().deleteResource("/short-lived-file.txt");

//         // .. and test that it does not exist anymore.
//         assertEquals(false, getStore().exists("/short-lived-file.txt"));

//         // Create two small sub-directories
//         getStore().createResource("/a", true);
//         getStore().createResource("/a/b", true);
//         getStore().createResource("/a/b/file1.txt", false);
//         getStore().createResource("/a/b/file2.txt", false);
//         getStore().createResource("/a/b/file3.txt", false);

//         getStore().createResource("/foo-file.bar", false);

//         // Delete subtree '/a/b'
//         getStore().deleteResource("/a/b");

//         // Check that subtree does not exist (or any if its sub-nodes)
//         assertEquals(false, getStore().exists("/a/b"));
//         assertEquals(false, getStore().exists("/a/b/file1.txt"));
//         assertEquals(false, getStore().exists("/a/b/file2.txt"));
//         assertEquals(false, getStore().exists("/a/b/file3.txt"));

//         getStore().deleteResource("/a");

//         assertEquals(false, getStore().exists("/a"));
//         assertEquals(true, getStore().exists("/foo-file.bar"));

//         // Check that root node cannot be deleted
//         getStore().deleteResource("/");
//         assertEquals(true, getStore().exists("/") && getStore().isCollection("/"));

    }

    /*
     * Test method for
     * 'org.vortikal.repositoryimpl.dao.MemoryContentStore.getInputStream(String)'
     */
    public void testGetInputStreamAndStoreContent() throws IOException {
        
        String testString = "I AM A CONTENT STRING";
        byte[] content = testString.getBytes();
        
        Path p = Path.fromString("/content.msg"); 
        Path p2 = Path.fromString("/content2.msg"); 
        
        // Create a node and store some content in it
        getStore().createResource(p, false);
        getStore().storeContent(p, new ByteArrayInputStream(content));
        
        // Verify length
        InputStream input = getStore().getInputStream(p).getInputStream();
        assertNotNull(input);
        
        byte[] content2 = getContent(input);
        
        // Check that received content is what we originally stored
        assertTrue(equals(content, content2));
        
        // Copy content and test again
        getStore().copy(p, p2);
        
        // Truncate original file (make sure the copy is a real clone)
        getStore().storeContent(p, new ByteArrayInputStream("".getBytes()));
        assertEquals(0, getStore().getContentLength(p));
        
        input = getStore().getInputStream(p2).getInputStream();
        content2 = getContent(input);
        assertTrue(equals(content, content2));
        
    }
    
    /*
     * Test method for
     * 'org.vortikal.repositoryimpl.dao.MemoryContentStore.copy(String, String)'
     */
    public void testCopy() throws IOException {
        
        byte[] contentFile1 = "I'm the contents of file1.txt".getBytes();
        byte[] contentFile2 = "I'm the contents of file2.txt".getBytes();
        byte[] contentFile3 = "foo bar baz mik mak hey ho ØÆÅ øæå".getBytes();
        
        // Create content tree
        getStore().createResource(Path.fromString("/a"), true);
        getStore().createResource(Path.fromString("/a/b"), true);
        getStore().createResource(Path.fromString("/a/b/file1.txt"), false);
        getStore().createResource(Path.fromString("/a/b/file2.txt"), false);
        getStore().createResource(Path.fromString("/a/b/file3.txt"), false);
        getStore().createResource(Path.fromString("/d"), true);
        getStore().createResource(Path.fromString("/d/e"), true);
        getStore().createResource(Path.fromString("/d/file4.txt"), false);
        
        // Insert some content
        getStore().storeContent(Path.fromString("/a/b/file1.txt"), new ByteArrayInputStream(contentFile1));
        getStore().storeContent(Path.fromString("/a/b/file2.txt"), new ByteArrayInputStream(contentFile2));
        getStore().storeContent(Path.fromString("/a/b/file3.txt"), new ByteArrayInputStream(contentFile3));

//         // Copy subtree '/d' to '/a/d', then check consistency      
         getStore().copy(Path.fromString("/d"), Path.fromString("/a/d"));
//         assertTrue(getStore().exists("/a/d"));
//         assertTrue(getStore().isCollection("/a/d"));
//         assertTrue(getStore().exists("/a/d/e"));
//         assertTrue(getStore().isCollection("/a/d/e"));
//         assertTrue(getStore().exists("/a/d/file4.txt"));
//         assertFalse(getStore().isCollection("/a/d/file4.txt"));

//         // Delete subtree '/d'
//         getStore().deleteResource("/d");
//         assertFalse(getStore().exists("/d"));
        
//         // Rename/move subtree '/a/b' to '/a/x'
        getStore().move(Path.fromString("/a/b"), Path.fromString("/a/x"));
//         getStore().copy("/a/b", "/a/x");
//         getStore().deleteResource("/a/b");
        
//         // Check consistency of entire '/a' subtree
//         assertTrue(getStore().exists("/a"));
//         assertTrue(getStore().isCollection("/a"));
//         assertTrue(getStore().exists("/a/x"));
//         assertTrue(getStore().isCollection("/a/x"));
//         assertTrue(getStore().exists("/a/x/file1.txt"));
//         assertFalse(getStore().isCollection("/a/x/file1.txt"));
//         assertTrue(getStore().exists("/a/x/file2.txt"));
//         assertFalse(getStore().isCollection("/a/x/file2.txt"));
//         assertTrue(getStore().exists("/a/x/file3.txt"));
//         assertFalse(getStore().isCollection("/a/x/file3.txt"));
//         assertTrue(getStore().exists("/a/d"));
//         assertTrue(getStore().isCollection("/a/d"));
//         assertTrue(getStore().exists("/a/d/e"));
//         assertTrue(getStore().isCollection("/a/d/e"));
//         assertTrue(getStore().exists("/a/d/file4.txt"));
//         assertFalse(getStore().isCollection("/a/d/file4.txt"));

        // Verify content
        byte[] content = getContent(getStore().getInputStream(Path.fromString("/a/x/file1.txt")).getInputStream());
        assertTrue(equals(contentFile1, content));
        content = getContent(getStore().getInputStream(Path.fromString("/a/x/file2.txt")).getInputStream());
        assertTrue(equals(contentFile2, content));
        content = getContent(getStore().getInputStream(Path.fromString("/a/x/file3.txt")).getInputStream());
        assertTrue(equals(contentFile3, content));
        
        content = getContent(getStore().getInputStream(Path.fromString("/a/d/file4.txt")).getInputStream());
        assertTrue(equals(new byte[0], content));
                
        // Rename '/a' subtree, then re-check consistency
        getStore().copy(Path.fromString("/a"), Path.fromString("/Copy of a"));
        getStore().deleteResource(Path.fromString("/a"));
//         assertFalse(getStore().exists("/a"));
        
//         // Check consistency of entire '/Copy of a' subtree
//         assertTrue(getStore().exists("/Copy of a"));
//         assertTrue(getStore().isCollection("/Copy of a"));
//         assertTrue(getStore().exists("/Copy of a/x"));
//         assertTrue(getStore().isCollection("/Copy of a/x"));
//         assertTrue(getStore().exists("/Copy of a/x/file1.txt"));
//         assertFalse(getStore().isCollection("/Copy of a/x/file1.txt"));
//         assertTrue(getStore().exists("/Copy of a/x/file2.txt"));
//         assertFalse(getStore().isCollection("/Copy of a/x/file2.txt"));
//         assertTrue(getStore().exists("/Copy of a/x/file3.txt"));
//         assertFalse(getStore().isCollection("/Copy of a/x/file3.txt"));
//         assertTrue(getStore().exists("/Copy of a/d"));
//         assertTrue(getStore().isCollection("/Copy of a/d"));
//         assertTrue(getStore().exists("/Copy of a/d/e"));
//         assertTrue(getStore().isCollection("/Copy of a/d/e"));
//         assertTrue(getStore().exists("/Copy of a/d/file4.txt"));
//         assertFalse(getStore().isCollection("/Copy of a/d/file4.txt"));

        // Verify content
        content = getContent(getStore().getInputStream(Path.fromString("/Copy of a/x/file1.txt")).getInputStream());
        assertTrue(equals(contentFile1, content));
        content = getContent(getStore().getInputStream(Path.fromString("/Copy of a/x/file2.txt")).getInputStream());
        assertTrue(equals(contentFile2, content));
        content = getContent(getStore().getInputStream(Path.fromString("/Copy of a/x/file3.txt")).getInputStream());
        assertTrue(equals(contentFile3, content));
        
        content = getContent(getStore().getInputStream(Path.fromString("/Copy of a/d/file4.txt")).getInputStream());
        assertTrue(equals(new byte[0], content));
                

    }

    private boolean equals(byte[] content1, byte[] content2) {
        if (content1.length != content2.length) return false;
        for (int i=0; i< content1.length; i++) {
            if (content1[i] != content2[i]) return false;
        }
        
        return true;
    }

    private byte[] getContent(InputStream input) throws IOException {
        byte[] buffer = new byte[1000];
        int n;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        while ((n = input.read(buffer, 0, buffer.length)) != -1) {
            bout.write(buffer, 0, n);
        }
        
        return bout.toByteArray();
    }
    
    /*
     * Test access and modifications by multiple concurrent threads (important).
     */
    public void testMultithreadedAccessAndModification() throws IOException {
        
        // The number of threads to run concurrently against content store
        int numWorkers = 1000;
        
        final class Worker implements Runnable {
            ContentStore store;
            String name;
            String workdir;
            
            public Worker(String name, String workdir, ContentStore store) {
                this.store = store;
                this.name = name;
                this.workdir = workdir;
            }
            
            public void run() {
                // Create a small structure under workdir, insert thread name
                // into a file, and generally mess about ..
                
                // Sleep a random amount of time before starting
                try {
                    Thread.sleep((long)Math.random()*500);
                } catch (InterruptedException ie) {}
                
                try {
                    this.store.createResource(Path.fromString(this.workdir), true);
                    this.store.createResource(Path.fromString(this.workdir + "/a"), true);
                    this.store.createResource(Path.fromString(this.workdir + "/a/AN_EMPTY_FILE.dat"), false);
                    this.store.createResource(Path.fromString(this.workdir + "/a/AN_EMPTY_FILE2.dat"), false);
                    
                    this.store.createResource(Path.fromString(this.workdir + "/worker_name.txt"), false);
                    this.store.storeContent(Path.fromString(this.workdir + "/worker_name.txt"), new ByteArrayInputStream(this.name.getBytes()));
                    
                    this.store.copy(Path.fromString(this.workdir + "/a"), Path.fromString(this.workdir + "/Copy of a (1)"));
                    this.store.copy(Path.fromString(this.workdir + "/a"), Path.fromString(this.workdir + "/Copy of a (2)"));
                    
                    this.store.copy(Path.fromString(this.workdir + "/a"), Path.fromString(this.workdir + "/x"));
                    this.store.deleteResource(Path.fromString(this.workdir + "/a"));
                    this.store.deleteResource(Path.fromString(this.workdir + "/Copy of a (1)"));
                    this.store.deleteResource(Path.fromString(this.workdir + "/Copy of a (2)"));
                    
                    this.store.copy(Path.fromString(this.workdir + "/worker_name.txt"), Path.fromString(this.workdir + "/name.txt"));
                    this.store.deleteResource(Path.fromString(this.workdir + "/worker_name.txt"));
                    
                } catch (Exception io) {
                    fail("Un-expected Exception while working in '" + this.workdir + "': " + io.getMessage());
                }
            }
        }
        
        // Set up store, create one subtree for workers, and one off-limit tree
        getStore().createResource(Path.fromString("/workers_play_area"), true);
        getStore().createResource(Path.fromString("/off_limits"), true);
        getStore().createResource(Path.fromString("/off_limits/i_will_survive.txt"), false);
        getStore().storeContent(Path.fromString("/off_limits/i_will_survive.txt"), 
                new ByteArrayInputStream("I Will Surviveeee !".getBytes()));
        
        // Create the worker threads, add them all to a single thread group
        Thread[] threads = new Thread[numWorkers];
        ThreadGroup group = new ThreadGroup("workers");
        for (int i=0; i<numWorkers; i++) {
            Worker worker = new Worker("Worker # " + i, 
                            "/workers_play_area/worker" + i, getStore());
            
            threads[i] = new Thread(group, worker, worker.name);
            
        }
        
        // Start all threads concurrently, as fast as possible
        for (int i=0; i<numWorkers; i++) {
            threads[i].start();
        }
        
        // Wait for all threads to finish the work
        for (int i=0; i<numWorkers; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException ie) {}
        }
        
        // Assert that all threads are in fact finished.
        assertEquals(0, group.activeCount());
        
        // Verify content store structures:
        // Verify off-limits subtree
//         assertTrue(getStore().exists("/off_limits"));
//         assertTrue(getStore().isCollection("/off_limits"));
//         assertTrue(getStore().exists("/off_limits/i_will_survive.txt"));
//         assertFalse(getStore().isCollection("/off_limits/i_will_survive.txt"));
        byte[] originalContent = "I Will Surviveeee !".getBytes();
        byte[] content = getContent(getStore().getInputStream(Path.fromString("/off_limits/i_will_survive.txt")).getInputStream());
        assertTrue(equals(originalContent, content));
        
        // Verify all worker areas in content store (should conform to specific pattern)
        for (int i=0; i<numWorkers; i++) {
//             assertTrue(getStore().exists("/workers_play_area/worker" + i));
//             assertTrue(getStore().isCollection("/workers_play_area/worker" + i));
            
//             assertTrue(getStore().exists("/workers_play_area/worker" + i + "/name.txt"));
//             assertFalse(getStore().isCollection("/workers_play_area/worker" + i + "/name.txt"));
//             content = getContent(getStore().getInputStream("/workers_play_area/worker" + i + "/name.txt"));
//             assertTrue(equals(content, ("Worker # " + i).getBytes()));
            
//             assertFalse(getStore().exists("/workers_play_area/worker" + i + "/a"));
            
//             assertTrue(getStore().exists("/workers_play_area/worker" + i + "/x"));
//             assertTrue(getStore().isCollection("/workers_play_area/worker" + i + "/x"));
//             assertTrue(getStore().exists("/workers_play_area/worker" + i + "/x/AN_EMPTY_FILE.dat"));
//             assertTrue(getStore().exists("/workers_play_area/worker" + i + "/x/AN_EMPTY_FILE2.dat"));
//             assertFalse(getStore().isCollection("/workers_play_area/worker" + i + "/x/AN_EMPTY_FILE2.dat"));
//             assertFalse(getStore().isCollection("/workers_play_area/worker" + i + "/x/AN_EMPTY_FILE2.dat"));
        }
    }
}
