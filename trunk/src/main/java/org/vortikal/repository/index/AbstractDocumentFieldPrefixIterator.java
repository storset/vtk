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
package org.vortikal.repository.index;

import java.io.IOException;
import java.util.NoSuchElementException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;

/**
 * Abstract iterator over documents for a given field and a field value prefix.
 * 
 * @author oyviste
 *
 */
abstract class AbstractDocumentFieldPrefixIterator implements CloseableIterator<Object> {

    private Log logger = LogFactory.getLog(AbstractDocumentFieldPrefixIterator.class);
    
    private IndexReader reader;
    private String iterationFieldName;
    private String prefix;
    private TermEnum tenum;
    private TermDocs tdocs;
    private int next = -1;
    
    public AbstractDocumentFieldPrefixIterator(IndexReader reader,
                                               String iterationFieldName,
                                               String prefix) 
        throws IOException {
        
        this.reader = reader;
        this.iterationFieldName = iterationFieldName.intern();
        this.prefix = prefix != null ? prefix : "";

        this.tenum = this.reader.terms(new Term(this.iterationFieldName, this.prefix));
        this.tdocs = this.reader.termDocs();

        while (tenum.term() != null && tenum.term().field() == iterationFieldName) {
            if (tenum.term().text().startsWith(prefix)) {
                tdocs.seek(tenum);
                next = nextDoc();
                break;
            }
            tenum.next();
        }
    }
    
    
    // Next non-deleted URI _including_ any multiples
    private int nextDoc() throws IOException {
        if (tdocs.next()) {
            return tdocs.doc();
        }
        
        // No more docs for current term, seek to next
        while (tenum.next() && tenum.term().field() == iterationFieldName) {
            if (tenum.term().text().startsWith(prefix)) {
                tdocs.seek(tenum);
                if (tdocs.next()) {
                    return tdocs.doc();
                }
            }
        }
        
        return -1;
    }
    
    /* (non-Javadoc)
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
        return (next != -1);
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#next()
     */
    public Object next() {
        if (next == -1) {
            throw new NoSuchElementException("No more elements");
        }
        
        Object retVal = null;
        
        try {
            Document doc = reader.document(next);
            retVal = getObjectFromDocument(doc);
            next = nextDoc();
        } catch (Exception e) {
            logger.warn("Exception while getting next element instance from document number " 
                                                                           +  next, e);
            next = -1;
        }
        
        return retVal;
    }
    
    protected abstract Object getObjectFromDocument(Document document) throws Exception;
    
    public void close() throws IOException {
        this.tenum.close();
        this.tdocs.close();
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#remove()
     */
    public void remove() {
        throw new UnsupportedOperationException("This iterator does not supporting element removal");
    }
    
}
