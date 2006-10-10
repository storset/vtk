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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.vortikal.repositoryimpl.index.management.IndexStatus;
import org.vortikal.repositoryimpl.index.management.IndexStatusException;

/**
 * 
 * @author oyviste
 */
public class LuceneIndexStatus implements IndexStatus {
    
    //private static Log logger = LogFactory.getLog(LuceneIndexStatus.class);
    
    private LuceneIndex index;
    
    public LuceneIndexStatus(LuceneIndex index) {
        this.index = index;
    }
    
    public boolean isLocked() throws IndexStatusException {
        try {
            return IndexReader.isLocked(this.index.getDirectory());
        } catch (IOException io) {
            throw new IndexStatusException("IOException while checking lock status: " +
                    io.getMessage());
        }
    }

    public boolean hasDeletions() throws IndexStatusException {
        IndexReader reader = null;
        try {
            reader = this.index.getReadOnlyIndexReader();
            return reader.hasDeletions(); 
        } catch (IOException io) {
            throw new IndexStatusException("IOException while checking deletions status: " +
                    io.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException io) {}
            }
        }
    }

    public int getNumberOfDocuments() throws IndexStatusException {
        IndexReader reader = null;
        try {
            reader = this.index.getReadOnlyIndexReader();
            return reader.numDocs();
        } catch (IOException io) {
            throw new IndexStatusException("IOException while getting number of docs: " 
                     + io.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException io) {}
            }
        }
    }

    public List getFieldNames() throws IndexStatusException {
        IndexReader reader = null;
        try {
            reader = this.index.getReadOnlyIndexReader();
            return new ArrayList(reader.getFieldNames(IndexReader.FieldOption.ALL));
        } catch (IOException io) {
            throw new IndexStatusException("IOException while getting field names: " 
                    + io.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException io) {}
            }
        }
    }
    
    public long getPhysicalSize() throws IndexStatusException {
        try {
            return this.index.getIndexByteSize();
        } catch (IOException io) {
            throw new IndexStatusException("Got IOException while getting size: " +
                    io.getMessage());
        }
    }
    
    public String getSystemPath() {
        return this.index.getStorageDirectory().getAbsolutePath();
    }
}
