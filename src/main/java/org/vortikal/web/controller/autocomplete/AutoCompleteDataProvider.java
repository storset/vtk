package org.vortikal.web.controller.autocomplete;

import java.util.List;
import java.util.Map;

public interface AutoCompleteDataProvider {
    
    public Map<String, List<Object>> getData(String resultSetRoot, String searchQuery);

}
