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
package org.vortikal.repositoryimpl.query.parser;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.query.QueryException;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repositoryimpl.PropertyManager;
import org.vortikal.repositoryimpl.query.query.PropertySortField;
import org.vortikal.repositoryimpl.query.query.SimpleSortField;
import org.vortikal.repositoryimpl.query.query.SortField;
import org.vortikal.repositoryimpl.query.query.SortFieldDirection;
import org.vortikal.repositoryimpl.query.query.Sorting;
import org.vortikal.repositoryimpl.query.query.SortingImpl;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.cache.ReusableObjectCache;
import org.vortikal.util.text.SimpleDateFormatCache;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * Utility class for performing searches returning result sets wrapped
 * in an XML structure.
 */
public class XmlSearcher implements InitializingBean {

    private Log logger = LogFactory.getLog(this.getClass());

    private Searcher searcher;
    private QueryManager queryManager;
    private PropertyManager propertyManager;
    private SortParser sortParser = new SortParser();
    private int maxResults = 1000;
    private ReusableObjectCache dateFormatCache = 
                            new SimpleDateFormatCache("yyyy-MM-dd HH:mm:ss z");

    public void setSearcher(Searcher searcher) {
        this.searcher = searcher;
    }

    public void setQueryManager(QueryManager queryManager) {
        this.queryManager = queryManager;
    }
    
    public void setPropertyManager(PropertyManager propertyManager) {
        this.propertyManager = propertyManager;
    }
    
    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public void afterPropertiesSet() {
        if (this.searcher == null) {
            throw new BeanInitializationException(
                "JavaBean property 'searcher' not set");
        }
        if (this.queryManager == null) {
            throw new BeanInitializationException(
                "JavaBean property 'queryManager' not set");
        }
        if (this.propertyManager == null) {
            throw new BeanInitializationException(
                "JavaBean property 'propertyManager' not set");
        }
    }
    

    public org.w3c.dom.NodeList executeQuery(String query, String sort,
                                             String maxResults) throws QueryException {

        String token = null;
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        if (securityContext != null) {
            token = securityContext.getToken();
        }

        return executeQuery(token, query, sort, maxResults);
    }

    public org.w3c.dom.NodeList executeQuery(String token, String query, 
                String sort, String maxResults) throws QueryException {
        
        Document doc = null;
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            doc = builder.newDocument();
        } catch (ParserConfigurationException e) {
            throw new QueryException(e.getMessage());
        }
        
        int limit = this.maxResults;
        try {
            limit = Integer.parseInt(maxResults);
        } catch (NumberFormatException e) {}
        if (limit > this.maxResults) {
            limit = this.maxResults;
        }
        
        try {
            Sorting sorting = this.sortParser.parseSortString(sort);
            ResultSet rs = this.queryManager.execute(token, query, sorting, limit);
            
            addResultSetToDocument(rs, doc);
        } catch (Exception e) {
            logger.warn("Error occurred while performing query: '" + query + "'", e);
            
            Element errorElement = doc.createElement("error");
            doc.appendChild(errorElement);
            errorElement.setAttribute("exception", e.getClass().getName());
            errorElement.setAttribute("query", query);
            errorElement.setAttribute("sort", sort);
            String msg = e.getMessage() != null ? e.getMessage() : "No message";
            errorElement.setTextContent(msg);
        }
        
