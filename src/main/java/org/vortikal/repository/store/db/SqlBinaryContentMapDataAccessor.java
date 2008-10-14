package org.vortikal.repository.store.db;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.vortikal.repository.store.BinaryContentDataAccessor;

public class SqlBinaryContentMapDataAccessor extends AbstractSqlMapDataAccessor implements BinaryContentDataAccessor {
	
	private static final Logger log = Logger.getLogger(SqlBinaryContentMapDataAccessor.class);
    
    public InputStream getBinaryStream(String binaryRef) {
    	
    	try {
    		String sqlMap = getSqlMap("selectBinaryPropertyEntry");
    		@SuppressWarnings("unchecked")
    		List<Map<String, Object>> resultList = getSqlMapClientTemplate().queryForList(sqlMap, binaryRef);
    		return (InputStream) resultList.get(0).get("binaryStream");
		} catch (Exception e) {
			log.error("An error occured while getting the binary stream for " + binaryRef, e);
		}
		
		return null;
        
    }

}
