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
package org.vortikal.repository.store.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySetImpl;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.store.PropertySetHandler;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalFactory;

import com.ibatis.sqlmap.client.event.RowHandler;



/**
 *
 */
class PropertySetRowHandler implements RowHandler {

    // Client callback for handling retrieved property set instances 
    protected PropertySetHandler clientHandler;
    

    // Need to keep track of current property set ID since many rows from iBATIS
    // can map to a single property set. The iteration from the database is 
    // ordered, so ID change signals a new PropertySet
    protected Integer currentId = null;
    protected List<Map<String, Object>> rowValueBuffer = new ArrayList<Map<String, Object>>();

    private final ResourceTypeTree resourceTypeTree;
    private final SqlMapIndexDao indexDao;
    private final PrincipalFactory principalFactory;
    
    private Map<Integer, Set<Principal>> aclReadPrincipalsCache
        = new HashMap<Integer, Set<Principal>>();
    
    public PropertySetRowHandler(PropertySetHandler clientHandler,
                                 ResourceTypeTree resourceTypeTree,
                                 PrincipalFactory principalFactory,
                                 SqlMapIndexDao indexDao) {
        this.clientHandler = clientHandler;
        this.resourceTypeTree = resourceTypeTree;
        this.principalFactory = principalFactory;
        this.indexDao = indexDao;
    }
    
