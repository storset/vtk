package org.vortikal.util.text;


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
public class DefaultStructuredTextTestCase extends TestCase {

    private DefaultStructuredText dst; 
    
    protected void setUp() throws Exception {
        super.setUp();
        dst = new DefaultStructuredText();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test of potential outofbounds.
     */
    public void testEndsWithUnfinishedLink() {
        try {
            dst.parseStructuredText("lala lala \"lala\":");
        } catch (StringIndexOutOfBoundsException e) {
            fail(e.getMessage());
        }
    }
}
