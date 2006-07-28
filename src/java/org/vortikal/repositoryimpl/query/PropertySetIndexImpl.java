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
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.query.PropertySetIndex;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repositoryimpl.PropertyManager;
import org.vortikal.repositoryimpl.PropertySetImpl;

/**
 * 
 * @author oyviste
 */
public class PropertySetIndexImpl implements PropertySetIndex, InitializingBean, 
                                             BeanNameAware {

    Log logger = LogFactory.getLog(PropertySetIndexImpl.class);
    
    private LuceneIndex indexAccessor; // Underlying Lucene index accessor.
    private DocumentMapper documentMapper;
    private PropertyManager propertyManager;
    private Analyzer analyzer;
    private String id;

    public void afterPropertiesSet() throws BeanInitializationException {
        if (this.indexAccessor == null) {
            throw new BeanInitializationException("Property 'indexAccessor' not set.");
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
        // XXX: Ugly casting to obtain implementation specific details.
        
        // NOTE: Write-locking must be done above this level.
        //       This is needed to ensure the possibility of efficiently batching 
        //       together operations without interruption.
        try {
            doc = this.documentMapper.getDocument((PropertySetImpl)propertySet);
            
            if (logger.isDebugEnabled()) {
                logger.debug("Adding new property set at URI '" 
                                                + propertySet.getURI() + "'");
                logger.debug("Document mapper created the following document: ");
                
                Enumeration fieldEnum = doc.fields();
                while (fieldEnum.hasMoreElements()) {
                    Field field = (Field)fieldEnum.nextElement();
                    if (field.isBinary()) {
                        logger.debug("Field '" + field.name() + "', value: [BINARY]");
                    } else {
                        logger.debug("Field '" + field.name() + "', value: '" 
                                                    + field.stringValue() + "'");
                    }
                }
            }
            
            this.indexAccessor.getIndexWriter().addDocument(doc, this.analyzer);
        } catch (DocumentMappingException dme) {
            logger.warn("Could not map property set to index document", dme);
            throw new IndexException("Could not map property set to index document", dme);
        } catch (IOException io) {
            throw new IndexException(io);
        }
    }

    public int deletePropertySetTreeByUUID(String rootUuid) 
        throws IndexException {
        try {
            int n = 0;
            
            IndexReader reader = this.indexAccessor.getIndexReader();
    
            if (logger.isDebugEnabled()) {
                logger.debug("Deleting property set tree with root ID '" 
                                                            + rootUuid + "'");
            }
            
            n += reader.deleteDocuments(new Term(DocumentMapper.ID_FIELD_NAME,
                                                                      rootUuid));
            
            n += reader.deleteDocuments(new Term(DocumentMapper.ANCESTORIDS_FIELD_NAME,
                                                                      rootUuid));
            
            if (n == 0) {
                logger.warn("Consistency warning: zero index documents deleted"
                        + " for tree with root ID '" + rootUuid + "'");
            }
            
            if (logger.isDebugEnabled()) {
                logger.debug("Deleted " + n + " index documents.");
            }
            
            return n;
        } catch (IOException io) {
            throw new IndexException(io);
        } 
    }
    
    public int deletePropertySetTree(String rootUri) 
        throws IndexException {
        
        TermEnum tenum = null;
        TermDocs tdocs = null;
        try {

            IndexReader reader = this.indexAccessor.getIndexReader();
            Term rootUriTerm = new Term(DocumentMapper.URI_FIELD_NAME, rootUri);
            String fieldName = rootUriTerm.field();
            tenum = reader.terms(rootUriTerm);
            tdocs = reader.termDocs();

            int n = 0;

            if (logger.isDebugEnabled()) {
                logger.debug("Deleting property set tree with root URI '" 
                        + rootUri + "'");
            }
            
            do {
                Term term = tenum.term();
                if (term != null 
                    && term.field() == fieldName 
                    && term.text().startsWith(rootUri)) {
                    
                    tdocs.seek(tenum);
                    
                    while (tdocs.next()) {
                        reader.deleteDocument(tdocs.doc());
                        ++n;
                    }
                } else break;

            } while (tenum.next());
            
            if (n == 0) {
                logger.warn("Consistency warning: zero index documents deleted"
                        + " for tree with root URI '" + rootUri + "'");
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Deleted " + n + " index documents.");
            }

            return n;
        } catch (IOException io) {
            throw new IndexException(io);
        } finally {
            try {
                if (tenum != null) tenum.close();
                if (tdocs != null) tdocs.close();
            } catch (IOException io) {}
        }
    }
    
    public int deletePropertySet(String uri) throws IndexException {
        try {
            Term uriTerm = new Term(DocumentMapper.URI_FIELD_NAME, uri);
            IndexReader reader = this.indexAccessor.getIndexReader();
            
            if (logger.isDebugEnabled()) {
                logger.debug("Deleting property set at URI '" + uri + "' from index.");
            }
            
            int n = reader.deleteDocuments(uriTerm);
            
            if (n > 1) {
                logger.warn("Consitency warning: " + n 
                        + " index documents deleted for URI '" + uri + "'"); 
            }

            return n;
        } catch (IOException io) {
            throw new IndexException (io);
        }
    }
    
    public int deletePropertySetByUUID(String uuid) throws IndexException {
        try {
            Term uuidTerm = new Term(DocumentMapper.ID_FIELD_NAME, uuid);
            IndexReader reader = this.indexAccessor.getIndexReader();
            
            if (logger.isDebugEnabled()) {
                logger.debug("Deleting property set with ID '" + uuid + "'");
            }
            
            int n = reader.deleteDocuments(uuidTerm);
            
            if (n != 1) { 
                logger.warn("Consitency warning: " + n + " index documents deleted"
                        + " for ID '" + uuid + "'");
            }
            
            return n;
        } catch (IOException io) {
            throw new IndexException(io);
        }
    }

    public PropertySet getPropertySet(String uri) throws IndexException {
        TermDocs td = null;
        try {
            IndexReader reader = this.indexAccessor.getIndexReader();
            PropertySet propSet = null;
            td = reader.termDocs(new Term(DocumentMapper.URI_FIELD_NAME, uri));
            
            if (td.next()) {
                Document doc = reader.document(td.doc()); 
                propSet = this.documentMapper.getPropertySet(doc);
            } else {
                throw new IndexException("Could not find any property set at URI '" + uri + "'");
            }
          
            if (td.next()) {
                logger.warn("Consistency warning: Multiple property sets"
                        + " exist in index for URI '" + uri + "'");
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

    public void clearContents() throws IndexException {
        try {
            this.indexAccessor.clearContents();
        } catch (IOException io) {
            throw new IndexException(io);
        }
    }
    
    public Iterator orderedIterator() throws IndexException {
        try {
            return new PropertySetIndexIterator(this.indexAccessor, 
                                            this.documentMapper,
                                            DocumentMapper.URI_FIELD_NAME,
                                            "");
        } catch (IOException io) {
            throw new IndexException(io);
        }
    }
    
    public Iterator orderedSubtreeIterator(String rootUri) throws IndexException {
        try {
            return new PropertySetIndexSubtreeIterator(this.indexAccessor, 
                                            this.documentMapper,
                                            rootUri);
        } catch (IOException io) {
            throw new IndexException(io);
        }
    }
    
    public void close(Iterator iterator) throws IndexException {
        try {
            
            if ((iterator instanceof PropertySetIndexIterator)) {
                ((PropertySetIndexIterator)iterator).close();
            } else if (iterator instanceof PropertySetIndexSubtreeIterator) {
                ((PropertySetIndexSubtreeIterator)iterator).close();
            } else {
                throw new IllegalArgumentException("Illegal iterator type");
            }
        
            
        } catch (IOException io) {
            throw new IndexException(io);
        }
    }

    public void commit() throws IndexException {
        try {
            this.indexAccessor.commit();
        } catch (IOException io) {
            throw new IndexException(io);
        }
    }
    
    public void close() throws IndexException {
        try {
            this.indexAccessor.close();
        } catch (IOException io) {
            throw new IndexException(io);
        }
    }
    
    public void reinitialize() throws IndexException {
        try {
            this.indexAccessor.reinitialize();
        } catch (IOException io) {
            throw new IndexException(io);
        }
    }
    
    public void addIndexContents(PropertySetIndex index) throws IndexException {
        if (! (index instanceof PropertySetIndexImpl)) {
            throw new IllegalArgumentException(
                    "Only 'org.vortikal.repositoryimpl.query.PropertySetIndexImpl' instances are supported.");
        }
        
        try {
            PropertySetIndexImpl indexImpl = (PropertySetIndexImpl)index;
            
            LuceneIndex accessor = indexImpl.indexAccessor;
            
            IndexReader[] readers = new IndexReader[] { accessor.getIndexReader() };
            
            if (logger.isDebugEnabled()) {
                logger.debug("Adding all contents of index '" 
                                        + indexImpl.getId()
                                        + "' to '"
                                        + this.getId() + "' (this index)");
            }
            
            this.indexAccessor.getIndexWriter().addIndexes(readers);
        } catch (IOException io) {
            throw new IndexException(io);
        }
        
    }

    public boolean lock() {
        return this.indexAccessor.writeLockAcquire();
    }

    public void unlock() throws IndexException {
        this.indexAccessor.writeLockRelease();
    }

    public void setDocumentMapper(DocumentMapper documentMapper) {
        this.documentMapper = documentMapper;
    }

    public void setIndexAccessor(LuceneIndex indexAccessor) {
        this.indexAccessor = indexAccessor;
    }

    public void setPropertyManager(PropertyManager propertyManager) {
        this.propertyManager = propertyManager;
    }
    
    public void setBeanName(String beanName) {
        this.id = beanName;
    }
    
    public String getId() {
        return this.id;
    }

}
