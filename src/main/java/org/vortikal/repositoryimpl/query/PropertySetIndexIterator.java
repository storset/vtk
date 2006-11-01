package org.vortikal.repositoryimpl.query;

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;

/**
 * Iterator over <code>PropertySet</code> instances by URI.
 *
 * @author oyviste
 */
class PropertySetIndexIterator extends AbstractDocumentFieldIterator {

    private DocumentMapper mapper;
    
    public PropertySetIndexIterator(IndexReader reader, DocumentMapper mapper)
            throws IOException {
        
        super(reader, DocumentMapper.URI_FIELD_NAME, null);
        this.mapper = mapper;
    }

    protected Object getObjectFromDocument(Document document) throws Exception {
        return this.mapper.getPropertySet(document);
    }

}
