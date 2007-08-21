/* Copyright (c) 2007, University of Oslo, Norway
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
package org.vortikal.repositoryimpl.store.db;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repositoryimpl.PropertyManager;
import org.vortikal.repositoryimpl.PropertySetImpl;
import org.vortikal.repositoryimpl.store.PropertySetHandler;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalFactory;

import com.ibatis.sqlmap.client.event.RowHandler;

/**
 *
 */
class PropertySetRowHandler implements RowHandler {

    private static final Log LOG = 
                LogFactory.getLog(PropertySetRowHandler.class);

    // Client callback for handling retrieved property set instances 
    protected PropertySetHandler clientHandler;
    
    protected PropertyManager propertyManager;
    protected PrincipalFactory principalFactory;
    
    
    // Need to keep track of current property set ID since many rows from iBATIS
    // can map to a single property set. The iteration from the database is 
    // ordered, so ID change signals a new PropertySet
    protected Integer currentId = null;
    protected List<Map> rowValueBuffer = new ArrayList<Map>();
    
    public PropertySetRowHandler(PropertySetHandler clientHandler,
                                 PropertyManager propertyManager,
                                 PrincipalFactory principalFactory) {
        this.clientHandler = clientHandler;
        this.propertyManager = propertyManager;
        this.principalFactory = principalFactory;
    }
    
        
    /**
     * iBATIS callback
     */
    public void handleRow(Object valueObject) {
        
        Map rowMap = (Map)valueObject;
        Integer id = (Integer)rowMap.get("id");
        
        if (this.currentId != null && !this.currentId.equals(id)) {
            // New property set encountered in row iteration, flush out current.
            PropertySet propertySet = createPropertySet(this.rowValueBuffer);
            
            // Do client callback 
            this.clientHandler.handlePropertySet(propertySet);
            
            // Clear current row buffer
            this.rowValueBuffer.clear();
        }
        
        this.currentId = id;
        this.rowValueBuffer.add(rowMap);
    }
    
    /**
     * Call-back to flush out the last rows. 
     * Must be called after iBATIS has finished calling {@link #handleRow(Object)}).
     */
    public void handleLastBufferedRows() {
        if (this.rowValueBuffer.isEmpty()){
            return;
        }
        
        PropertySet propertySet = createPropertySet(this.rowValueBuffer);
        this.clientHandler.handlePropertySet(propertySet);    
    }
    
    private PropertySet createPropertySet(List<Map> rowBuffer) {
        
        Map firstRow = rowBuffer.get(0);
        
        PropertySetImpl propertySet = new PropertySetImpl();
        String uri = (String)firstRow.get("uri");

        propertySet.setUri(uri);
        populateStandardProperties(firstRow, propertySet);
        populateAncestorIds(firstRow, propertySet);
        populateExtraProperties(rowBuffer, propertySet);
        
        
        return propertySet;
    }
    
    protected void populateStandardProperties(Map row, PropertySetImpl propertySet) {

        // ID
        propertySet.setID(((Integer)row.get("id")).intValue());
        
        // isCollection
        boolean collection = "Y".equals(row.get("isCollection"));
        Property prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, PropertyType.COLLECTION_PROP_NAME,
                Boolean.valueOf(collection));
            propertySet.addProperty(prop);
        
