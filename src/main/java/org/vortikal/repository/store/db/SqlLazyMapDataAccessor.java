package org.vortikal.repository.store.db;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceImpl;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.store.LazyDataAccessor;

public class SqlLazyMapDataAccessor extends AbstractSqlMapDataAccessor implements LazyDataAccessor {
	
    public Property loadBinaryContent(Resource resource) {    	
    	Property binaryProperty = resource.getProperty(Namespace.DEFAULT_NAMESPACE, PropertyType.BINARY_REF);
    	
        if (binaryProperty != null && StringUtils.isNotBlank(binaryProperty.getStringValue())) {
        	Map<String, Object> params = new HashMap<String, Object>();
        	params.put("resourceId", ((ResourceImpl) resource).getID());
        	params.put("binaryPropRef", binaryProperty.getStringValue());
            String sqlMap = getSqlMap("selectBinaryPropertyEntry");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> propertyList = getSqlMapClientTemplate().queryForList(sqlMap, params);
            
            Map<String, Object> binaryContent = propertyList.get(0);
            
            byte[] content = (byte[]) binaryContent.get("binaryContent");
            String mimetype = (String) binaryContent.get("binaryMimetype");
            
            return resource.createBinaryProperty(Namespace.DEFAULT_NAMESPACE, PropertyType.BINARY_CONTENT, content, mimetype);
        }
        
        return null;
	}

}
