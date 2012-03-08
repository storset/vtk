/* Copyright (c) 2005, University of Oslo, Norway
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

package org.vortikal.repository.index;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;


/**
 * File system backed Lucene index.
 */
public class FSBackedLuceneIndex extends AbstractLuceneIndex {
    
    private static final Log logger = LogFactory.getLog(FSBackedLuceneIndex.class);
    
    /**
     * Absolute path to existing index storage directory on local file system.
     */
    private File storageDirectory;
    
    public FSBackedLuceneIndex(File storageDirectory, 
                               Analyzer analyzer, 
                               boolean forceUnlock) throws IOException {
        super(analyzer, forceUnlock);
        this.storageDirectory = storageDirectory;
    }
    
    /**
     * Create fs directory. One instance of this is created at startup
     * and whenever an index is re-initialized or re-created.
     */
    @Override
    protected Directory createDirectory() throws IOException {
        logger.debug("Initializing index storage directory at path '" 
                + this.storageDirectory.getAbsolutePath() + "'");

        if (! this.storageDirectory.isDirectory()) {
            throw new IOException("Storage directory path '" 
                    + this.storageDirectory.getAbsolutePath() + "' is not a directory.");
        } else if (! this.storageDirectory.canWrite()) {
            throw new IOException("Storage directory path '" 
                    + this.storageDirectory.getAbsolutePath() + "' is not writable.");
        }

        // Picks default best Impl at runtime based on current system.
        // For all but Windows, this means NIOFSDirectory.
        return FSDirectory.open(this.storageDirectory);
    }
    
    /**
     * Return total physical size of index on disk in bytes.
     */
    public long getIndexSizeInBytes() throws IOException {
        long length = 0;
        File[] contents = this.storageDirectory.listFiles();
        for (int i=0; i<contents.length; i++) {
            if (contents[i].isFile()) length += contents[i].length();
        }
        return length;
    }
    
    /**
     * Do a simple test for index corruption. This method will re-initialize the index and
     * iterate over all existing documents.
     * 
     * An <code>IOException</code> is thrown if corruption is detected.
     * 
     * @throws IOException if corruption is detected.
     */
    public void corruptionTest() throws IOException {
        super.reinitialize();
        
        IndexReader reader = super.getIndexReader();
        
        for (int i=0; i<reader.maxDoc(); i++) {
            if (reader.isDeleted(i)) continue;
            reader.document(i);
        }
        
        super.commit(); // Close reader
    }

    public File getStorageDirectory() {
        return this.storageDirectory;
    }

}
