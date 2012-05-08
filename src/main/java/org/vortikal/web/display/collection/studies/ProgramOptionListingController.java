package org.vortikal.web.display.collection.studies;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.repository.Resource;
import org.vortikal.web.display.collection.CollectionListingController;
import org.vortikal.web.search.Listing;
import org.vortikal.web.search.SearchComponent;

public class ProgramOptionListingController extends CollectionListingController {

    @Override
    public void runSearch(HttpServletRequest request, Resource collection, Map<String, Object> model, int pageLimit)
            throws Exception {

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
    
}
