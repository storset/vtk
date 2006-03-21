package org.vortikal.web.controller;

import java.util.List;

import org.vortikal.repository.Resource;

public interface LastModifiedEvaluator {

    /**
     * @param lookupList
     *            A list of the values we want (or do not want) to handle lastModified for. The
     *            values in the list are compared with the value for the selected property, see
     *            {@link setPropertyNamespace} and {@link #setPropertyName}.
     */
    public void setLookupList(List lookupList);

    /**
     * @param handleLastModifiedForValuesInList
     *            If true, report last-modified if value found in lookupList. If false, report
     *            last-modified if value not ofund in list
     */
    public void setHandleLastModifiedForValuesInList(boolean handleLastModifiedForValuesInList);

    /**
     * 
     * @param propertyNamespace
     *            Namespace for the property we want to look for
     */
    public void setPropertyNamespace(String propertyNamespace);

    /**
     * 
     * @param propertyName
     *            Name of the property we want to look for
     */
    public void setPropertyName(String propertyName);

    /**
     * 
     * @param resource
     *            The resource we want to test if we should report last-modified for
     * @return true if we should report last-modified, else false
     */
    public boolean reportLastModified(Resource resource);

}
