package org.vortikal.repositoryimpl.query;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;

/**
 * Simple URI-only iterator, lexicographic order.
 * 
 * @author oyviste
 *
 */
public class PropertySetIndexUriIterator implements CloseableIterator {

    private String next = null;
    private TermEnum te;
    private TermDocs td;
    private IndexReader reader;
    private String iteratorField = DocumentMapper.URI_FIELD_NAME.intern();
        
    public PropertySetIndexUriIterator(IndexReader reader) throws IOException {
        this.te = reader.terms(new Term(this.iteratorField, ""));
        this.td = reader.termDocs(new Term(this.iteratorField, ""));
        this.reader = reader;
        
        if (te.term() != null && te.term().field() == iteratorField) {
            td.seek(te);
            next = nextUri();
        }
    }
    
    // Next non-deleted URI _including_ any multiples
    private String nextUri() throws IOException {
        while (td.next()) {
            if (! reader.isDeleted(td.doc())) {
                return te.term().text();
            }
        }
        
        // No more docs for current term, seek to next
        while (te.next() && te.term().field() == iteratorField) {
            td.seek(te);
            while (td.next()) {
                if (! reader.isDeleted(td.doc())) {
                    return te.term().text();
                }
            }
        }
        
        return null;
    }
    
    public boolean hasNext() {
        return (next != null);
    }

    public Object next() {
        if (next == null) {
            throw new IllegalStateException("No more elements");
        }
        
        String retVal = next;
        
        try {
            next = nextUri();
        } catch (IOException e) {
            next = null;
        }
        
        return retVal;
    }

    public void remove() {
        throw new UnsupportedOperationException("Iterator does not support elment removal");
    }
    
    public void close() throws Exception {
        te.close();
        td.close();
    }

}
