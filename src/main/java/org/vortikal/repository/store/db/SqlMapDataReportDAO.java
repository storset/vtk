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
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.repository.reporting.DataReportException;
import org.vortikal.repository.reporting.Pair;
import org.vortikal.repository.reporting.PropertyValueFrequencyQuery;
import org.vortikal.repository.reporting.ReportScope;
import org.vortikal.repository.reporting.UriPrefixScope;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFactory;
import org.vortikal.repository.resourcetype.ValueFormatException;
import org.vortikal.repository.store.DataReportDAO;
import org.vortikal.repository.store.db.ibatis.PropertyValueFrequencyQueryParameter;

/**
 * @author oyviste
 */
public class SqlMapDataReportDAO  extends AbstractSqlMapDataAccessor 
    implements DataReportDAO, InitializingBean {
    
    private ValueFactory valueFactory;
    
    public List<Pair<Value, Integer>> executePropertyFrequencyValueQuery(PropertyValueFrequencyQuery query) 
        throws DataReportException {
        
        PropertyTypeDefinition def = query.getPropertyTypeDefintion();

        PropertyValueFrequencyQueryParameter params
            = new PropertyValueFrequencyQueryParameter(def.getName(), 
                                                    def.getNamespace().getUri());
        
        if (query.getLimit() != PropertyValueFrequencyQuery.LIMIT_UNLIMITED) {
            params.setLimit(query.getLimit());
        }
        
        params.setMinValueFrequency(query.getMinValueFrequency());
        
        params.setOrdering(query.getOrdering());

        Path uri = null;
        if (query.getScoping().size() > 0) {
            for (ReportScope scope: query.getScoping()) {
                if (scope instanceof UriPrefixScope) {
                    List<Path> uris = ((UriPrefixScope)scope).getUriPrefixes();
                    if (uris.isEmpty()) {
                        return new ArrayList<Pair<Value,Integer>>(0); // This scoping yields zero results
                    } else if (uris.size() == 1) {
                        uri = uris.get(0);
                    } else {
                        throw new UnsupportedOperationException("This data report dao does not support URI prefix scope with more than one URI");
                    }
                } else {
                    throw new UnsupportedOperationException("Unsupported report scope type:" + scope.getClass());
                }
            }
        }
        
        if (uri != null && !uri.isRoot()) {
            params.setUriWildcard(
                    SqlDaoUtils.getUriSqlWildcard(uri, AbstractSqlMapDataAccessor.SQL_ESCAPE_CHAR));
        }
        
        String sqlMap = getSqlMap("dataReportPropValueFrequency");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result 
            = getSqlMapClientTemplate().queryForList(sqlMap, params);
        
        List<Pair<Value, Integer>> retval = 
                                    new ArrayList<Pair<Value, Integer>>();
        
        try {
            for (Map<String, Object> row: result) {
                String stringValue = (String)row.get("value");
                Integer frequency = (Integer)row.get("frequency");
                Value value = this.valueFactory.createValue(stringValue, def.getType());
                retval.add(new Pair<Value, Integer>(value, frequency));
            }
        } catch (ValueFormatException vfe) {
            throw new DataReportException("DAO: Unable to map property value from database");
        }
        
        return retval;
    }

    @Required
    public void setValueFactory(ValueFactory valueFactory) {
        this.valueFactory = valueFactory;
    }

}
