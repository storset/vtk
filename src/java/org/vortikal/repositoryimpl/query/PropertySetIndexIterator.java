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
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.vortikal.repository.PropertySet;

/**
 * Iterator implementation for property set index.
 * 
 * @author oyviste
 *
 */
class PropertySetIndexIterator implements Iterator {

    private Log logger = LogFactory.getLog(PropertySetIndexIterator.class);
    
    private IndexReader reader;
    private LuceneIndex index;
    private DocumentMapper mapper;
    private String iterationFieldName;
    private String iterationFieldStartValue;
    private TermEnum tenum;
    private TermDocs tdocs;
    private int currentDoc;
    
    public PropertySetIndexIterator(LuceneIndex index,
                                    DocumentMapper mapper,
                                    String iterationFieldName,
                                    String iterationFieldStartValue) 
        throws IOException {
        this.reader = index.getReadOnlyIndexReader();
        this.index = index;
        this.iterationFieldName = iterationFieldName.intern();
        this.iterationFieldStartValue = iterationFieldStartValue != null ?
                                        iterationFieldStartValue : "";
        this.mapper = mapper;
        init();
    }
    
    private void init() throws IOException {
        tenum = reader.terms(new Term(iterationFieldName, iterationFieldStartValue));
        tdocs = reader.termDocs();
        
        currentDoc = -1;
        if (tenum.term() != null 
             && tenum.term().field() == iterationFieldName) {
            
            tdocs.seek(tenum);
            
            if (tdocs.next()) {
                currentDoc = tdocs.doc();
            } 
        }
    }
    
    /* (non-Javadoc)
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
        return (currentDoc != -1);
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#next()
     */
    public Object next() {
        if (!hasNext()) {
            throw new IllegalStateException("No more elements");
        }
     
        PropertySet propSet = null;
        try {
            Document doc = reader.document(currentDoc);
            propSet = mapper.getPropertySet(doc);
            
            currentDoc = -1;
            if (! tdocs.next()) {
                if (tenum.next()) {
                    if (tenum.term() != null
                          && tenum.term().field() == iterationFieldName) {
                        tdocs.seek(tenum);
                        
                        if (tdocs.next()) {
                            currentDoc = tdocs.doc();
                        }
                    } 
                } 
            } else {
                currentDoc = tdocs.doc();
            }
        } catch (IOException io) {
            logger.warn("IOException during property set index iteration", io);
            currentDoc = -1;
        }
        
        return propSet;
    }
    
    public void close() throws IOException {
        tenum.close();
        tdocs.close();
        index.releaseReadOnlyIndexReader(reader);
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#remove()
     */
    public void remove() {
        throw new UnsupportedOperationException("This iterator does not supporting element removal");
    }
    
}
