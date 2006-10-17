package org.vortikal.web.controller;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.Property;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;

public class LastModifiedEvaluatorImpl implements LastModifiedEvaluator {
    
    private static Log logger = LogFactory.getLog(LastModifiedEvaluatorImpl.class);
    private List lookupList;

    private boolean handleLastModifiedForValuesInList;

    private PropertyTypeDefinition propertyDefinition;

    /**
     * @param lookupList
     *            A list of the values we want (or do not want) to handle lastModified for. The
     *            values in the list are compared with the value for the selected property, see
     *            {@link setPropertyNamespace} and {@link #setPropertyName}.
     */
    public void setLookupList(List lookupList) {
        this.lookupList = lookupList;
    }

    /**
     * @param handleLastModifiedForValuesInList
     *            If true, report last-modified if value found in lookupList. If false, report
     *            last-modified if value not ofund in list
     */
    public void setHandleLastModifiedForValuesInList(boolean handleLastModifiedForValuesInList) {
        this.handleLastModifiedForValuesInList = handleLastModifiedForValuesInList;
    }

    /**
     * 
     * @param propertyDefinition
     *            The property type definition we want to look for
     */
    public void setPropertyDefinition(PropertyTypeDefinition propertyDefinition) {
        this.propertyDefinition = propertyDefinition;
    }

    public boolean reportLastModified(Resource resource) throws IllegalArgumentException {
        if (resource == null) {
            throw new IllegalArgumentException("resource can't be null");
        }
        if (lookupList == null || lookupList.size() == 0) {
            return !handleLastModifiedForValuesInList;
        }

        Property prop = resource.getProperty(this.propertyDefinition);
        if (prop == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Can't find property '" + this.propertyDefinition
                             + "' for resource with uri " + resource.getURI());
            }
            // Since we havn't found the property, we know it is not in the list of accepted
            // values, and we should behave as it is not found in the list
            return !handleLastModifiedForValuesInList;
        }
        String schema = prop.getStringValue();
        Iterator schemaIterator = lookupList.iterator();
        boolean found = false;
        while (schemaIterator.hasNext()) {
            String schemaFromList = (String) schemaIterator.next();
            if (schemaFromList.equals(schema)) {
                found = true;
                break;
            }
        }
        return found == handleLastModifiedForValuesInList;
    }

}
