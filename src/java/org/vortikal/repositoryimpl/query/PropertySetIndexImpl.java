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
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.query.PropertySetIndex;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repositoryimpl.PropertyManagerImpl;
import org.vortikal.repositoryimpl.PropertySetImpl;
import org.vortikal.repositoryimpl.index.IndexException;

/**
 * XXX: Not to be considered finished, but functional, at least.
 * May need to model things a bit differently to support indexing to alternate
 * index, hot-switching between indexes after re-indexing, locking etc.
 * XXX: handle DocumentMappingException's
 * 
 * @author oyviste
 *
 */
public class PropertySetIndexImpl implements PropertySetIndex, InitializingBean {

    Log logger = LogFactory.getLog(PropertySetIndexImpl.class);
    
    private LuceneIndex index; // Underlying Lucene index.
    private DocumentMapper documentMapper;
    private PropertyManagerImpl propertyManager;
    private Analyzer analyzer;

    public void afterPropertiesSet() throws BeanInitializationException {
        if (index == null) {
            throw new BeanInitializationException("Property 'index' not set.");
        } else if (documentMapper == null) {
            throw new BeanInitializationException("Property 'documentMapper' not set.");
        }
        
        this.analyzer = initializePerFieldAnalyzer();
    }
    
    private PerFieldAnalyzerWrapper initializePerFieldAnalyzer() {
        
        List propDefs = propertyManager.getPropertyTypeDefinitions();
        
        KeywordAnalyzer keywordAnalyzer = new KeywordAnalyzer();
        EscapedMultiValueFieldAnalyzer multiValueAnalyzer = 
            new EscapedMultiValueFieldAnalyzer(FieldMapper.MULTI_VALUE_FIELD_SEPARATOR);
        
        PerFieldAnalyzerWrapper wrapper = new PerFieldAnalyzerWrapper(keywordAnalyzer);
        
        for (Iterator i = propDefs.iterator(); i.hasNext(); ) {
            PropertyTypeDefinition def = (PropertyTypeDefinition)i.next();
            String fieldName;
            Namespace ns = def.getNamespace();
            if (ns != null && ns.getPrefix() != null) {
                fieldName = ns.getPrefix() 
                          + DocumentMapper.FIELD_NAMESPACEPREFIX_NAME_SEPARATOR
                          + def.getName();
            } else {
                fieldName = def.getName();
            }
            
            if (def.isMultiple()) {
                wrapper.addAnalyzer(fieldName, multiValueAnalyzer);
            } else {
                wrapper.addAnalyzer(fieldName, keywordAnalyzer);
            }
        }
        
        // Also need to consider special fields
        wrapper.addAnalyzer(DocumentMapper.ACL_INHERITED_FROM_FIELD_NAME, keywordAnalyzer);
        wrapper.addAnalyzer(DocumentMapper.ID_FIELD_NAME, keywordAnalyzer);
        wrapper.addAnalyzer(DocumentMapper.NAME_FIELD_NAME, keywordAnalyzer);
        wrapper.addAnalyzer(DocumentMapper.ANCESTORIDS_FIELD_NAME, multiValueAnalyzer);
        wrapper.addAnalyzer(DocumentMapper.RESOURCETYPE_FIELD_NAME, keywordAnalyzer);
        wrapper.addAnalyzer(DocumentMapper.URI_FIELD_NAME, keywordAnalyzer);
        
        return wrapper;
    }
    
    public void addPropertySet(PropertySet propertySet) throws IndexException {

        Document doc = null;
        // XXX: FIXME: ugly casting 
        // XXX: FIXME: locking must be done at some point, need to think more about how to best model this.
        try {
            doc = documentMapper.getDocument((PropertySetImpl)propertySet);
            
            if (logger.isDebugEnabled()) {
                logger.debug("Adding new property set at URI '" + propertySet.getURI() + "'");
                logger.debug("Document mapper created the following document: ");
                
                Enumeration fieldEnum = doc.fields();
                while (fieldEnum.hasMoreElements()) {
                    Field field = (Field)fieldEnum.nextElement();
                    logger.debug("Field '" + field.name() + "', value: '" + field.stringValue() + "'");
                }
            }
            
            index.getIndexWriter().addDocument(doc, this.analyzer);
        } catch (DocumentMappingException dme) {
            logger.warn("Could not map property set to index document", dme);
            throw new IndexException("Could not map property set to index document", dme);
        } catch (IOException io) {
            throw new IndexException(io);
        }
        
    }

