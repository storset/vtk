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
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.StaleReaderException;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.index.TermPositions;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.vortikal.repository.IllegalOperationException;

/**
 * An <code>IndexReader</code> that delegates calls to a wrapped instance, but prevents
 * any writing operations to succeed. This is in fact a <em>real read-only</em> index reader.
 * 
 * It also includes simple reference-counting and close-on-last-reference-released semantics.
 * The reference-counting-related methods are not synchronized, and should only be called by
 * the code managing the read-only instances.
 * 
 * @author oyviste
 */
final class ReadOnlyIndexReader extends IndexReader {
    
    private final Log LOG = LogFactory.getLog(ReadOnlyIndexReader.class);
    
    private IndexReader wrappedReader;
    private int refCount = 0;
    private boolean closeOnZeroReferences = false;
    
    public ReadOnlyIndexReader(IndexReader reader) {
        super(reader.directory());
        this.wrappedReader = reader;
    }
    
    protected void increaseReferenceCount() {
        ++this.refCount;
    }
    
    protected void decreaseReferenceCount() throws IOException {
        if (--this.refCount <= 0 && this.closeOnZeroReferences) {
            this.wrappedReader.close();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Closed reader; reference count was decreased to zero.");
            }
        }
    }
    
    protected int getReferenceCount() {
        return this.refCount;
    }
    
    /**
     * Indicates that this read-only reader instance should be closed when reference count
     * reaches zero.
     * 
     * @return <code>true</code> if it was closed immediately, <code>false</code> if it was only
     *         marked for closing because of a reference count greater than zero.
     *         
     * @throws IOException
     */
    protected boolean closeOnZeroReferences() throws IOException {
        if (this.refCount == 0) {
            this.wrappedReader.close();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Closed reader; reference count was zero");
            }
            return true;
        } 
        
        this.closeOnZeroReferences = true;
        return false;
    }
    
    protected void doClose() throws IOException {
        this.closeOnZeroReferences = false;
        this.wrappedReader.close();
    }

    protected void doCommit() {
        // Does nothing, because this reader cannot physically modify an index.
    }

    protected void doDelete(int docNum) {
        throw new IllegalOperationException("Writing operations are not supported by this reader instance");
    }

    protected void doSetNorm(int doc, String field, byte value) {
        throw new IllegalOperationException("Writing operations are not supported by this reader instance");
    }

    protected void doUndeleteAll() {
        throw new IllegalOperationException("Writing operations are not supported by this reader instance");
    }

    public void setNorm(int doc, String field, float value)
    throws StaleReaderException, CorruptIndexException,
        LockObtainFailedException, IOException {
        throw new IllegalOperationException("Writing operations are not supported by this reader instance.");
    }

    public int docFreq(Term t) throws IOException {
        return this.wrappedReader.docFreq(t);
    }

    public Document document(int n) throws IOException {
        return this.wrappedReader.document(n);
    }

    public Document document(int n, FieldSelector fieldSelector) throws IOException {
        return this.wrappedReader.document(n, fieldSelector);
    }

    public Collection<?> getFieldNames(IndexReader.FieldOption fldOption) {
        return this.wrappedReader.getFieldNames(fldOption);
    }

    public TermFreqVector getTermFreqVector(int docNumber, String field)
            throws IOException {
        return this.wrappedReader.getTermFreqVector(docNumber, field);
    }

    public TermFreqVector[] getTermFreqVectors(int docNumber)
            throws IOException {
        return this.wrappedReader.getTermFreqVectors(docNumber);
    }

    public boolean hasDeletions() {
        return this.wrappedReader.hasDeletions();
    }

    public boolean isDeleted(int n) {
        return this.wrappedReader.isDeleted(n);
    }

    public int maxDoc() {
        return this.wrappedReader.maxDoc();
    }

    public byte[] norms(String field) throws IOException {
        return this.wrappedReader.norms(field);
    }

    public void norms(String field, byte[] bytes, int offset)
            throws IOException {
        this.wrappedReader.norms(field, bytes, offset);
    }

    public int numDocs() {
        return this.wrappedReader.numDocs();
    }

    public TermDocs termDocs() throws IOException {
        return this.wrappedReader.termDocs();
    }

    public TermPositions termPositions() throws IOException {
        return this.wrappedReader.termPositions();
    }

    public TermEnum terms() throws IOException {
        return this.wrappedReader.terms();
    }

    public TermEnum terms(Term t) throws IOException {
        return this.wrappedReader.terms(t);
    }

    public long getVersion() { 
        return wrappedReader.getVersion(); 
    }
    
    public boolean isCurrent() throws IOException { 
        return wrappedReader.isCurrent(); 
    }
    
    public boolean hasNorms(String field) throws IOException {
        return wrappedReader.hasNorms(field);
    }

    public Directory directory() {
        throw new IllegalOperationException("This is a read-only IndexReader instance, "
                + "Directory-access will not be provided.");
    }

    public boolean isOptimized() {
        return wrappedReader.isOptimized();
    }

    public TermDocs termDocs(Term term) throws IOException {
        return wrappedReader.termDocs(term);
    }

    public TermPositions termPositions(Term term) throws IOException {
        return wrappedReader.termPositions(term); 
    }
    
}
