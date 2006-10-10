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

package org.vortikal.repositoryimpl.index.lucene;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;


/**
 * File system backed Lucene index.
 * @author oyviste
 */
public class FSBackedLuceneIndex extends AbstractLuceneIndex 
    implements InitializingBean {
    
    private static final Log logger = LogFactory.getLog(FSBackedLuceneIndex.class);
    
    public void afterPropertiesSet() throws BeanInitializationException {
        if (this.storageRootPath == null) {
            throw new BeanInitializationException("Required property 'storageRootPath' not set.");
        } else if (this.storageId == null) {
            throw new BeanInitializationException("Required property 'storageId' not set.");
        }
        
        try {
            this.storageDirectory = initializeStorageDirectory(this.storageRootPath,
                                                               this.storageId);
        } catch (IOException io) {
            throw new BeanInitializationException("IOException while initializing storage directory", io);
        }
        
        super.afterPropertiesSet();
    }
    
    private String storageRootPath;
    private String storageId;
    private File storageDirectory;
    
    /** Creates a new instance of FSBackedStandardLuceneIndex */
    public FSBackedLuceneIndex() {
    }
    
    /**
     * Create fs directory. One instance of this is created at startup
     * and whenever an index is re-initialized or re-created.
     */
    protected Directory createDirectory(boolean eraseContents) throws IOException {
        logger.debug("Initializing index directory at path '" 
                                                + this.storageDirectory.getAbsolutePath() + "'");
        
        return FSDirectory.getDirectory(this.storageDirectory, eraseContents);
    }
    
    private File initializeStorageDirectory(String storageRootPath, String storageId)
            throws IOException {

        File storageDirectory = new File(storageRootPath, storageId);

        if (storageDirectory.isDirectory()) {
            if (!storageDirectory.canWrite()) {
                throw new IOException("Resolved storage directory '"
                        + storageDirectory.getAbsolutePath()
                        + "' is not writable");
            }
        } else if (storageDirectory.isFile()) {
            throw new IOException("Resolved storage directory '"
                    + storageDirectory.getAbsolutePath() + "' is a file");
        } else {
            // Directory does not exist, we need to create it.
            if (!storageDirectory.mkdir()) {
                throw new IOException(
                        "Failed to create resolved storage directory '"
                                + storageDirectory.getAbsolutePath() + "'");
            }
        }

        return storageDirectory;
    }

    public long getIndexByteSize() throws IOException {
        long length = 0;
        File[] contents = this.storageDirectory.listFiles();
        for (int i=0; i<contents.length; i++) {
            if (contents[i].isFile()) length += contents[i].length();
        }
        return length;
    }

    public File getStorageDirectory() {
        return this.storageDirectory;
    }

    public void setStorageId(String storageId) {
        this.storageId = storageId;
    }

    public void setStorageRootPath(String storageRootPath) {
        this.storageRootPath = storageRootPath;
    }

}