    public void clearIndex() throws IndexException {
        try {
            index.createNewIndex();
        } catch (IOException io) {
            throw new IndexException(io);
        }
    }

    public int deletePropertySet(String uri) throws IndexException {
        IndexReader reader = null;
        IndexSearcher searcher = null;
        try {
            Term uriTerm = new Term(DocumentMapper.URI_FIELD_NAME, uri);
            TermQuery q = new TermQuery(uriTerm);
            
            reader = index.getIndexReader();
            searcher = new IndexSearcher(reader);
            
            Hits hits = searcher.search(q);
            
            if (logger.isDebugEnabled()) {
                logger.debug("Deleting property set at URI '" + uri + "' from index.");
            }
            
            if (hits.length() == 0) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Property set at URI '" + uri + "' not found in index.");
                }
                return 0;
            } else {
                String id = hits.doc(0).get(DocumentMapper.ID_FIELD_NAME);
                int n = reader.deleteDocuments(new Term(DocumentMapper.URI_FIELD_NAME, uri));
                n += reader.deleteDocuments(new Term(DocumentMapper.ANCESTORIDS_FIELD_NAME, id));

                if (logger.isDebugEnabled()) {
                    logger.debug("Deleted " + n + " documents from index.");
                }
                
                return n;
            }
        } catch (IOException io) {
            throw new IndexException (io);
        } finally {
            if (searcher != null) {
                try {
                    searcher.close();
                } catch (IOException io) {}
            }
        }
    }

    public void updatePropertySet(PropertySet propertySet) throws IndexException {
        try {
            IndexReader reader = index.getIndexReader();
            reader.deleteDocuments(new Term(DocumentMapper.URI_FIELD_NAME, propertySet.getURI()));
            
            IndexWriter writer = index.getIndexWriter();
            writer.addDocument(documentMapper.getDocument((PropertySetImpl)propertySet), this.analyzer);
        } catch (IOException io) {
            throw new IndexException(io);
        }
    }

    public PropertySet getPropertySet(String uri) throws IndexException {
        IndexSearcher searcher = null;
        try {
            PropertySet propSet = null;
            searcher = index.getIndexSearcher();
            TermQuery tq = new TermQuery(new Term(DocumentMapper.URI_FIELD_NAME, uri));
            Hits hits = searcher.search(tq);
            if (hits.length() > 1) {
                throw new IndexException("Multiple property sets exist in index for a single URI.");
            }
            
            if (hits.length() == 1) {
                Document doc = hits.doc(0);
                propSet = documentMapper.getPropertySet(doc);
            } else {
                throw new IndexException("Could not find any property set with URI '" + uri + "'");
            }
          
            return propSet;
        } catch (IOException io) {
            throw new IndexException(io);
        } finally {
            if (searcher != null) {
                try {
                    searcher.close();
                } catch (IOException io) {}
            }
        }
    }

    public void commit() throws IndexException {
        try {
            index.commit();
        } catch (IOException io) {
            throw new IndexException(io);
        }
    }

    public boolean lock() {
        return index.lockAcquire();
    }

    public void unlock() throws IndexException {
        index.lockRelease();
    }

    public void setDocumentMapper(DocumentMapper documentMapper) {
        this.documentMapper = documentMapper;
    }

    public void setIndex(LuceneIndex index) {
        this.index = index;
    }

    public void setPropertyManager(PropertyManagerImpl propertyManager) {
        this.propertyManager = propertyManager;
    }

}
