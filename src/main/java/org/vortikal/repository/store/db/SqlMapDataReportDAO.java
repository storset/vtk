/* Copyright (c) 2008, University of Oslo, Norway
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.reporting.DataReportException;
import org.vortikal.repository.reporting.Pair;
import org.vortikal.repository.reporting.PropertyValueFrequencyQuery;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFactory;
import org.vortikal.repository.store.DataReportDAO;

/**
 * TODO: Currently does not support queries on properties stored in vortex_resource. 
 *
 * @author oyviste
 */
public class SqlMapDataReportDAO  extends AbstractSqlMapDataAccessor 
    implements DataReportDAO, InitializingBean {
    
    private Set<PropertyTypeDefinition> vortexResourcePropDefs;
    private ResourceTypeTree resourceTypeTree;
    
    @Override
    public void initDao() {
        // XXX: Add all properties stored in vortex_resource table here, query on them 
        // is not supported yet ..
        this.vortexResourcePropDefs = new HashSet<PropertyTypeDefinition>();
        Namespace ns = Namespace.DEFAULT_NAMESPACE;
        vortexResourcePropDefs.add(this.resourceTypeTree.getPropertyTypeDefinition(ns, "createdBy"));
        vortexResourcePropDefs.add(this.resourceTypeTree.getPropertyTypeDefinition(ns, "creationTime"));
        vortexResourcePropDefs.add(this.resourceTypeTree.getPropertyTypeDefinition(ns, "owner"));
        vortexResourcePropDefs.add(this.resourceTypeTree.getPropertyTypeDefinition(ns, "contentType"));
        vortexResourcePropDefs.add(this.resourceTypeTree.getPropertyTypeDefinition(ns, "characterEncoding"));
        vortexResourcePropDefs.add(this.resourceTypeTree.getPropertyTypeDefinition(ns, "guessedCharacterEncoding"));
        vortexResourcePropDefs.add(this.resourceTypeTree.getPropertyTypeDefinition(ns, "userSpecifiedCharacterEncoding"));
        vortexResourcePropDefs.add(this.resourceTypeTree.getPropertyTypeDefinition(ns, "contentLanguage"));
        vortexResourcePropDefs.add(this.resourceTypeTree.getPropertyTypeDefinition(ns, "lastModified"));
        vortexResourcePropDefs.add(this.resourceTypeTree.getPropertyTypeDefinition(ns, "modifiedBy"));
        vortexResourcePropDefs.add(this.resourceTypeTree.getPropertyTypeDefinition(ns, "contentLastModified"));
        vortexResourcePropDefs.add(this.resourceTypeTree.getPropertyTypeDefinition(ns, "contentModifiedBy"));
        vortexResourcePropDefs.add(this.resourceTypeTree.getPropertyTypeDefinition(ns, "propertiesLastModified"));
        vortexResourcePropDefs.add(this.resourceTypeTree.getPropertyTypeDefinition(ns, "propertiesModifiedBy"));
        vortexResourcePropDefs.add(this.resourceTypeTree.getPropertyTypeDefinition(ns, "contentLength"));
    }

    public List<Pair<Value, Integer>> executePropertyFrequencyValueQuery(
                                            String token,
                                            PropertyValueFrequencyQuery query) 
        throws DataReportException {
        
        PropertyTypeDefinition def = query.getPropertyTypeDefintion();
        if (this.vortexResourcePropDefs.contains(def)) {
            throw new DataReportException("Query on property type '" + def.getName() + "' not supported yet.");
        }
        
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("name", def.getName());
        if (! def.getNamespace().equals(Namespace.DEFAULT_NAMESPACE)) {
            params.put("namespace", def.getNamespace().getUri());
        }
        
        if (query.getLimit() != PropertyValueFrequencyQuery.LIMIT_UNLIMITED) {
            params.put("limit", query.getLimit());
        }
        
        params.put("ordering", query.getOrdering());
        
        if (query.getUriScope() != null && !"/".equals(query.getUriScope().getUri())) {
            params.put("uriWildcard", SqlDaoUtils.getUriSqlWildcard(
                    query.getUriScope().getUri(), SqlMapDataAccessor.SQL_ESCAPE_CHAR));
        }
        
        String sqlMap = getSqlMap("dataReportPropValueFrequencyExtraPropEntry");
        
        List<Map<String, Object>> result 
            = getSqlMapClientTemplate().queryForList(sqlMap, params);
        
        List<Pair<Value, Integer>> retval = 
                                    new ArrayList<Pair<Value, Integer>>();
        
        for (Map row: result) {
            String stringValue = (String)row.get("value");
            Integer frequency = (Integer)row.get("frequency");
            Value value = ValueFactory.getInstance().createValue(stringValue, def.getType());
            retval.add(new Pair<Value, Integer>(value, frequency));
        }
        
        return retval;
    }

    @Required
    public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
    }

}
