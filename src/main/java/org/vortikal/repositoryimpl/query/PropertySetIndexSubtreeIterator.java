package org.vortikal.repositoryimpl.query;

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;

public class PropertySetIndexSubtreeIterator extends  AbstractDocumentFieldPrefixIterator {

    private DocumentMapper mapper;
    
    public PropertySetIndexSubtreeIterator(IndexReader reader, DocumentMapper mapper, String rootUri)
            throws IOException {
        super(reader, DocumentMapper.URI_FIELD_NAME, rootUri);
        this.mapper = mapper;
    }

    protected Object getObjectFromDocument(Document doc) throws Exception {
        return mapper.getPropertySet(doc);
    }

}
