package org.vortikal.repositoryimpl.dao;

import org.apache.log4j.BasicConfigurator;

public class MemoryContentStoreTestCase extends AbstractContentStoreTestCase {

    private MemoryContentStore store;

    protected void setUp() throws Exception {
        BasicConfigurator.configure();
        super.setUp();
        store = new MemoryContentStore();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public ContentStore getStore() {
        return store;
    }

}
