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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;

import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repositoryimpl.PropertyManager;
import org.vortikal.repositoryimpl.query.WildcardPropertySelect;
import org.vortikal.repositoryimpl.query.query.PropertySelect;
import org.vortikal.repositoryimpl.query.query.PropertySortField;
import org.vortikal.repositoryimpl.query.query.SimpleSortField;
import org.vortikal.repositoryimpl.query.query.SortField;
import org.vortikal.repositoryimpl.query.query.SortFieldDirection;
import org.vortikal.repositoryimpl.query.query.Sorting;
import org.vortikal.repositoryimpl.query.query.SortingImpl;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;


/**
 * Utility class for performing searches returning result sets wrapped
 * in an XML structure.
 *
 * XXX: move this class to another package (has org.vortikal.web.*
 * dependencies among other things).
 *
 */
public class XmlSearcher implements InitializingBean {

    private Log logger = LogFactory.getLog(this.getClass());

    private Searcher searcher;
    private QueryManager queryManager;
    private PropertyManager propertyManager;
    private int maxResults = 1000;
    private String defaultDateFormatKey;
    private Map namedDateFormats = new HashMap();
    private Repository repository;
    private String defaultLocale = Locale.getDefault().getLanguage();
    

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

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setDefaultDateFormatKey(String defaultDateFormatKey) {
        this.defaultDateFormatKey = defaultDateFormatKey;
    }
    
    public void setNamedDateFormats(Map namedDateFormats) {
        this.namedDateFormats = namedDateFormats;
    }
    
    public void setDefaultLocale(String defaultLocale) {
        this.defaultLocale = defaultLocale;
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
        if (this.repository == null) {
            throw new BeanInitializationException(
                "JavaBean property 'repository' not set");
        }
        if (this.namedDateFormats == null || this.namedDateFormats.isEmpty()) {
            throw new BeanInitializationException(
                "JavaBean property 'namedDateFormats' not set");
        }
        
        for (Iterator i = this.namedDateFormats.keySet().iterator(); i.hasNext();) {
            Object key = i.next();
            if (!(key instanceof String)) {
                throw new BeanInitializationException(
                    "All keys in the 'namedDateFormats' map must be of type java.lang.String"
                    + "(found " + key.getClass().getName() + ")");
            }
            Object value = this.namedDateFormats.get(key);
            if (!(value instanceof FastDateFormat)) {
                throw new BeanInitializationException(
                    "All values in the 'namedDateFormats' map must be of type "
                    + FastDateFormat.class.getName()
                    + "(found " + value.getClass().getName() + ")");
            }
        }

        if (this.defaultDateFormatKey == null) {
            throw new BeanInitializationException(
                "JavaBean property 'defaultDateFormatKey' not set");
        }
        if (!this.namedDateFormats.containsKey(this.defaultDateFormatKey)) {
            throw new BeanInitializationException(
                "The map 'namedDateFormats' must contain an entry specified by the "
                + "'defaultDateFormatKey' JavaBean property");
        }

        if (this.defaultLocale == null) {
            throw new BeanInitializationException(
                "JavaBean property 'defaultLocale' not set");
        }
    }
    

    public NodeList executeQuery(String query, String sort,
                                             String maxResults) throws QueryException {
        return executeQuery(query, sort, maxResults, null);
    }


    public NodeList executeQuery(String query, String sort, String maxResults,
                                 String fields) throws QueryException {
        
        String token = null;
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        if (securityContext != null) {
            token = securityContext.getToken();
        }
        Document doc = executeDocumentQuery(token, query, sort, maxResults, fields);
        return doc.getDocumentElement().getChildNodes();
    }
  

    public Document executeDocumentQuery(String token, String query,
                                         String sort, String maxResults) throws QueryException {
        return executeDocumentQuery(token, query, sort, maxResults, null);
    }
    