        return doc.getDocumentElement().getChildNodes();
    }
  
    private void addResultSetToDocument(ResultSet rs, Document doc) {
        long start = System.currentTimeMillis();
        
        Element resultElement = doc.createElement("results");
        doc.appendChild(resultElement);
        resultElement.setAttribute("size", String.valueOf(rs.getSize()));
        for (Iterator i = rs.iterator(); i.hasNext();) {
            PropertySet propSet = (PropertySet)i.next();
            addPropertySetToResults(doc, resultElement, propSet);
        }
        
        if (logger.isDebugEnabled()) {
            long now = System.currentTimeMillis();
            logger.debug("Building XML result set took " + (now - start) + " ms");
        }
    }
    
    private void addPropertySetToResults(Document doc, Element resultsElement, 
                            PropertySet propSet) {
        
        Element propertySetElement = doc.createElement("resource");
        resultsElement.appendChild(propertySetElement);
        
        Element uri = doc.createElement("property");
        uri.setAttribute("name", "uri");
        Element uriValue = doc.createElement("value");
        uriValue.setTextContent(propSet.getURI());
        uri.appendChild(uriValue);
        
        Element name = doc.createElement("property");
        name.setAttribute("name", "name");
        Element nameValue = doc.createElement("value");
        nameValue.setTextContent(propSet.getName());
        name.appendChild(nameValue);
        
        Element type = doc.createElement("property");
        type.setAttribute("name", "type");
        Element typeValue = doc.createElement("value");
        typeValue.setTextContent(propSet.getResourceType());
        type.appendChild(typeValue);
        
        propertySetElement.appendChild(uri);
        propertySetElement.appendChild(name);
        propertySetElement.appendChild(type);
        
        for (Iterator i = propSet.getProperties().iterator(); i.hasNext();) {
            Property prop = (Property) i.next();
            addPropertyToPropertySetElement(doc, propertySetElement, prop);
        }
        
    }
    
    private void addPropertyToPropertySetElement(Document doc, Element propSetElement,
            Property prop) {
        
        if (prop.getDefinition() == null) {
            return;
        }
        
        Element propertyElement = doc.createElement("property");
        
        String namespaceUri = prop.getNamespace().getUri();
        if (namespaceUri != null) {
            propertyElement.setAttribute("namespace", namespaceUri);
        }
        
        String prefix = prop.getNamespace().getPrefix();
        if (prefix != null) {
            propertyElement.setAttribute("name", prefix + ":" + prop.getName());
        } else {
            propertyElement.setAttribute("name", prop.getName());
        }
        
        if (prop.getDefinition().isMultiple()) {
            Element valuesElement = doc.createElement("values");
            Value[] values = prop.getValues();
            for (int i=0; i<values.length; i++) {
                Element valueElement = doc.createElement("value");
                valueElement.setTextContent(valueToString(values[i]));
                valuesElement.appendChild(valueElement);
            }
            propertyElement.appendChild(valuesElement);
        } else {
            Element valueElement = doc.createElement("value");
            Value value = prop.getValue();
            valueElement.setTextContent(valueToString(value));
            propertyElement.appendChild(valueElement);
        }
        
        propSetElement.appendChild(propertyElement);        
    }
    
    private String valueToString(Value value) {
        switch (value.getType()) {
            case PropertyType.TYPE_DATE:
                DateFormat f = (DateFormat)this.dateFormatCache.getInstance();
                String formattedDate = f.format(value.getDateValue());
                this.dateFormatCache.putInstance(f);

                return formattedDate;
            case PropertyType.TYPE_PRINCIPAL:
                return value.getPrincipalValue().getName();
            default:
                return value.toString();
        }
    }

    private class SortParser {

        /**
         * Parses a sort specification of the syntax
         * <code>field(:asc|:desc)?(,field(:asc|:desc)?)*</code> and
         * produces a {@link Sorting} object.
         *
         * @param sortString the sort specification
         * @return a sort object, or <code>null</code> if the string does
         * not contain any valid sort fields.
         */
        public Sorting parseSortString(String sortString) {
            if (sortString == null || "".equals(sortString.trim())) {
                return null;
            }
        
            String[] fields = sortString.split(",");
            List result = new ArrayList();
            Set referencedFields = new HashSet();
        
            for (int i = 0; i < fields.length; i++) {
                String specifier = fields[i].trim();
                String field = null;
                SortFieldDirection direction = SortFieldDirection.ASC;
                String[] pair = specifier.split("\\s+");
                if (pair.length == 2) {
                    field = pair[0];
                    if ("descending".startsWith(pair[1])) {
                        direction = SortFieldDirection.DESC;
                    }
                } else if (pair.length == 1) {
                    field = pair[0];
                } else {
                    throw new QueryException("Invalid sort field: '" + specifier + "'");
                }
                SortField sortField = null;
                if ("uri".equals(field) || "type".equals(field) || "name".equals(field)) {
                    sortField = new SimpleSortField(field, direction);
                } else {
                    String prefix = null;
                    String name = null;

                    String[] components = field.split(":");
                    if (components.length == 2) {
                        prefix = components[0];
                        name = components[1];
                    } else if (components.length == 1) {
                        name = components[0];
                    } else {
                        throw new QueryException("Unknown sort field: '" + field + "'");
                    }
                    PropertyTypeDefinition def =
                        propertyManager.getPropertyDefinitionByPrefix(prefix, name);
                    sortField = new PropertySortField(def, direction);
                }
                if (referencedFields.contains(field)) {
                    throw new QueryException(
                        "Sort field '" + field + "' occurs more than once");
                }
                referencedFields.add(field);
                result.add(sortField);
            }

            if (result.isEmpty()) {
                return null;
            }
            return new SortingImpl(result);
        }
    }

}
