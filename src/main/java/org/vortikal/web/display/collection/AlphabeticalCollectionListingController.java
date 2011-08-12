package org.vortikal.web.display.collection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.web.RequestContext;
import org.vortikal.web.display.listing.ListingPager;
import org.vortikal.web.display.listing.ListingPagingLink;
import org.vortikal.web.search.Listing;
import org.vortikal.web.search.SearchComponent;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

public class AlphabeticalCollectionListingController extends CollectionListingController {

    private PropertyTypeDefinition titlePropDef;
    private PropertyTypeDefinition displayTypePropDef;
    private SearchComponent alternateSearchComponent;

    @Override
    public void runSearch(HttpServletRequest request, Resource collection, Map<String, Object> model, int pageLimit)
            throws Exception {

        if (this.alternateSearchComponent != null) {
            Listing listing = this.alternateSearchComponent.execute(request, collection, 1, pageLimit, 0);
            if (listing != null && listing.size() > 0) {
                model.put("displayAlternateLink", "true");
            }
        }

        Property type = collection.getProperty(displayTypePropDef);
        if (type != null && "alphabetical".equals(type.getStringValue())) {
            getAlphabeticalOrdredProjects(request, collection, model, pageLimit);
        } else {
            super.runSearch(request, collection, model, pageLimit);
        }
    }

    /*
     * Putting an alphabetical ordered list on the view model. No need to sort
     * the result as it is already sorted by the search component. The result is
     * a map consisting of a key that is the first character of the title and a
     * value that is a list of files starting with the key char. The key is
     * upper case.
     */
    public void getAlphabeticalOrdredProjects(HttpServletRequest request, Resource collection,
            Map<String, Object> model, int pageLimit) throws Exception {
        int page = ListingPager.getPage(request, ListingPager.UPCOMING_PAGE_PARAM);
        int limit = pageLimit;
        int totalHits = 0;
        Map<String, List<PropertySet>> alpthabeticalOrdredResult = new LinkedHashMap<String, List<PropertySet>>();
        List<Listing> results = new ArrayList<Listing>();

        for (SearchComponent component : this.searchComponents) {
            Listing listing = component.execute(request, collection, page, limit, 0);
            results.add(listing);
            totalHits = listing.getTotalHits();
            List<PropertySet> files = listing.getFiles();
            List<PropertySet> tmpFiles = new ArrayList<PropertySet>();

            // array is convenient for string constructors
            char currentIndexChar[] = new char[1];
            for (int i = 0; i < files.size(); i++) {
                PropertySet file = files.get(i);
                Property title = file.getProperty(this.titlePropDef);
                char firstCharInTitle = title.getStringValue().trim().charAt(0);
                if (i == 0) {
                    currentIndexChar[0] = firstCharInTitle;
                }
                if (currentIndexChar[0] != firstCharInTitle) {
                    String key = new String(currentIndexChar).toUpperCase();
                    if (alpthabeticalOrdredResult.get(key) != null) {
                        alpthabeticalOrdredResult.get(key).addAll(tmpFiles);
                    } else {
                        alpthabeticalOrdredResult.put(key, tmpFiles);
                    }
                    currentIndexChar[0] = firstCharInTitle;
                    tmpFiles = new ArrayList<PropertySet>();
                }
                tmpFiles.add(file);
            }
            if (tmpFiles.size() > 0) {
                String key = new String(currentIndexChar).toUpperCase();
                if (alpthabeticalOrdredResult.get(key) != null) {
                    alpthabeticalOrdredResult.get(key).addAll(tmpFiles);
                } else {
                    alpthabeticalOrdredResult.put(key, tmpFiles);
                }
            }
        }

        Service service = RequestContext.getRequestContext().getService();
        URL baseURL = service.constructURL(RequestContext.getRequestContext().getResourceURI());

        model.put("alpthabeticalOrdredResult", alpthabeticalOrdredResult);
        List<ListingPagingLink> urls = ListingPager.generatePageThroughUrls(totalHits, pageLimit, baseURL, page);
        model.put(MODEL_KEY_PAGE_THROUGH_URLS, urls);
        model.put(MODEL_KEY_PAGE, page);
        model.put(MODEL_KEY_SEARCH_COMPONENTS, results);
    }

    public void setTitlePropDef(PropertyTypeDefinition titlePropDef) {
        this.titlePropDef = titlePropDef;
    }

    public PropertyTypeDefinition getTitlePropDef() {
        return titlePropDef;
    }

    public void setDisplayTypePropDef(PropertyTypeDefinition displayTypePropDef) {
        this.displayTypePropDef = displayTypePropDef;
    }

    public PropertyTypeDefinition getDisplayTypePropDef() {
        return displayTypePropDef;
    }

    public void setAlternateSearchComponent(SearchComponent alternateSearchComponent) {
        this.alternateSearchComponent = alternateSearchComponent;
    }

}