    public Document executeDocumentQuery(String token, String query,
                                         String sort, String maxResults,
                                         String fields) throws QueryException {
        Set properties = null;
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
            SearchEnvironment envir = new SearchEnvironment(sort, fields);
            PropertySelect select = envir.getPropertySelect();
            Sorting sorting = envir.getSorting();

            if (select == null) {
                select = WildcardPropertySelect.WILDCARD_PROPERTY_SELECT;
            }
            if (logger.isDebugEnabled()) {
                logger.debug("About to execute query: " + query + ": sort = " + sorting
                             + ", limit = " + limit + ", envir = " + envir);
            }

            ResultSet rs = this.queryManager.execute(token, query, sorting, limit, select);
            
            addResultSetToDocument(rs, doc, envir);
        } catch (Exception e) {
            this.logger.warn("Error occurred while performing query: '" + query + "'", e);
            
            Element errorElement = doc.createElement("error");
            doc.appendChild(errorElement);
            errorElement.setAttribute("exception", e.getClass().getName());
            errorElement.setAttribute("query", query);
            errorElement.setAttribute("sort", sort);
            String msg = e.getMessage() != null ? e.getMessage() : "No message";
            Text text = doc.createTextNode(msg);
            errorElement.appendChild(text);

        }
        return doc;
    }
    

    private void addResultSetToDocument(ResultSet rs, Document doc, SearchEnvironment envir) {
        long start = System.currentTimeMillis();
        
        Element resultElement = doc.createElement("results");
        doc.appendChild(resultElement);
        resultElement.setAttribute("size", String.valueOf(rs.getSize()));
        resultElement.setAttribute("totalHits", String.valueOf(rs.getTotalHits()));
        for (Iterator i = rs.iterator(); i.hasNext();) {
            PropertySet propSet = (PropertySet)i.next();
            addPropertySetToResults(doc, resultElement, propSet, envir);
        }
        
        if (this.logger.isDebugEnabled()) {
            long now = System.currentTimeMillis();
            this.logger.debug("Building XML result set took " + (now - start) + " ms");
        }
    }
    
    private void addPropertySetToResults(Document doc, Element resultsElement, 
                                         PropertySet propSet, SearchEnvironment envir) {
        
        Element propertySetElement = doc.createElement("resource");
        resultsElement.appendChild(propertySetElement);
        propertySetElement.setAttribute("uri", propSet.getURI());
        propertySetElement.setAttribute("name", propSet.getName());
        propertySetElement.setAttribute("type", propSet.getResourceType());

        for (Iterator i = propSet.getProperties().iterator(); i.hasNext();) {
            Property prop = (Property) i.next();
            addPropertyToPropertySetElement(doc, propertySetElement, prop, envir);
        }
        
    }
    
    private void addPropertyToPropertySetElement(Document doc, Element propSetElement,
                                                 Property prop, SearchEnvironment envir) {
        
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
        
        Locale locale = envir.getLocale();

        Set formatSet = envir.getFormats().getFormats(prop.getDefinition());
        if (!formatSet.contains(null)) {
            // Add default (null) format:
            formatSet.add(null);
        }

        for (Iterator iter = formatSet.iterator(); iter.hasNext();) {
            String format = (String) iter.next();
            if (prop.getDefinition().isMultiple()) {
                Element valuesElement = doc.createElement("values");
                if (format != null) {
                    valuesElement.setAttribute("format", format);
                }
                Value[] values = prop.getValues();
                for (int i = 0; i < values.length; i++) {
                    Element valueElement = valueElement(doc, values[i], format, locale);
                    valuesElement.appendChild(valueElement);
                }
                propertyElement.appendChild(valuesElement);
            } else {
                Value value = prop.getValue();
                Element valueElement = valueElement(doc, value, format, locale);
                if (format != null) {
                    valueElement.setAttribute("format", format);
                }

                propertyElement.appendChild(valueElement);
            }
        }
        propSetElement.appendChild(propertyElement);        
    }

    
    private Element valueElement(Document doc, Value value, String format, Locale locale) {
            Element valueElement = doc.createElement("value");
            Text text = doc.createTextNode(valueToString(value, format, locale));
            valueElement.appendChild(text);
            return valueElement;
    }
    

    private String valueToString(Value value, String format, Locale locale) {
        switch (value.getType()) {
            case PropertyType.TYPE_DATE:
                if (format == null) {
                    format = this.defaultDateFormatKey;
                }
                FastDateFormat f = null;
                    
                // Check if format refers to any of the
                // predefined (named) formats:
                String key = format + "_" + locale.getLanguage();

                f = (FastDateFormat) this.namedDateFormats.get(key);
                if (f == null) {
                    key = format;
                    f = (FastDateFormat) this.namedDateFormats.get(key);
                }
                try {
                    if (f == null) {
                        // Parse the given format
                        // XXX: formatter instances should be cached
                        f = FastDateFormat.getInstance(format, locale);
                    }
                    return f.format(value.getDateValue());
                } catch (Throwable t) {
                    return "Error: " + t.getMessage();
                }

            case PropertyType.TYPE_PRINCIPAL:
                return value.getPrincipalValue().getName();
            default:
                return value.toString();
        }
    }

    private class Formats {

        private Map formats = new HashMap();

        public void addFormat(PropertyTypeDefinition def, String format) {
            Set s = (Set) this.formats.get(def);
            if (s == null) {
                s = new HashSet();
                this.formats.put(def, s);
            }
            s.add(format);
        }

        public Set getFormats(PropertyTypeDefinition def) {
            Set set = (Set) this.formats.get(def);
            if (set == null) {
                set = new HashSet();
            }
            return set;
        }
        
        public String toString() {
            StringBuffer sb = new StringBuffer(this.getClass().getName());
            sb.append(": formats = ").append(this.formats);
            return sb.toString();
        }
    }


    private class SearchEnvironment {
        
        private HashSetPropertySelect select = null;
        private Sorting sort;
        private Formats formats = new Formats();
        private Locale locale = new Locale(defaultLocale);
        
        public SearchEnvironment(String sort, String fields) {
            parseSortString(sort);
            parseFields(fields);
            resolveLocale();
        }

        public PropertySelect getPropertySelect() {
            return this.select;
        }

        public Sorting getSorting() {
            return this.sort;
        }

        public Formats getFormats() {
            return this.formats;
        }

        public Locale getLocale() {
            return this.locale;
        }
        

        public String toString() {
            StringBuffer sb = new StringBuffer(this.getClass().getName());
            sb.append(": select = ").append(this.select);
            sb.append(", sort = ").append(this.sort);
            sb.append(", formats = ").append(this.formats);
            sb.append(", locale = ").append(this.locale);
            return sb.toString();
        }
        

        /**
         * Parses a sort specification of the syntax
         * <code>field(:asc|:desc)?(,field(:asc|:desc)?)*</code> and
         * produces a {@link Sorting} object.
         *
         * @param sortString the sort specification
         * @return a sort object, or <code>null</code> if the string does
         * not contain any valid sort fields.
         */
        public void parseSortString(String sortString) {
            if (sortString == null || "".equals(sortString.trim())) {
                return;
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
                        XmlSearcher.this.propertyManager.getPropertyDefinitionByPrefix(prefix, name);
                    sortField = new PropertySortField(def, direction);
                }
                if (referencedFields.contains(field)) {
                    throw new QueryException(
                        "Sort field '" + field + "' occurs more than once");
                }
                referencedFields.add(field);
                result.add(sortField);
            }

            if (!result.isEmpty()) {
                this.sort = new SortingImpl(result);
            }
        }


        private void parseFields(String fields) {
            if (fields == null || "".equals(fields.trim())) {
                return;
            }
            String[] fieldsArray = splitFields(fields);

            for (int i = 0; i < fieldsArray.length; i++) {
                String fullyQualifiedName = fieldsArray[i];
                if ("".equals(fullyQualifiedName.trim())) {
                    continue;
                }
                fullyQualifiedName = fullyQualifiedName.replaceAll("\\,", ",");
                String prefix = null;
                String name = fullyQualifiedName.trim();


                String format = null;
                int bracketStartPos = name.indexOf("[");
                if (bracketStartPos != -1 && bracketStartPos > 1) {
                    int bracketEndPos = name.indexOf("]", bracketStartPos);
                    if (bracketEndPos != -1) {
                        format = name.substring(bracketStartPos + 1, bracketEndPos);
                        name = name.substring(0, bracketStartPos);
                    }
                }

                int separatorPos = name.indexOf(":");
                if (separatorPos != -1) {
                    prefix = name.substring(0, separatorPos).trim();
                    name = name.substring(separatorPos + 1).trim();
                }
                
                PropertyTypeDefinition def =
                    propertyManager.getPropertyDefinitionByPrefix(prefix, name);

                if (def != null && format != null) {
                    this.formats.addFormat(def, format);
                }
                if (def != null) {
                    if (this.select == null) {
                        this.select = new HashSetPropertySelect();
                    }
                    this.select.addPropertyDefinition(def);
                }
            }
        }
        

        private void resolveLocale() {
            try {
                RequestContext requestContext = RequestContext.getRequestContext();
                SecurityContext securityContext = SecurityContext.getSecurityContext();
                String token = securityContext.getToken();
                String uri = requestContext.getResourceURI();
                Resource resource = repository.retrieve(token, uri, true);
                String contentLanguage = resource.getContentLanguage();
                if (contentLanguage != null) {
                    String lang = contentLanguage;
                    if (contentLanguage.indexOf("_") != -1) {
                        lang = contentLanguage.substring(0, contentLanguage.indexOf("_"));
                    }
                    this.locale = new Locale(lang);
                }
                
            } catch (Throwable t) { 
                if (logger.isDebugEnabled()) {
                    logger.debug("Unable to resolve locale of resource", t);
                }
            }
        }
        

        // Splits fields on ',' characters, allowing commas to appear
        // within (non-nested) brackets.
        private String[] splitFields(String fields) {
            List l = new ArrayList();
            String s = new String();
            boolean insideBrackets = false;
            for (int i = 0; i < fields.length(); i++) {
                if (',' == fields.charAt(i) && !insideBrackets) {
                    l.add(s);
                    s = new String();
                } else {

                    if ('[' == fields.charAt(i)) {
                        if (!insideBrackets) {
                            insideBrackets = true;
                        }

                    } else if (']' == fields.charAt(i)) {
                        if (insideBrackets) {
                            insideBrackets = false;
                        }
                    }
                    s += fields.charAt(i);
                }

            }
            if (!"".equals(s)) {
                l.add(s);
            }
            return (String[]) l.toArray(new String[l.size()]);
        }
    }


    private class HashSetPropertySelect implements PropertySelect {
        private Set properties = new HashSet();
        
        public void addPropertyDefinition(PropertyTypeDefinition def) {
            this.properties.add(def);
        }

        public boolean isIncludedProperty(PropertyTypeDefinition def) {
            return this.properties.contains(def);
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(this.getClass().getName()).append(":");
            sb.append("propertiess = ").append(this.properties);
            return sb.toString();
        }
        
    }

}
