package org.vortikal.repository;

import java.util.List;

public interface HierarchicalVocabulary {

    public List<String> getResourceTypeDescendantNames(String resourceTypeName);

}