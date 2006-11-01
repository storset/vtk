/* Copyright (c) 2006, University of Oslo, Norway
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

import java.io.IOException;
import java.util.NoSuchElementException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.vortikal.repository.IllegalOperationException;
import org.vortikal.repositoryimpl.CloseableIterator;

/**
 * Abstract iterator in document order (can be considered unordered).
 * This is the most efficient way of iterating through all index documents, 
 * however, the iteration is not sorted at all.
 * 
 * @author oyviste
 *
 */
abstract class AbstractDocumentIterator implements CloseableIterator {

    private IndexReader reader;
    private int next = 0;
    private Document currentDoc = null;
    
    public AbstractDocumentIterator(IndexReader reader) throws IOException {
        this.reader = reader;
        this.currentDoc = nextDoc();
    }
    
    private Document nextDoc() throws IOException {
        Document doc = null;
        
        while (next < reader.maxDoc()) {
            if (reader.isDeleted(next)) {
                ++next;
            } else break;
        }
        
        if (next < reader.maxDoc()) {
            doc = reader.document(next++);
        } 
        
        return doc;
    }
    
    public void close() {
        // Don't close the provided reader here, it is managed by the propety set index itself.
    }

    public boolean hasNext() {
        return currentDoc != null;
    }

    public Object next() {
        if (currentDoc == null) {
            throw new NoSuchElementException("No more elements");
        }
        
        Object retVal = null;
        try {
            retVal = getObjectFromDocument(currentDoc);
            currentDoc = nextDoc();
        } catch (Exception e) {
            currentDoc = null;
        }
        
        return retVal;
    }
    
    protected abstract Object getObjectFromDocument(Document document) throws Exception;
    
    public void remove() {
        throw new IllegalOperationException("Iterator does not support element removal");
    }

}
