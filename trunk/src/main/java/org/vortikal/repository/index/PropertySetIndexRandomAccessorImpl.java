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
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.FieldSelectorResult;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.vortikal.repository.Path;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.index.mapping.DocumentMapper;
import org.vortikal.repository.index.mapping.FieldNames;

/**
 * Random accessor for property set index.
 * Caches internal <code>TermDocs</code> instances.
 * 
 * @author oyviste
 *
 */
class PropertySetIndexRandomAccessorImpl implements PropertySetIndexRandomAccessor {

    private TermDocs uriTermDocs;
    private TermDocs uuidTermDocs;
    private DocumentMapper mapper;
    private IndexReader reader;
    
    public PropertySetIndexRandomAccessorImpl(IndexReader reader, DocumentMapper mapper) 
        throws IOException {
        this.mapper = mapper;
        this.reader = reader;
        this.uriTermDocs = reader.termDocs(new Term(FieldNames.URI_FIELD_NAME, ""));
        this.uuidTermDocs = reader.termDocs(new Term(FieldNames.ID_FIELD_NAME, ""));
  
    }
    
    @Override
    public boolean exists(Path uri) throws IndexException {
        return (countInstances(uri) > 0);
    }
    
    @Override
    public int countInstances(Path uri) throws IndexException {
        int count = 0;
        try {
            this.uriTermDocs.seek(new Term(FieldNames.URI_FIELD_NAME, uri.toString()));
            while (this.uriTermDocs.next()) {
                ++count;
            }
        } catch (IOException io) {
            throw new IndexException(io);
        }
        
        return count;
    }
    
    @Override
    public PropertySet getPropertySetByURI(Path uri) throws IndexException {
        PropertySet propSet = null;

        try {
            this.uriTermDocs.seek(new Term(FieldNames.URI_FIELD_NAME, uri.toString()));
            if (this.uriTermDocs.next()) {
                propSet = this.mapper.getPropertySet(
                        this.reader.document(this.uriTermDocs.doc()));
            }
        } catch (IOException io) {
            throw new IndexException(io);
        }
        
        return propSet;
    }

    @Override
    public PropertySet getPropertySetByUUID(String uuid) throws IndexException {
        PropertySet propSet = null;
        try {
            this.uuidTermDocs.seek(new Term(FieldNames.ID_FIELD_NAME, uuid));
            if (this.uuidTermDocs.next()) {
                propSet = this.mapper.getPropertySet(
                        this.reader.document(this.uuidTermDocs.doc()));
            }
        } catch (IOException io) {
            throw new IndexException(io);
        }
        
        return propSet;
    }
    
    @Override
    public PropertySetInternalData getPropertySetInternalData(final Path uri) throws IndexException {
        try {
            this.uriTermDocs.seek(new Term(FieldNames.URI_FIELD_NAME, uri.toString()));

            if (this.uriTermDocs.next()) {
                Document doc = this.reader.document(this.uriTermDocs.doc(), new FieldSelector() {
                    private static final long serialVersionUID = 1L;
                    @Override
                    public FieldSelectorResult accept(String fieldName) {
                        if (FieldNames.URI_FIELD_NAME == fieldName
                                || FieldNames.RESOURCETYPE_FIELD_NAME == fieldName
                                || FieldNames.STORED_ACL_READ_PRINCIPALS_FIELD_NAME == fieldName
                                || FieldNames.STORED_ACL_INHERITED_FROM_FIELD_NAME == fieldName
                                || FieldNames.STORED_ID_FIELD_NAME == fieldName) {
                            return FieldSelectorResult.LOAD;
                        }

                        return FieldSelectorResult.NO_LOAD;
                    }
                });
                
                final String rt = doc.get(FieldNames.RESOURCETYPE_FIELD_NAME);
                final int id = this.mapper.getResourceId(doc);
                final int aclInheritedFrom = this.mapper.getAclInheritedFrom(doc);
                final Set<String> aclReadPrincipalNames = this.mapper.getACLReadPrincipalNames(doc);
                
                return new PropertySetInternalData() {
                    @Override
                    public Path getURI() {
                        return uri;
                    }

                    @Override
                    public String getResourceType() {
                        return rt;
                    }

                    @Override
                    public int getResourceId() {
                        return id;
                    }

                    @Override
                    public int getAclInheritedFromId() {
                        return aclInheritedFrom;
                    }
                    
                    @Override
                    public Set<String> getAclReadPrincipalNames() {
                        return aclReadPrincipalNames;
                    }
                };
            }
            
            return null;
        } catch (IOException io) {
            throw new IndexException(io);
        }
    }
    
    @Override
    public void close() throws IndexException {
        try {
            this.uriTermDocs.close();
            this.uuidTermDocs.close();
        } catch (IOException io) {
            throw new IndexException(io);
        }
    }

}
