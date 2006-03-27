package org.vortikal.repositoryimpl.dao;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;

public class SimpleFileSystemContentStoreTestCase extends AbstractContentStoreTestCase {

    private ContentStore store;

    private String tmpDir;

    private static Log logger = LogFactory.getLog(SimpleFileSystemContentStoreTestCase.class);

    protected void setUp() throws Exception {
        BasicConfigurator.configure();
        super.setUp();
        SimpleFileSystemContentStore store = new SimpleFileSystemContentStore();
        tmpDir = System.getProperty("java.io.tmpdir") + "/contentStore" + getRandomIntAsString();
        
        File tmpDirFile = new File(tmpDir);
        tmpDirFile.mkdir();
        store.setRepositoryDataDirectory(tmpDir);
        setStore(store);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        
    }
    
    private String getRandomIntAsString() {
        Random generator = new Random(Calendar.getInstance().getTimeInMillis());
        return String.valueOf(generator.nextInt());
    }

    public ContentStore getStore() {
        return store;
    }

    public void setStore(ContentStore store) {
        this.store = store;
    }
    
    
    public void testCreateResource() throws IOException {
        String uri = "/test.html";
        getStore().createResource(uri, false);
        InputStream is = getStore().getInputStream(uri);
        assertNotNull(is);
        is.close();
        assertEquals(0, getStore().getContentLength(uri));
    }

    public void testStoreFileContentAndRetrieve() throws IOException {
        String uri = "/test.html";
        getStore().createResource(uri, false);
        String testString = "This is a test æøå ÆØÅ";

        InputStream inputStreamBeforeStoringContent = getStore().getInputStream(uri);
        assertNotNull(inputStreamBeforeStoringContent);
        inputStreamBeforeStoringContent.close();
        assertEquals(0, getStore().getContentLength(uri));

        ByteArrayInputStream inputStreamForStoringContent = 
            new ByteArrayInputStream(testString.getBytes());
        
        getStore().createResource(uri, false);

        getStore().storeContent(uri, inputStreamForStoringContent);
        inputStreamForStoringContent.close();

        assertEquals(testString.getBytes().length, getStore().getContentLength(uri));
        InputStream inputStreamAfterStoringContent = getStore().getInputStream(uri);
        assertNotNull(inputStreamAfterStoringContent);
        // Write some assertions to make sure the content of the file is ok
        inputStreamAfterStoringContent.close();

        getStore().deleteResource(uri);
        //assertEquals(0, getStore().getContentLength(uri));

        try {
            InputStream inputStreamAfterDelete = getStore().getInputStream(uri);
            fail("file does not exists");
        } catch (FileNotFoundException e) {
            // ok
        }
    }
    
    
    

}
