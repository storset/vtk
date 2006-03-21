package org.vortikal.web.controller;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.Property;
import org.vortikal.repository.Resource;

public class LastModifiedEvaluatorImpl implements LastModifiedEvaluator {
    
    private static Log logger = LogFactory.getLog(LastModifiedEvaluatorImpl.class);
    private List lookupList;

    private boolean handleLastModifiedForValuesInList;

    private String propertyName;

    private String propertyNamespace;

    public void setLookupList(List lookupList) {
        this.lookupList = lookupList;
    }

    public void setHandleLastModifiedForValuesInList(boolean handleLastModifiedForValuesInList) {
        this.handleLastModifiedForValuesInList = handleLastModifiedForValuesInList;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public void setPropertyNamespace(String propertyNamespace) {
        this.propertyNamespace = propertyNamespace;
    }

    public boolean reportLastModified(Resource resource) throws IllegalArgumentException {
        if (resource == null) {
            throw new IllegalArgumentException("resource can't be null");
        }
        if (lookupList != null && lookupList.size() > 0) {
            Property schemaProp = resource.getProperty(propertyNamespace, propertyName);
            if (schemaProp == null) {
                logger.debug("Can't find property '" + propertyNamespace + ":" + propertyName
                        + "' for resource with uri " + resource.getURI());
                // Since we havn't found the property, we know it is not in the list of accepted
                // values, and we should behave as it is not found in the list
                return !handleLastModifiedForValuesInList;
            }
            String schema = schemaProp.getValue();
            Iterator schemaIterator = lookupList.iterator();
            boolean found = false;
            while (schemaIterator.hasNext()) {
                String schemaFromList = (String) schemaIterator.next();
                if (schemaFromList.equals(schema)) {
                    found = true;
                    break;
                }
            }
            if ((found && !handleLastModifiedForValuesInList)
                    || (!found && handleLastModifiedForValuesInList)) {
                return false;
            }
        }
        return true;
    }

}
