package org.vortikal.web.controller;

import java.util.List;

import org.vortikal.repository.Resource;

public interface LastModifiedEvaluator {

    public void setLookupList(List lookupList);

    public void setHandleLastModifiedForValuesInList(boolean handleLastModifiedForValuesInList);

    public void setPropertyName(String propertyName);

    public void setPropertyNamespace(String propertyNamespace);

    public boolean reportLastModified(Resource resource);

}
