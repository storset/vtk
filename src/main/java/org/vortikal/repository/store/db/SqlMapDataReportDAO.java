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
import org.vortikal.repository.reporting.DataReportException;
import org.vortikal.repository.reporting.Pair;
import org.vortikal.repository.reporting.PropertyValueFrequencyQuery;
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
    
    public List<Pair<Value, Integer>> executePropertyFrequencyValueQuery(
                                            String token,
                                            PropertyValueFrequencyQuery query) 
        throws DataReportException {
        
        PropertyTypeDefinition def = query.getPropertyTypeDefintion();

        PropertyValueFrequencyQueryParameter params
            = new PropertyValueFrequencyQueryParameter(def.getName(), 
                                                    def.getNamespace().getUri());
        
        if (query.getLimit() != PropertyValueFrequencyQuery.LIMIT_UNLIMITED) {
            params.setLimit(query.getLimit());
        }
        
        params.setOrdering(query.getOrdering());
        
        if (query.getUriScope() != null && !"/".equals(query.getUriScope().getUri())) {
            params.setUriWildcard(
                    SqlDaoUtils.getUriSqlWildcard(query.getUriScope().getUri(), 
                               AbstractSqlMapDataAccessor.SQL_ESCAPE_CHAR));
        }
        
        String sqlMap = getSqlMap("dataReportPropValueFrequency");
        
        List<Map<String, Object>> result 
            = getSqlMapClientTemplate().queryForList(sqlMap, params);
        
        List<Pair<Value, Integer>> retval = 
                                    new ArrayList<Pair<Value, Integer>>();
        
        try {
            for (Map row: result) {
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
