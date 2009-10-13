package org.vortikal.web.display.collection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.repository.Resource;
import org.vortikal.web.search.Listing;
import org.vortikal.web.search.SearchComponent;
import org.vortikal.web.service.URL;

public class PersonListingController extends
        AbstractCollectionListingController {

    private SearchComponent searcher;

    protected void runSearch(HttpServletRequest request, Resource collection,
            Map<String, Object> model, int pageLimit) throws Exception {
        List<Listing> results = new ArrayList<Listing>();
        int page = getPage(request, UPCOMING_PAGE_PARAM);
  
        URL nextURL = null;
        URL prevURL = null;
        
        Listing persons = this.searcher.execute(request, collection, page, pageLimit, 0);
        
        if(persons.size() > 0){
            results.add(persons);
        }
        
        if (persons.size() > 0 && page > 1) {
            prevURL = createURL(request, PREVIOUS_PAGE_PARAM, PREV_BASE_OFFSET_PARAM);
            prevURL.setParameter(UPCOMING_PAGE_PARAM, String.valueOf(page - 1));
        }

        if (persons.hasMoreResults()) {
            nextURL = createURL(request, PREVIOUS_PAGE_PARAM, PREV_BASE_OFFSET_PARAM);
            nextURL.setParameter(UPCOMING_PAGE_PARAM, String.valueOf(page + 1));
        } else if (persons.size() == pageLimit) {
            nextURL = URL.create(request);
            nextURL.setParameter(PREVIOUS_PAGE_PARAM, String.valueOf(page));
        }
        
        cleanURL(nextURL);
        cleanURL(prevURL);
        
        if (nextURL != null) {
            nextURL.setParameter(USER_DISPLAY_PAGE, String.valueOf(page + 1));
        }
        if (prevURL != null && page > 2) {
            prevURL.setParameter(USER_DISPLAY_PAGE, String.valueOf(page -1));
        }

        model.put("nextURL", nextURL);
        model.put("prevURL", prevURL);
        model.put("searchComponents", results);
    }
    public void setSearcher(SearchComponent searcher) {
        this.searcher = searcher;
    }
    public SearchComponent getSearcher() {
        return searcher;
    }



}
