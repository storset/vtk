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
 * Iterator over subtree in property set index.
 * 
 * @author oyviste
 *
 */
class PropertySetIndexSubtreeIterator implements Iterator {

    private Log logger = LogFactory.getLog(PropertySetIndexSubtreeIterator.class);
    
    private IndexReader reader;
    private LuceneIndex index;
    private DocumentMapper mapper;
    private String field;
    private String rootUri;
    private TermEnum tenum;
    private TermDocs tdocs;
    private int currentDoc;
    
    public PropertySetIndexSubtreeIterator(LuceneIndex index,
                                           DocumentMapper mapper,
                                           String rootUri) 
        throws IOException {
        
        this.reader = index.getReadOnlyIndexReader();
        this.index = index;
        this.field = DocumentMapper.URI_FIELD_NAME;
        this.rootUri = rootUri;
        this.mapper = mapper;
        init();
    }
    
    private void init() throws IOException {
        this.tenum = this.reader.terms(new Term(this.field, this.rootUri));
        this.tdocs = this.reader.termDocs();
        
        this.currentDoc = -1;
        if (this.tenum.term() != null 
             && this.tenum.term().field() == this.field
             && this.tenum.term().text().startsWith(this.rootUri)) {
            
            this.tdocs.seek(this.tenum);
            
            if (this.tdocs.next()) {
                this.currentDoc = this.tdocs.doc();
            } 
        }
    }
    
    /* (non-Javadoc)
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
        return (this.currentDoc != -1);
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
            Document doc = this.reader.document(this.currentDoc);
            propSet = this.mapper.getPropertySet(doc);
            
            this.currentDoc = -1;
            if (! this.tdocs.next()) {
                if (this.tenum.next()) {
                    if (this.tenum.term() != null
                          && this.tenum.term().field() == this.field
                          && this.tenum.term().text().startsWith(this.rootUri)) {
                        
                        this.tdocs.seek(this.tenum);
                        
                        if (this.tdocs.next()) {
                            this.currentDoc = this.tdocs.doc();
                        }
                    } 
                } 
            } else {
                this.currentDoc = this.tdocs.doc();
            }
        } catch (IOException io) {
            logger.warn("IOException during property set index iteration", io);
            this.currentDoc = -1;
        }
        
        return propSet;
    }
    
    public void close() throws IOException {
        this.tenum.close();
        this.tdocs.close();
        this.index.releaseReadOnlyIndexReader(this.reader);
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#remove()
     */
    public void remove() {
        throw new UnsupportedOperationException("This iterator does not supporting element removal");
    }
    
}
