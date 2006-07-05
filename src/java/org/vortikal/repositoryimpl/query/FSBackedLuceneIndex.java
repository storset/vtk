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

package org.vortikal.repositoryimpl.query;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;


/**
 * File system backed Lucene index.
 * @author oyviste
 */
public class FSBackedLuceneIndex extends AbstractLuceneIndex {
    
    private static final Log logger = LogFactory.getLog(FSBackedLuceneIndex.class);
    
    /**
     * Path to index directory on local file system.
     */
    private String indexPath;
    
    /** Creates a new instance of FSBackedStandardLuceneIndex */
    public FSBackedLuceneIndex(String indexPath, 
                               Analyzer analyzer, 
                               boolean eraseExistingIndex,
                               boolean forceUnlock) throws IOException {
        
        super(analyzer, eraseExistingIndex, forceUnlock);
        
        this.indexPath = indexPath;
        
        super.initialize();
    }
    
    /**
     * Create fs directory. One instance of this is created at startup
     * and whenever an index is re-initialized or re-created.
     */
    protected Directory createDirectory(boolean eraseContents) throws IOException {
        logger.debug("Initializing index directory at path '" + this.indexPath + "'");

        if (this.indexPath == null || "".equals(this.indexPath.trim())) {
            throw new IOException("Index path invalid: " + this.indexPath);
        }
        
        File path = new File(this.indexPath);
        if (!path.isDirectory()) {
            throw new IOException("Path '" + this.indexPath + "' is not a directory.");
        } else if (!path.canWrite()) {
            throw new IOException("Path '" + this.indexPath + "' is not writable.");
        }
        
        return FSDirectory.getDirectory(path, eraseContents);
    }
    
    public long getIndexByteSize() throws IOException {
        long length = 0;
        File indexDir = new File(this.indexPath);
        if (!indexDir.isDirectory()) 
            throw new IOException("Index path is not a directory: '" +
                                  this.indexPath + "'");

        File[] contents = indexDir.listFiles();
        for (int i=0; i<contents.length; i++) {
            if (contents[i].isFile()) length += contents[i].length();
        }
        return length;
    }

}
