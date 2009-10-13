package org.vortikal.web.display.collection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.repository.Resource;
import org.vortikal.web.display.article.ArticleListingSearcher;
import org.vortikal.web.search.Listing;
import org.vortikal.web.search.SearchComponent;

public class PersonListingController extends
        AbstractCollectionListingController {

    private SearchComponent searcher;

    protected void runSearch(HttpServletRequest request, Resource collection,
            Map<String, Object> model, int pageLimit) throws Exception {
        List<Listing> results = new ArrayList<Listing>();
        
        Listing persons = this.searcher.execute(request, collection, 1, pageLimit, 0);
        
        if(persons.size() > 0){
            results.add(persons);
        }
        
        model.put("searchComponents", results);
    }
    public void setSearcher(SearchComponent searcher) {
        this.searcher = searcher;
    }
    public SearchComponent getSearcher() {
        return searcher;
    }



}