    /**
     * iBATIS callback
     */
    @Override
    public void handleRow(Object valueObject) {
        
        @SuppressWarnings("unchecked")
        Map<String, Object> rowMap = (Map<String, Object>) valueObject;
        Integer id = (Integer)rowMap.get("id");
        
        if (this.currentId != null && !this.currentId.equals(id)) {
            // New property set encountered in row iteration, flush out current.
            PropertySetImpl propertySet = createPropertySet(this.rowValueBuffer);
            
            // Get ACL read principals
            Set<Principal> aclReadPrincipals = getAclReadPrincipals(propertySet);
            
            if (aclReadPrincipals != null) {
                // Do client callback (only if we have ACL, which means the resource is present in the database)
                this.clientHandler.handlePropertySet(propertySet, aclReadPrincipals);
            }
            
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
        
        PropertySetImpl propertySet = createPropertySet(this.rowValueBuffer);
        
        // Get ACL read principals
        Set<Principal> aclReadPrincipals = getAclReadPrincipals(propertySet);
        
        if (aclReadPrincipals != null) {
            // Do client callback (only if we have ACL, which means the resource is present in the database) 
            this.clientHandler.handlePropertySet(propertySet, aclReadPrincipals);
        } 
    }
    
    private Set<Principal> getAclReadPrincipals(PropertySetImpl propertySet) {
        
        Integer aclResourceId = propertySet.isInheritedAcl() ? 
                        propertySet.getAclInheritedFrom() : propertySet.getID();
                        
        // Try cache first:
        Set<Principal> aclReadPrincipals = this.aclReadPrincipalsCache.get(
                                                    aclResourceId);
        
        if (aclReadPrincipals == null) {
            // Not found in cache
            aclReadPrincipals = this.indexDao.getAclReadPrincipals(propertySet);
            
            if (aclReadPrincipals != null) {
                this.aclReadPrincipalsCache.put(aclResourceId, Collections.unmodifiableSet(aclReadPrincipals));
            }
        }
        
        return aclReadPrincipals;
    }

    private Principal getPrincipal(String id, Principal.Type type) {
         // Don't include metadata, not necessary for indexing-purposes, and it's costly,
        //  and we bog down the metadata-cache (lots of LDAP lookups).
        return this.principalFactory.getPrincipal(id, type, false);
    }
    
    private PropertySetImpl createPropertySet(List<Map<String, Object>> rowBuffer) {
        
        Map<String, Object> firstRow = rowBuffer.get(0);
        
        PropertySetImpl propertySet = new PropertySetImpl();
        String uri = (String)firstRow.get("uri");

        propertySet.setUri(Path.fromString(uri));
        populateStandardProperties(firstRow, propertySet);
        populateExtraProperties(rowBuffer, propertySet);
        
        return propertySet;
    }
    
    protected void populateStandardProperties(Map<String, Object> row, PropertySetImpl propertySet) {

        // ID
        propertySet.setID(((Integer)row.get("id")).intValue());
        
        // isCollection
        boolean collection = "Y".equals(row.get("isCollection"));
        PropertyTypeDefinition propDef = 
            this.resourceTypeTree.getPropertyTypeDefinition(
                Namespace.DEFAULT_NAMESPACE, PropertyType.COLLECTION_PROP_NAME);
        Property prop = propDef.createProperty(Boolean.valueOf(collection));
        propertySet.addProperty(prop);
        
        // createdBy
        Principal createdBy = getPrincipal((String)row.get("createdBy"), Principal.Type.USER);
        propDef = this.resourceTypeTree.getPropertyTypeDefinition(
                Namespace.DEFAULT_NAMESPACE, PropertyType.CREATEDBY_PROP_NAME);
        prop = propDef.createProperty(createdBy);
        propertySet.addProperty(prop);

        // creationTime 
        propDef = this.resourceTypeTree.getPropertyTypeDefinition(
                Namespace.DEFAULT_NAMESPACE, PropertyType.CREATIONTIME_PROP_NAME);
        prop = propDef.createProperty((Date)row.get("creationTime"));
        propertySet.addProperty(prop);

        // owner
        Principal principal = getPrincipal((String)row.get("owner"), Principal.Type.USER);
        propDef = this.resourceTypeTree.getPropertyTypeDefinition(
                Namespace.DEFAULT_NAMESPACE, PropertyType.OWNER_PROP_NAME);
        prop = propDef.createProperty(principal);
        propertySet.addProperty(prop);

        // contentType
        String string = (String)row.get("contentType");
        if (string != null) {
            propDef = this.resourceTypeTree.getPropertyTypeDefinition(
                    Namespace.DEFAULT_NAMESPACE, PropertyType.CONTENTTYPE_PROP_NAME);
            prop = propDef.createProperty(string);
            propertySet.addProperty(prop);
        }
        
        // characterEncoding
        string = (String)row.get("characterEncoding");
        if (string != null) {
            propDef = this.resourceTypeTree.getPropertyTypeDefinition(
                    Namespace.DEFAULT_NAMESPACE, PropertyType.CHARACTERENCODING_PROP_NAME);
            prop = propDef.createProperty(string);
            propertySet.addProperty(prop);
        }
        
        // guessedCharacterEncoding
        string = (String)row.get("guessedCharacterEncoding");
        if (string != null) {
            propDef = this.resourceTypeTree.getPropertyTypeDefinition(
                    Namespace.DEFAULT_NAMESPACE, PropertyType.CHARACTERENCODING_GUESSED_PROP_NAME);
            prop = propDef.createProperty(string);
            propertySet.addProperty(prop);
        }
        
        // userSpecifiedCharacterEncoding
        string = (String)row.get("userSpecifiedCharacterEncoding");
        if (string != null) {
            propDef = this.resourceTypeTree.getPropertyTypeDefinition(
                    Namespace.DEFAULT_NAMESPACE, PropertyType.CHARACTERENCODING_USER_SPECIFIED_PROP_NAME);
            prop = propDef.createProperty(string);
            propertySet.addProperty(prop);
        }
        
        // contentLanguage
        string = (String)row.get("contentLanguage");
        if (string != null) {
            propDef = this.resourceTypeTree.getPropertyTypeDefinition(
                    Namespace.DEFAULT_NAMESPACE, PropertyType.CONTENTLOCALE_PROP_NAME);
            prop = propDef.createProperty(string);
            propertySet.addProperty(prop);
        }
        
        // lastModified
        propDef = this.resourceTypeTree.getPropertyTypeDefinition(
                Namespace.DEFAULT_NAMESPACE, PropertyType.LASTMODIFIED_PROP_NAME);
        prop = propDef.createProperty((Date)row.get("lastModified"));
        propertySet.addProperty(prop);

        // modifiedBy
        principal = getPrincipal((String)row.get("modifiedBy"), Principal.Type.USER);
        propDef = this.resourceTypeTree.getPropertyTypeDefinition(
                Namespace.DEFAULT_NAMESPACE, PropertyType.MODIFIEDBY_PROP_NAME);
        prop = propDef.createProperty(principal);
        propertySet.addProperty(prop);

        // contentLastModified
        propDef = this.resourceTypeTree.getPropertyTypeDefinition(
                Namespace.DEFAULT_NAMESPACE, PropertyType.CONTENTLASTMODIFIED_PROP_NAME);
        prop = propDef.createProperty((Date)row.get("contentLastModified"));
        propertySet.addProperty(prop);

        // contentModifiedBy
        principal = getPrincipal((String)row.get("contentModifiedBy"), Principal.Type.USER);
        propDef = this.resourceTypeTree.getPropertyTypeDefinition(
                Namespace.DEFAULT_NAMESPACE, PropertyType.CONTENTMODIFIEDBY_PROP_NAME);
        prop = propDef.createProperty(principal);
        propertySet.addProperty(prop);

        // propertiesLastModified
        propDef = this.resourceTypeTree.getPropertyTypeDefinition(
                Namespace.DEFAULT_NAMESPACE, PropertyType.PROPERTIESLASTMODIFIED_PROP_NAME);
        prop = propDef.createProperty((Date)row.get("propertiesLastModified"));
        propertySet.addProperty(prop);

        // propertiesModifiedBy
        principal = getPrincipal((String)row.get("propertiesModifiedBy"), Principal.Type.USER);
        propDef = this.resourceTypeTree.getPropertyTypeDefinition(
                Namespace.DEFAULT_NAMESPACE, PropertyType.PROPERTIESMODIFIEDBY_PROP_NAME);
        prop = propDef.createProperty(principal);
        propertySet.addProperty(prop);
        
        if (!collection) {
            Long contentLength = (Long)row.get("contentLength");
            propDef = this.resourceTypeTree.getPropertyTypeDefinition(
                    Namespace.DEFAULT_NAMESPACE, PropertyType.CONTENTLENGTH_PROP_NAME);
            prop = propDef.createProperty(contentLength);
            propertySet.addProperty(prop);
        }
        
        propertySet.setResourceType((String)row.get("resourceType"));
        
        Integer aclInheritedFrom = (Integer)row.get("aclInheritedFrom");
        if (aclInheritedFrom == null) {
            propertySet.setAclInheritedFrom(PropertySetImpl.NULL_RESOURCE_ID);
        } else {
            propertySet.setAclInheritedFrom(aclInheritedFrom.intValue());
        }
    }
    
    protected void populateExtraProperties(List<Map<String, Object>> rowBuffer, 
                                           PropertySetImpl propertySet) {
        
        Map<SqlDaoUtils.PropHolder, List<Object>> propMap = 
                            new HashMap<SqlDaoUtils.PropHolder, List<Object>>();
        
        for (Map<String, Object> row: rowBuffer) {
            SqlDaoUtils.PropHolder holder = new SqlDaoUtils.PropHolder();
            holder.namespaceUri = (String)row.get("namespace");
            holder.name = (String)row.get("name");
            holder.resourceId = (Integer)row.get("id");
            holder.binary = (Boolean)row.get("binary");
        
            if (holder.name != null) { 
                List<Object> values = propMap.get(holder);
                if (values == null) {
                    values = new ArrayList<Object>(2);
                    holder.values = values;
                    propMap.put(holder, values);
                }
                values.add(row.get("value"));
            }
        }

        for (SqlDaoUtils.PropHolder holder: propMap.keySet()) {
            if (holder.binary) {
                // Skip binary props for system index
                continue;
            }
            
            Namespace namespace = this.resourceTypeTree.getNamespace(holder.namespaceUri);
            PropertyTypeDefinition propDef = this.resourceTypeTree.getPropertyTypeDefinition(
                    namespace, holder.name);
            
            String[] stringValues = new String[holder.values.size()];
            for (int i=0; i<stringValues.length; i++) {
                stringValues[i] = (String)holder.values.get(i);
            }
            Property property = propDef.createProperty(stringValues);
            propertySet.addProperty(property);
        }

    }
}
