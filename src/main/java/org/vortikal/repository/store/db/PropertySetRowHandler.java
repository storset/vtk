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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertyImpl;
import org.vortikal.repository.PropertySetImpl;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.store.PropertySetHandler;
import org.vortikal.repository.store.db.SqlDaoUtils.PropHolder;
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
    
    private final Map<Integer, Set<Principal>> aclReadPrincipalsCache
            = new LinkedHashMap<Integer, Set<Principal>>() {
        @Override
        protected boolean removeEldestEntry(Entry<Integer, Set<Principal>> eldest) {
            return size() > 2000;
        }
    };
    
    private final Map<Path, List<Property>> inheritablePropertiesCache
            = new LinkedHashMap<Path, List<Property>>() {
        @Override
        protected boolean removeEldestEntry(Entry<Path, List<Property>> eldest) {
            return size() > 2000;
        }
    };
    
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
        Set<Principal> aclReadPrincipals = this.aclReadPrincipalsCache.get(aclResourceId);
        
        if (aclReadPrincipals == null) {
            // Not found in cache
            aclReadPrincipals = this.indexDao.loadAclReadPrincipals(propertySet);
            
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
        
        // Standard props found in vortex_resource table:
        populateStandardProperties(firstRow, propertySet);
        
        // Add any inherited properties
        populateInheritedProperties(propertySet);
        
        // Add extra props set directly on node last, to allow override of any inherited props:
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
    
    private void populateExtraProperties(List<Map<String, Object>> rowBuffer, 
                                           PropertySetImpl propertySet) {
        
        Map<PropHolder, List<Object>> propMap = 
                            new HashMap<PropHolder, List<Object>>();
        
        for (Map<String, Object> row: rowBuffer) {
            if ((Boolean)row.get("binary")) {
                // Skip all binary prop rows
                continue;
            }
            PropHolder holder = new PropHolder();
            holder.namespaceUri = (String)row.get("namespace");
            holder.name = (String)row.get("name");
            holder.resourceId = (Integer)row.get("id");
            holder.inheritable = (Boolean)row.get("inheritable");
            
            List<Object> values = propMap.get(holder);
            if (values == null) {
                values = new ArrayList<Object>(2);
                holder.values = values;
                propMap.put(holder, values);
            }
            values.add(row.get("value"));
        }

        for (PropHolder holder: propMap.keySet()) {
            propertySet.addProperty(createProperty(holder));
        }
    }
    
    
    private void populateInheritedProperties(PropertySetImpl propSet) {
        // Root node cannot inherit anything
        if (propSet.getURI().isRoot()) return;

        // Process all ancestors starting with parent and going up towards root
        final Path parent = propSet.getURI().getParent();
        final List<Path> paths = parent.getPaths();
        
        // Do we have all ancestor paths in cache ?
        final List<Path> cacheMissPaths = new ArrayList<Path>(2);
        Map<Path, List<Property>> loadedInheritablePropertiesMap = null;
        for (Path p: paths) {
            if (!inheritablePropertiesCache.containsKey(p)) {
                cacheMissPaths.add(p);
            }
        }
        if (!cacheMissPaths.isEmpty()) {
            // Do not touch cache before we have resolved everything for current resource
            // since the cache will expire old entries when updated. Keep newly loaded things in
            // local variable instead, and update cache with loaded elements when finished resolving.
            loadedInheritablePropertiesMap = loadInheritablePropertiesMap(cacheMissPaths);
        }

        // Populate effective set of inherited properties
        final Set<String> encountered = new HashSet<String>();
        for (int i=paths.size()-1; i >= 0; i--) {
            Path p = paths.get(i);
            List<Property> inheritableProps = inheritablePropertiesCache.get(p);
            if (inheritableProps == null) {
                // Was a cache miss, look it up in loaded map
                inheritableProps = loadedInheritablePropertiesMap.get(p);
            }
            
            for (Property property: inheritableProps) {
                String namespaceUri = property.getDefinition().getNamespace().getUri();
                String name = property.getDefinition().getName();
                if (encountered.add(namespaceUri + ":" + name)) {
                    // First occurence from bottom to top, add it.
                    // (it will override anything from ancestors):
                    propSet.addProperty(property);
                }
            }
        }
        
        // Add any loaded inheritable props to cache
        if (loadedInheritablePropertiesMap != null) {
            inheritablePropertiesCache.putAll(loadedInheritablePropertiesMap);
        }
    }
    
    /**
     * Loads inheritable properties for selected paths from database.
     * 
     * Paths with no inheritable props will map to the empty list (this
     * method will never add mappings with <code>null</code> values).
     * 
     * All <code>Property</code> instances created by this method
     * will have the inherited-flag set to <code>true</code>.
     * 
     * @param uri
     * @return 
     */
    private Map<Path, List<Property>> loadInheritablePropertiesMap(List<Path> paths) {

        // Initialize to empty list of inheritable props per selected path
        final Map<Path, List<Property>> inheritablePropsMap = new HashMap<Path, List<Property>>();
        for (Path p : paths) {
            inheritablePropsMap.put(p, Collections.EMPTY_LIST);
        }
        
        // Load from database
        final List<Map<String,Object>> rows = indexDao.loadInheritablePropertyRows(paths);
        if (rows.isEmpty()) {
            return inheritablePropsMap;
        }
        
        // Aggretate property rows and set up sparse inheritable map for selected paths
        final Map<Path, Set<PropHolder>> inheritableHolderMap = new HashMap<Path, Set<PropHolder>>();
        final Map<PropHolder, List<Object>> propValuesMap = new HashMap<PropHolder, List<Object>>();
        for (Map<String, Object> propEntry : rows) {
            if ((Boolean)propEntry.get("binary")) {
                // Skip all binary property rows
                continue;
            }
            
            final PropHolder holder = new PropHolder();
            holder.namespaceUri = (String) propEntry.get("namespaceUri");
            holder.name = (String) propEntry.get("name");
            holder.resourceId = (Integer) propEntry.get("resourceId");
            holder.propID = propEntry.get("id");
            holder.inheritable = true;
            
            List<Object> values = propValuesMap.get(holder);
            if (values == null) {
                // New property
                values = new ArrayList<Object>(2);
                holder.values = values;
                propValuesMap.put(holder, values);
                
                // Link current canonical PropHolder instance to inheritable map
                Path p = Path.fromString((String) propEntry.get("uri"));
                Set<PropHolder> set = inheritableHolderMap.get(p);
                if (set == null) {
                    set = new HashSet<PropHolder>();
                    inheritableHolderMap.put(p, set);
                }
                set.add(holder);
            }

            // Aggregate current property's value (binary values ignored)
            values.add(propEntry.get("value"));
        }

        for (Map.Entry<Path, Set<PropHolder>> entry: inheritableHolderMap.entrySet()) {
            Path p = entry.getKey();
            Set<PropHolder> propHolders = entry.getValue();
            List<Property> props = new ArrayList<Property>(propHolders.size());
            for (PropHolder holder : propHolders) {
                Property prop = createProperty(holder);
                // All prop instances in cache shall have inherited flag set to 'true'
                ((PropertyImpl) prop).setInherited(true);
                props.add(prop);
            }
            inheritablePropsMap.put(p, props);
        }
        
        return inheritablePropsMap;
    }
    
    private Property createProperty(PropHolder holder) {
        assert (!holder.binary);
        
        Namespace namespace = this.resourceTypeTree.getNamespace(holder.namespaceUri);
        PropertyTypeDefinition propDef = this.resourceTypeTree.getPropertyTypeDefinition(
                namespace, holder.name);

        String[] stringValues = new String[holder.values.size()];
        for (int i = 0; i < stringValues.length; i++) {
            stringValues[i] = (String) holder.values.get(i);
        }
        return propDef.createProperty(stringValues);
    }
    
}