        // createdBy
        Principal createdBy = principalFactory.getUserPrincipal(
                                                    (String)row.get("createdBy"));
        prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, PropertyType.CREATEDBY_PROP_NAME,
                createdBy);
        propertySet.addProperty(prop);

        // creationTime 
        prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, PropertyType.CREATIONTIME_PROP_NAME,
                (Date)row.get("creationTime"));
            propertySet.addProperty(prop);

        // owner
        Principal principal = principalFactory.getUserPrincipal((String)row.get("owner"));
        prop = propertyManager.createProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.OWNER_PROP_NAME,
            principal);
        propertySet.addProperty(prop);

        // contentType
        String string = (String)row.get("contentType");
        if (string != null) {
            prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, 
                PropertyType.CONTENTTYPE_PROP_NAME,
                string);
            propertySet.addProperty(prop);
        }
        
        // characterEncoding
        string = (String)row.get("characterEncoding");
        if (string != null) {
            prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, 
                PropertyType.CHARACTERENCODING_PROP_NAME,
                string);
            propertySet.addProperty(prop);
        }
        
        // guessedCharacterEncoding
        string = (String)row.get("guessedCharacterEncoding");
        if (string != null) {
            prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, 
                PropertyType.CHARACTERENCODING_GUESSED_PROP_NAME,
                string);
            propertySet.addProperty(prop);
        }
        
        // userSpecifiedCharacterEncoding
        string = (String)row.get("userSpecifiedCharacterEncoding");
        if (string != null) {
            prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, 
                PropertyType.CHARACTERENCODING_USER_SPECIFIED_PROP_NAME,
                string);
            propertySet.addProperty(prop);
        }
        
        // contentLanguage
        string = (String)row.get("contentLanguage");
        if (string != null) {
            prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, 
                PropertyType.CONTENTLOCALE_PROP_NAME,
                string);
            propertySet.addProperty(prop);
        }
        
        // lastModified
        prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, PropertyType.LASTMODIFIED_PROP_NAME,
                (Date)row.get("lastModified"));
        propertySet.addProperty(prop);

        // modifiedBy
        principal = principalFactory.getUserPrincipal((String)row.get("modifiedBy"));
        prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, PropertyType.MODIFIEDBY_PROP_NAME,
                principal);
        propertySet.addProperty(prop);

        // contentLastModified
        prop = propertyManager.createProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.CONTENTLASTMODIFIED_PROP_NAME,
            (Date)row.get("contentLastModified"));
        propertySet.addProperty(prop);

        // contentModifiedBy
        principal = principalFactory.getUserPrincipal((String)row.get("contentModifiedBy"));
        prop = propertyManager.createProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.CONTENTMODIFIEDBY_PROP_NAME,
            principal);
        propertySet.addProperty(prop);

        // propertiesLastModified
        prop = propertyManager.createProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.PROPERTIESLASTMODIFIED_PROP_NAME,
            (Date)row.get("propertiesLastModified"));
        propertySet.addProperty(prop);

        // propertiesModifiedBy
        principal = principalFactory.getUserPrincipal((String)row.get("propertiesModifiedBy"));
        prop = propertyManager.createProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.PROPERTIESMODIFIEDBY_PROP_NAME,
            principal);
        propertySet.addProperty(prop);
        
        if (!collection) {
            Long contentLength = (Long)row.get("contentLength");
            prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, PropertyType.CONTENTLENGTH_PROP_NAME,
                contentLength);
            propertySet.addProperty(prop);
        }
        
        propertySet.setResourceType((String)row.get("resourceType"));
        
        Integer aclInheritedFrom = (Integer)row.get("aclInheritedFrom");
        if (aclInheritedFrom == null) {
            propertySet.setAclInheritedFrom(-1);
        } else {
            propertySet.setAclInheritedFrom(aclInheritedFrom.intValue());
        }
    }
    
    protected void populateExtraProperties(List<Map> rowBuffer, 
                                           PropertySetImpl propertySet) {
        
        Map<SqlDaoUtils.PropHolder, List<String>> propMap = 
                            new HashMap<SqlDaoUtils.PropHolder, List<String>>();
        
        for (Map row: rowBuffer) {

            SqlDaoUtils.PropHolder holder = new SqlDaoUtils.PropHolder();
            holder.namespaceUri = (String)row.get("namespace");
            holder.name = (String)row.get("name");
            holder.resourceId = ((Integer)row.get("id")).intValue();
        
            if (holder.name != null) { 

                List<String> values = propMap.get(holder);
                if (values == null) {
                    values = new ArrayList<String>();
                    holder.type = ((Integer)row.get("typeId")).intValue();
                    holder.values = values;
                    propMap.put(holder, values);
                }
                values.add((String)row.get("value"));
            }

        }

        for (Iterator i = propMap.keySet().iterator(); i.hasNext();) {
            SqlDaoUtils.PropHolder holder = (SqlDaoUtils.PropHolder) i.next();
            Property property = this.propertyManager.createProperty(
                holder.namespaceUri, holder.name, 
                holder.values.toArray(new String[]{}));
            propertySet.addProperty(property);
        }

    }
    
    protected void populateAncestorIds(Map row, PropertySetImpl propSet) {
        String idString = (String)row.get("ancestorIds");
        propSet.setAncestorIds(getAncestorIdsFromString(idString));
    }

    /**
     * XXX: Horrible hack mapping from a single string of ids returned by database 
     * function for getting ancestor ids, to a Java int[] array. Needed because
     * we haven't ported Oracle-function to something more intelligent, and we
     * don't want to have two different Java-classes for Oracle and PostgreSQL.
     * 
     * Only temporary, until we get the id-problem 
     * sorted out, either by not using procedures/functions at all, or change
     * how we do the SQL/procedures.
     * 
     * @param idString Single-space separated string of ancestor ids 
     *                 (where ids are parseable integers) 
     * @return array of integers
     */
    private int[] getAncestorIdsFromString(String idString) {
        if (idString == null || "".equals(idString)) {
            return new int[0];
        }
        
        idString = idString.trim();
        
        int n = 1, p = -1, c = 0;
        while ((p = idString.indexOf(' ', p+1)) != -1) ++n;
        
        int[] ids = new int[n];
        
        p = 0; n = -1;
        while ((n = idString.indexOf(' ', (p = n+1))) != -1) {
            ids[c++] = Integer.parseInt(idString.substring(p, n));
        }
        ids[c] = Integer.parseInt(idString.substring(p, idString.length()));
        
        return ids;
    }


}
