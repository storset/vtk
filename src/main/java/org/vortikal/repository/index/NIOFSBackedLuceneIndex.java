package org.vortikal.repository.index;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;

public class NIOFSBackedLuceneIndex extends AbstractLuceneIndex {
	
	/*
	 * @see https://issues.apache.org/jira/browse/LUCENE-1451
	 * 
	 * Proper fix for this is available in Lucene 2.9
	 */
    static {
    	System.setProperty("org.apache.lucene.FSDirectory.class", NIOFSDirectory.class.getName());
    }
        
    private File storageDirectory;
    
    public NIOFSBackedLuceneIndex(File storageDirectory, 
                               Analyzer analyzer, 
                               boolean eraseExistingIndex,
                               boolean forceUnlock) throws IOException {
        super(analyzer, eraseExistingIndex, forceUnlock);
        this.storageDirectory = storageDirectory;
    }
    
	@Override
	protected Directory createDirectory() throws IOException {
		
        if (! this.storageDirectory.isDirectory()) {
            throw new IOException("Storage directory path '" 
                    + this.storageDirectory.getAbsolutePath() + "' is not a directory.");
        } else if (! this.storageDirectory.canWrite()) {
            throw new IOException("Storage directory path '" 
                    + this.storageDirectory.getAbsolutePath() + "' is not writable.");
        }
		
		return NIOFSDirectory.getDirectory(this.storageDirectory);
	}
	
	
    public void corruptionTest() throws IOException {
        super.reinitialize();
        
        IndexReader reader = super.getIndexReader();
        
        for (int i=0; i<reader.maxDoc(); i++) {
            if (reader.isDeleted(i)) continue;
            reader.document(i);
        }
        
        super.commit(); // Close reader
    }
}
