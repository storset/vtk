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

        List<SearchComponent> searchComponents = searchComponentsMap.get("default");
        if (facultyProp != null) {
            searchComponents = searchComponentsMap.get("faculty");
        }
        if (sortProp != null && facultyProp == null) {
            searchComponents = searchComponentsMap.get("alphabetical");
        } else if (sortProp != null && facultyProp != null) {
            searchComponents = searchComponentsMap.get("alphabeticalFaculty");
        }
        
        model.put("sort", sortProp != null ? sortProp.getStringValue() : "default");

        List<Listing> results = new ArrayList<Listing>();
        for (SearchComponent component : searchComponents) {

            Listing listing = component.execute(request, collection, 1, 500, 0);
            // Add the listing to the results
            if (listing.getFiles().size() > 0) {
                results.add(listing);
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
