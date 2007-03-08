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
package org.vortikal.repositoryimpl.index;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.vortikal.repository.PropertySet;
import org.vortikal.repositoryimpl.query.WildcardPropertySelect;

/**
 * Random accessor for property set index.
 * Caches internal <code>TermDocs</code> instances.
 * 
 * @author oyviste
 *
 */
public class PropertySetIndexRandomAccessorImpl implements PropertySetIndexRandomAccessor {

    private TermDocs uriTermDocs;
    private TermDocs uuidTermDocs;
    private DocumentMapper mapper;
    private IndexReader reader;
    
    public PropertySetIndexRandomAccessorImpl(IndexReader reader, DocumentMapper mapper) 
        throws IOException {
        this.mapper = mapper;
        this.reader = reader;
        this.uriTermDocs = reader.termDocs(new Term(DocumentMapper.URI_FIELD_NAME, ""));
        this.uuidTermDocs = reader.termDocs(new Term(DocumentMapper.ID_FIELD_NAME, ""));
  
    }
    
    public boolean exists(String uri) throws IndexException {
        return (countInstances(uri) > 0);
    }
    
    public int countInstances(String uri) throws IndexException {
        int count = 0;
        try {
            this.uriTermDocs.seek(new Term(DocumentMapper.URI_FIELD_NAME, uri));
            while (this.uriTermDocs.next()) {
                if (! reader.isDeleted(this.uriTermDocs.doc())) ++count;
            }
        } catch (IOException io) {
            throw new IndexException(io);
        }
        
        return count;
    }
    
    public PropertySet getPropertySetByURI(String uri) throws IndexException {
        PropertySet propSet = null;

        try {
            this.uriTermDocs.seek(new Term(DocumentMapper.URI_FIELD_NAME, uri));
            while (this.uriTermDocs.next()) {
                if (! reader.isDeleted(this.uriTermDocs.doc())) {
                    propSet = this.mapper.getPropertySet(
                        this.reader.document(this.uriTermDocs.doc()),
                        WildcardPropertySelect.WILDCARD_PROPERTY_SELECT);
                }
            }
        } catch (IOException io) {
            throw new IndexException(io);
        }
        
        return propSet;
    }

    public PropertySet getPropertySetByUUID(String uuid) throws IndexException {
        PropertySet propSet = null;
        try {
            this.uuidTermDocs.seek(new Term(DocumentMapper.ID_FIELD_NAME, uuid));
            while (this.uuidTermDocs.next()) {
                if (! reader.isDeleted(this.uuidTermDocs.doc())) {
                    propSet = mapper.getPropertySet(
                        reader.document(this.uuidTermDocs.doc()),
                        WildcardPropertySelect.WILDCARD_PROPERTY_SELECT);
                }
            }
        } catch (IOException io) {
            throw new IndexException(io);
        }
        
        return propSet;
    }

    public void close() throws IndexException {
        try {
            this.uriTermDocs.close();
            this.uuidTermDocs.close();
        } catch (IOException io) {
            throw new IndexException(io);
        }

    }

}
