package org.vortikal.repositoryimpl.query;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.vortikal.repository.PropertySet;

/**
 * Random accessor for property set index.
 * Caches <code>TermDocs</code> instances.
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
                    propSet = this.mapper.getPropertySet(this.reader.document(this.uriTermDocs.doc()));
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
                    propSet = mapper.getPropertySet(reader.document(this.uuidTermDocs.doc()));
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
