package org.vortikal.repositoryimpl.dao;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import junit.framework.TestCase;

import org.vortikal.repository.IllegalOperationException;

/**
 * Test case for <code>org.vortikal.repositoryimpl.dao.MemoryContentStore</code> 
 * implementation.
 * 
 * @author oyviste
 *
 */
public class MemoryContentStoreTestCase extends TestCase {

    private MemoryContentStore store;

    protected void setUp() throws Exception {
        super.setUp();
        store = new MemoryContentStore();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test initial state of content store after it has been instantiated.
     */
    public void testInitialState() throws IOException {
        // Check that the content store has the root node created
        assertEquals(true, store.exists("/"));

        // Check that the root node is a directory
        assertEquals(true, store.isCollection("/"));

    }

    /*
     * Test method for
     * 'org.vortikal.repositoryimpl.dao.MemoryContentStore.createResource(String,
     * boolean)'
     */
    public void testCreateResource() throws IOException {

        // Create a test directory
        store.createResource("/test", true);

        // Check if directory now exists, and is a directory.
        assertTrue(store.exists("/test"));
        assertTrue(store.isCollection("/test"));

        // Now create a valid content resource
        store.createResource("/test/empty-file.txt", false);
        assertTrue(store.exists("/test/empty-file.txt"));
        assertFalse(store.isCollection("/test/empty-file.txt"));

        // Create a new resource under invalid parent
        try {
            store.createResource("/non-existant-parent/new-resource.txt",
                            false);
            fail("Expected IOException when creating new node under non-existing parent.");
        } catch (IOException io) {
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

        store.createResource("/file.txt", false);
        store.storeContent("/file.txt", new ByteArrayInputStream(content));

        // Test that content length is correct
        assertEquals(content.length, store.getContentLength("/file.txt"));

        // Test that content length on directory throws
        // IllegalOperationException
        store.createResource("/dir", true);
        try {
            store.getContentLength("/dir");
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
        store.createResource("/short-lived-file.txt", false);

        assertEquals(true, store.exists("/short-lived-file.txt")
                && !store.isCollection("/short-lived-file.txt"));

        // Now delete file ..
        store.deleteResource("/short-lived-file.txt");

        // .. and test that it does not exist anymore.
        assertEquals(false, store.exists("/short-lived-file.txt"));

        // Create two small sub-directories
        store.createResource("/a", true);
        store.createResource("/a/b", true);
        store.createResource("/a/b/file1.txt", false);
        store.createResource("/a/b/file2.txt", false);
        store.createResource("/a/b/file3.txt", false);

        store.createResource("/foo-file.bar", false);

        // Delete subtree '/a/b'
        store.deleteResource("/a/b");

        // Check that subtree does not exist (or any if its sub-nodes)
        assertEquals(false, store.exists("/a/b"));
        assertEquals(false, store.exists("/a/b/file1.txt"));
        assertEquals(false, store.exists("/a/b/file2.txt"));
        assertEquals(false, store.exists("/a/b/file3.txt"));

        store.deleteResource("/a");

        assertEquals(false, store.exists("/a"));
        assertEquals(true, store.exists("/foo-file.bar"));

        // Check that root node cannot be deleted
        store.deleteResource("/");
        assertEquals(true, store.exists("/") && store.isCollection("/"));

    }

    /*
     * Test method for
     * 'org.vortikal.repositoryimpl.dao.MemoryContentStore.getInputStream(String)'
     */
    public void testGetInputStreamAndStoreContent() throws IOException {
        
        String testString = "I AM A CONTENT STRING";
        byte[] content = testString.getBytes();
        
        // Create a node and store some content in it
        store.createResource("/content.msg", false);
        store.storeContent("/content.msg", new ByteArrayInputStream(content));
        
        // Verify length
        InputStream input = store.getInputStream("/content.msg");
        assertNotNull(input);
        
        byte[] content2 = getContent(input);
        
        // Check that received content is what we originally stored
        assertTrue(equals(content, content2));
        
        // Copy content and test again
        store.copy("/content.msg", "/content2.msg");
        
        // Truncate original file (make sure the copy is a real clone)
        store.storeContent("/content.msg", new ByteArrayInputStream("".getBytes()));
        assertEquals(0, store.getContentLength("/content.msg"));
        
        input = store.getInputStream("/content2.msg");
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
        store.createResource("/a", true);
        store.createResource("/a/b", true);
        store.createResource("/a/b/file1.txt", false);
        store.createResource("/a/b/file2.txt", false);
        store.createResource("/a/b/file3.txt", false);
        store.createResource("/d", true);
        store.createResource("/d/e", true);
        store.createResource("/d/file4.txt", false);
        
        // Insert some content
        store.storeContent("/a/b/file1.txt", new ByteArrayInputStream(contentFile1));
        store.storeContent("/a/b/file2.txt", new ByteArrayInputStream(contentFile2));
        store.storeContent("/a/b/file3.txt", new ByteArrayInputStream(contentFile3));

        // Copy subtree '/d' to '/a/d', then check consistency      
        store.copy("/d", "/a/d");
        assertTrue(store.exists("/a/d"));
        assertTrue(store.isCollection("/a/d"));
        assertTrue(store.exists("/a/d/e"));
        assertTrue(store.isCollection("/a/d/e"));
        assertTrue(store.exists("/a/d/file4.txt"));
        assertFalse(store.isCollection("/a/d/file4.txt"));

        // Delete subtree '/d'
        store.deleteResource("/d");
        assertFalse(store.exists("/d"));
        
        // Rename/move subtree '/a/b' to '/a/x'
        store.copy("/a/b", "/a/x");
        store.deleteResource("/a/b");
        
        // Check consistency of entire '/a' subtree
        assertTrue(store.exists("/a"));
        assertTrue(store.isCollection("/a"));
        assertTrue(store.exists("/a/x"));
        assertTrue(store.isCollection("/a/x"));
        assertTrue(store.exists("/a/x/file1.txt"));
        assertFalse(store.isCollection("/a/x/file1.txt"));
        assertTrue(store.exists("/a/x/file2.txt"));
        assertFalse(store.isCollection("/a/x/file2.txt"));
        assertTrue(store.exists("/a/x/file3.txt"));
        assertFalse(store.isCollection("/a/x/file3.txt"));
        assertTrue(store.exists("/a/d"));
        assertTrue(store.isCollection("/a/d"));
        assertTrue(store.exists("/a/d/e"));
        assertTrue(store.isCollection("/a/d/e"));
        assertTrue(store.exists("/a/d/file4.txt"));
        assertFalse(store.isCollection("/a/d/file4.txt"));

        // Verify content
        byte[] content = getContent(store.getInputStream("/a/x/file1.txt"));
        assertTrue(equals(contentFile1, content));
        content = getContent(store.getInputStream("/a/x/file2.txt"));
        assertTrue(equals(contentFile2, content));
        content = getContent(store.getInputStream("/a/x/file3.txt"));
        assertTrue(equals(contentFile3, content));
        
        content = getContent(store.getInputStream("/a/d/file4.txt"));
        assertTrue(equals(new byte[0], content));
                
        // Rename '/a' subtree, then re-check consistency
        store.copy("/a", "/Copy of a");
        store.deleteResource("/a");
        assertFalse(store.exists("/a"));
        
        // Check consistency of entire '/Copy of a' subtree
        assertTrue(store.exists("/Copy of a"));
        assertTrue(store.isCollection("/Copy of a"));
        assertTrue(store.exists("/Copy of a/x"));
        assertTrue(store.isCollection("/Copy of a/x"));
        assertTrue(store.exists("/Copy of a/x/file1.txt"));
        assertFalse(store.isCollection("/Copy of a/x/file1.txt"));
        assertTrue(store.exists("/Copy of a/x/file2.txt"));
        assertFalse(store.isCollection("/Copy of a/x/file2.txt"));
        assertTrue(store.exists("/Copy of a/x/file3.txt"));
        assertFalse(store.isCollection("/Copy of a/x/file3.txt"));
        assertTrue(store.exists("/Copy of a/d"));
        assertTrue(store.isCollection("/Copy of a/d"));
        assertTrue(store.exists("/Copy of a/d/e"));
        assertTrue(store.isCollection("/Copy of a/d/e"));
        assertTrue(store.exists("/Copy of a/d/file4.txt"));
        assertFalse(store.isCollection("/Copy of a/d/file4.txt"));

        // Verify content
        content = getContent(store.getInputStream("/Copy of a/x/file1.txt"));
        assertTrue(equals(contentFile1, content));
        content = getContent(store.getInputStream("/Copy of a/x/file2.txt"));
        assertTrue(equals(contentFile2, content));
        content = getContent(store.getInputStream("/Copy of a/x/file3.txt"));
        assertTrue(equals(contentFile3, content));
        
        content = getContent(store.getInputStream("/Copy of a/d/file4.txt"));
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
        int numWorkers = 100;
        
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
                    store.createResource(workdir, true);
                    store.createResource(workdir + "/a", true);
                    store.createResource(workdir + "/a/AN_EMPTY_FILE.dat", false);
                    store.createResource(workdir + "/a/AN_EMPTY_FILE2.dat", false);
                    
                    store.createResource(workdir + "/worker_name.txt", false);
                    store.storeContent(workdir + "/worker_name.txt", new ByteArrayInputStream(name.getBytes()));
                    
                    store.copy(workdir + "/a", workdir + "/Copy of a (1)");
                    store.copy(workdir + "/a", workdir + "/Copy of a (2)");
                    
                    store.copy(workdir + "/a", workdir + "/x");
                    store.deleteResource(workdir + "/a");
                    store.deleteResource(workdir + "/Copy of a (1)");
                    store.deleteResource(workdir + "/Copy of a (2)");
                    
                    store.copy(workdir + "/worker_name.txt", workdir + "/name.txt");
                    store.deleteResource(workdir + "/worker_name.txt");
                    
                } catch (IOException io) {
                    fail("Un-expected IOException while working in '" + workdir + "': " + io.getMessage());
                    System.exit(1);
                }
            }
        }
        
        // Set up store, create one subtree for workers, and one off-limit tree
        store.createResource("/workers_play_area", true);
        store.createResource("/off_limits", true);
        store.createResource("/off_limits/i_will_survive.txt", false);
        store.storeContent("/off_limits/i_will_survive.txt", 
                new ByteArrayInputStream("I Will Surviveeee !".getBytes()));
        
        // Create the worker threads, add them all to a single thread group
        Thread[] threads = new Thread[numWorkers];
        ThreadGroup group = new ThreadGroup("workers");
        for (int i=0; i<numWorkers; i++) {
            Worker worker = new Worker("Worker # " + i, 
                            "/workers_play_area/worker" + i, store);
            
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
        assertTrue(store.exists("/off_limits"));
        assertTrue(store.isCollection("/off_limits"));
        assertTrue(store.exists("/off_limits/i_will_survive.txt"));
        assertFalse(store.isCollection("/off_limits/i_will_survive.txt"));
        byte[] originalContent = "I Will Surviveeee !".getBytes();
        byte[] content = getContent(store.getInputStream("/off_limits/i_will_survive.txt"));
        assertTrue(equals(originalContent, content));
        
        // Verify all worker areas in content store (should conform to specific pattern)
        for (int i=0; i<numWorkers; i++) {
            assertTrue(store.exists("/workers_play_area/worker" + i));
            assertTrue(store.isCollection("/workers_play_area/worker" + i));
            
            assertTrue(store.exists("/workers_play_area/worker" + i + "/name.txt"));
            assertFalse(store.isCollection("/workers_play_area/worker" + i + "/name.txt"));
            content = getContent(store.getInputStream("/workers_play_area/worker" + i + "/name.txt"));
            assertTrue(equals(content, ("Worker # " + i).getBytes()));
            
            assertFalse(store.exists("/workers_play_area/worker" + i + "/a"));
            
            assertTrue(store.exists("/workers_play_area/worker" + i + "/x"));
            assertTrue(store.isCollection("/workers_play_area/worker" + i + "/x"));
            assertTrue(store.exists("/workers_play_area/worker" + i + "/x/AN_EMPTY_FILE.dat"));
            assertTrue(store.exists("/workers_play_area/worker" + i + "/x/AN_EMPTY_FILE2.dat"));
            assertFalse(store.isCollection("/workers_play_area/worker" + i + "/x/AN_EMPTY_FILE2.dat"));
            assertFalse(store.isCollection("/workers_play_area/worker" + i + "/x/AN_EMPTY_FILE2.dat"));
        }
    }
}
