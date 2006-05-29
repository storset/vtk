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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.DOMOutputter;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;

import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.query.QueryException;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repositoryimpl.PropertyManager;
import org.vortikal.repositoryimpl.query.query.PropertySortField;
import org.vortikal.repositoryimpl.query.query.Query;
import org.vortikal.repositoryimpl.query.query.SimpleSortField;
import org.vortikal.repositoryimpl.query.query.SortField;
import org.vortikal.repositoryimpl.query.query.SortFieldDirection;
import org.vortikal.repositoryimpl.query.query.Sorting;
import org.vortikal.repositoryimpl.query.query.SortingImpl;
import org.vortikal.security.SecurityContext;


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


    public org.w3c.dom.NodeList executeQuery(String token, String query, String sort,
                                             String maxResults) throws QueryException {
        Element rootElement = null;

        try {
            int limit = Integer.parseInt(maxResults);
            if (limit > this.maxResults) {
                limit = this.maxResults;
            }
            Sorting sorting = this.sortParser.parseSortString(sort);
            ResultSet rs = this.queryManager.execute(token, query, sorting,
                                                     limit);
            rootElement = resultSetToElement(rs);
        } catch (Exception e) {
            logger.warn("Error occurred while performing query: '" + query + "'", e);
            rootElement = new Element("error");
            rootElement.setAttribute("exception", e.getClass().getName());
            rootElement.setAttribute("query", query);
            rootElement.setAttribute("sort", sort);
            String msg = e.getMessage();
            if (msg == null) msg = "No message";
            rootElement.setText(msg);
        }
        Document doc = new Document(rootElement);
        return getW3CNodeList(doc);
    }


    private Element resultSetToElement(ResultSet resultSet) {
        Element resultElement = new Element("results");
        resultElement.setAttribute("size", String.valueOf(resultSet.getSize()));
        for (Iterator i = resultSet.iterator(); i.hasNext();) {
            PropertySet propertySet = (PropertySet) i.next();
            resultElement.addContent(propertySetToElement(propertySet));
        }
        return resultElement;
    }


    private Element propertySetToElement(PropertySet propertySet) {
        Element propertySetElement = new Element("propertyset");
        propertySetElement.setAttribute("uri", propertySet.getURI());
        propertySetElement.setAttribute("name", propertySet.getName());
        propertySetElement.setAttribute("type", propertySet.getResourceType());
        for (Iterator i = propertySet.getProperties().iterator(); i.hasNext();) {
            Property property = (Property) i.next();
            Element propertyElement = propertyToElement(property);
            if (propertyElement != null) {
                propertySetElement.addContent(propertyElement);
            }
        }
        return propertySetElement;
    }
    

    private Element propertyToElement(Property property) {
        if (property.getDefinition() == null) {
            return null;
        }

        Element propertyElement = new Element("property");
        String namespaceUri = property.getNamespace().getUri();
        if (namespaceUri != null) {
            propertyElement.setAttribute("namespace", namespaceUri);
        }
        String prefix = property.getNamespace().getPrefix();
        if (prefix != null) {
            propertyElement.setAttribute("name", prefix + ":" + property.getName());
        } else {
            propertyElement.setAttribute("name", property.getName());
        }
        
        if (property.getDefinition().isMultiple()) {
            Element valuesElement = new Element("values");
            Value[] values = property.getValues();
            for (int i = 0; i < values.length; i++) {
                Element valueElement = new Element("value");
                valueElement.setText(values[i].toString());
                valuesElement.addContent(valueElement);
            }
            propertyElement.addContent(valuesElement);
        } else {
            Element valueElement = new Element("value");
            Value value = property.getValue();
            valueElement.setText(value.toString());
            propertyElement.addContent(valueElement);
        }
        propertyElement.setAttribute("name", property.getName());
        return propertyElement;
    }
    

    
    private org.w3c.dom.NodeList getW3CNodeList(org.jdom.Document jdomDocument) {
        org.w3c.dom.Document domDoc = null;

        try {
            DOMOutputter oupt = new DOMOutputter();
            domDoc = oupt.output(jdomDocument);
        }
        catch (Exception e) {}
        
        return domDoc != null ? domDoc.getDocumentElement().getChildNodes() : null;
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
