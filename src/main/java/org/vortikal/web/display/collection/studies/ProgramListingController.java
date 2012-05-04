package org.vortikal.web.display.collection.studies;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.repository.Property;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.web.display.collection.CollectionListingController;
import org.vortikal.web.search.Listing;
import org.vortikal.web.search.SearchComponent;

public class ProgramListingController extends CollectionListingController {

    private Map<String, List<SearchComponent>> searchComponentsMap;
    private PropertyTypeDefinition facultyPropDef;
    private PropertyTypeDefinition sortBy;

    @Override
    public void runSearch(HttpServletRequest request, Resource collection, Map<String, Object> model, int pageLimit)
            throws Exception {

        Property facultyProp = collection.getProperty(facultyPropDef);
        Property sortProp = collection.getProperty(sortBy);
        
        boolean faculty = (facultyProp != null) ? true : false;
        String sort =     (sortProp != null) ? sortProp.getStringValue() : "default";

        List<SearchComponent> searchComponents;
        
        if("default".equals(sort)) {
        	if (!faculty) {
                searchComponents = searchComponentsMap.get("default");
            } else {
                searchComponents = searchComponentsMap.get("faculty");
            }
        } else {
            if (!faculty) {
                searchComponents = searchComponentsMap.get(sort);
            } else  {
                searchComponents = searchComponentsMap.get(sort + "Faculty");
            }
        }
        
        model.put("sort", sort);

        List<Listing> results = new ArrayList<Listing>();
        if(searchComponents != null) {
            for (SearchComponent component : searchComponents) {
                Listing listing = component.execute(request, collection, 1, 500, 0);
                if (listing.getFiles().size() > 0) { // Add the listing to the results
                    results.add(listing);
                }
            }
        }

        model.put(MODEL_KEY_SEARCH_COMPONENTS, results);

    }

    public Map<String, List<SearchComponent>> getSearchComponentsMap() {
        return searchComponentsMap;
    }

    public void setSearchComponentsMap(Map<String, List<SearchComponent>> searchComponentsMap) {
        this.searchComponentsMap = searchComponentsMap;
    }

    public void setFacultyPropDef(PropertyTypeDefinition facultyPropDef) {
        this.facultyPropDef = facultyPropDef;
    }

    public void setSortBy(PropertyTypeDefinition sortBy) {
        this.sortBy = sortBy;
    }

}
