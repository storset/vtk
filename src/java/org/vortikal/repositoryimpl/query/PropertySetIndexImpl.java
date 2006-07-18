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
import org.apache.lucene.index.TermDocs;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.query.PropertySetIndex;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repositoryimpl.PropertyManager;
import org.vortikal.repositoryimpl.PropertySetImpl;

/**
 * XXX: Not to be considered finished, but functional, at least.
 * May need to model things a bit differently to support indexing to alternate
 * index, hot-switching between indexes after re-indexing, locking etc.
 * 
 * XXX: handle DocumentMappingException's
 * 
 * @author oyviste
 *
 */
public class PropertySetIndexImpl implements PropertySetIndex, InitializingBean {

    Log logger = LogFactory.getLog(PropertySetIndexImpl.class);
    
    private LuceneIndex index; // Underlying Lucene index accessor.
    private DocumentMapper documentMapper;
    private PropertyManager propertyManager;
    private Analyzer analyzer;

    public void afterPropertiesSet() throws BeanInitializationException {
        if (this.index == null) {
            throw new BeanInitializationException("Property 'index' not set.");
        } else if (this.documentMapper == null) {
            throw new BeanInitializationException("Property 'documentMapper' not set.");
        }
        
        this.analyzer = initializePerFieldAnalyzer();
    }
    
    private PerFieldAnalyzerWrapper initializePerFieldAnalyzer() {
        
        List propDefs = this.propertyManager.getPropertyTypeDefinitions();
        
        KeywordAnalyzer keywordAnalyzer = new KeywordAnalyzer();
        EscapedMultiValueFieldAnalyzer multiValueAnalyzer = 
            new EscapedMultiValueFieldAnalyzer(FieldValueMapper.MULTI_VALUE_FIELD_SEPARATOR);
        
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
        
        // Special fields
        wrapper.addAnalyzer(DocumentMapper.ACL_INHERITED_FROM_FIELD_NAME, keywordAnalyzer);
        wrapper.addAnalyzer(DocumentMapper.ID_FIELD_NAME, keywordAnalyzer);
        wrapper.addAnalyzer(DocumentMapper.NAME_FIELD_NAME, keywordAnalyzer);
        wrapper.addAnalyzer(DocumentMapper.ANCESTORIDS_FIELD_NAME, multiValueAnalyzer);
        wrapper.addAnalyzer(DocumentMapper.RESOURCETYPE_FIELD_NAME, keywordAnalyzer);
        wrapper.addAnalyzer(DocumentMapper.URI_FIELD_NAME, keywordAnalyzer);
        wrapper.addAnalyzer(DocumentMapper.URI_DEPTH_FIELD_NAME, keywordAnalyzer);
        
        return wrapper;
    }
    
    public void addPropertySet(PropertySet propertySet) throws IndexException {

        Document doc = null;
        // XXX: FIXME: ugly casting
        
        // NOTE: "Transaction"-write-locking must be done above this level.
        //       This is needed to ensure the possibility of efficiently batching 
        //       together operations.
        try {
            doc = this.documentMapper.getDocument((PropertySetImpl)propertySet);
            
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Adding new property set at URI '" 
                                                + propertySet.getURI() + "'");
                this.logger.debug("Document mapper created the following document: ");
                
                Enumeration fieldEnum = doc.fields();
                while (fieldEnum.hasMoreElements()) {
                    Field field = (Field)fieldEnum.nextElement();
                    if (field.isBinary()) {
                        this.logger.debug("Field '" + field.name() + "', value: [BINARY]");
                    } else {
                        this.logger.debug("Field '" + field.name() + "', value: '" 
                                                    + field.stringValue() + "'");
                    }
                }
            }
            
            this.index.getIndexWriter().addDocument(doc, this.analyzer);
        } catch (DocumentMappingException dme) {
            this.logger.warn("Could not map property set to index document", dme);
            throw new IndexException("Could not map property set to index document", dme);
        } catch (IOException io) {
            throw new IndexException(io);
        }
    }

    public void clear() throws IndexException {
        try {
            this.index.clear();
        } catch (IOException io) {
            throw new IndexException(io);
        }
    }

    public int deletePropertySet(String uri, boolean deleteDescendants) throws IndexException {
        TermDocs td = null;
        try {
            Term uriTerm = new Term(DocumentMapper.URI_FIELD_NAME, uri);
            IndexReader reader = this.index.getIndexReader();
            
            td = reader.termDocs(uriTerm);
            
            if (! td.next()) {
                return 0; // Not found in index
            }
            
            int n = 0;
            if (deleteDescendants) {
                Field idField = reader.document(td.doc()).getField(DocumentMapper.ID_FIELD_NAME);
                String id = 
                    Integer.toString(BinaryFieldValueMapper.getIntegerFromStoredBinaryField(idField));
                
                int d = reader.deleteDocuments(
                            new Term(DocumentMapper.ANCESTORIDS_FIELD_NAME, id));
               
                if (logger.isDebugEnabled() && d > 0) {
                    logger.debug("Deleted " + d
                        + " descendant(s) of property set at URI '" + uri
                        + "' from index");
                }
                
                n += d;
            } 
            
            if (logger.isDebugEnabled()) {
                logger.debug("Deleting property set at URI '" + uri + "' from index.");
            }
            n += reader.deleteDocuments(uriTerm);
            
            if (logger.isDebugEnabled()) {
                logger.debug("Deleted " + n + " index documents.");
            }
            
            return n;
        } catch (IOException io) {
            throw new IndexException (io);
        } finally {
            if (td != null) {
                try {
                    td.close();
                } catch (IOException io) {}
            }
        }
    }

    public void updatePropertySet(PropertySet propertySet) throws IndexException {
        try {
            IndexReader reader = this.index.getIndexReader();
            reader.deleteDocuments(new Term(DocumentMapper.URI_FIELD_NAME, propertySet.getURI()));
            
            IndexWriter writer = this.index.getIndexWriter();
            writer.addDocument(this.documentMapper.getDocument((PropertySetImpl)propertySet), this.analyzer);
        } catch (IOException io) {
            throw new IndexException(io);
        }
    }

    public PropertySet getPropertySet(String uri) throws IndexException {
        TermDocs td = null;
        try {
            IndexReader reader = this.index.getIndexReader();
            PropertySet propSet = null;
            td = reader.termDocs(new Term(DocumentMapper.URI_FIELD_NAME, uri));
            
            if (td.next()) {
                Document doc = reader.document(td.doc()); 
                propSet = this.documentMapper.getPropertySet(doc);
            } else {
                throw new IndexException("Could not find any property set at URI '" + uri + "'");
            }
          
            if (td.next()) {
                this.logger.warn("Multiple property sets exist in index for a single URI");
            }
            
            return propSet;
        } catch (IOException io) {
            throw new IndexException(io);
        } finally {
            if (td != null) {
                try {
                    td.close();
                } catch (IOException io) {}
            }
        }
    }

    public void commit() throws IndexException {
        try {
            this.index.commit();
        } catch (IOException io) {
            throw new IndexException(io);
        }
    }

    public boolean lock() {
        return this.index.writeLockAcquire();
    }

    public void unlock() throws IndexException {
        this.index.writeLockRelease();
    }

    public void setDocumentMapper(DocumentMapper documentMapper) {
        this.documentMapper = documentMapper;
    }

    public void setIndex(LuceneIndex index) {
        this.index = index;
    }

    public void setPropertyManager(PropertyManager propertyManager) {
        this.propertyManager = propertyManager;
    }

}
