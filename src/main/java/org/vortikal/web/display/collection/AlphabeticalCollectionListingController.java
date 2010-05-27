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
import org.vortikal.web.display.listing.ListingPager;
import org.vortikal.web.search.Listing;
import org.vortikal.web.search.SearchComponent;
import org.vortikal.web.service.URL;

public class AlphabeticalCollectionListingController extends CollectionListingController {

    private PropertyTypeDefinition titlePropDef;
    private PropertyTypeDefinition displayTypePropDef;

    @Override
    public void runSearch(HttpServletRequest request, Resource collection, Map<String, Object> model, int pageLimit)
            throws Exception {
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
    private void getAlphabeticalOrdredProjects(HttpServletRequest request, Resource collection,
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

            char firstCharInTitle[] = new char[1];
            for (int i = 0; i < files.size(); i++) {
                PropertySet file = files.get(i);
                Property title = file.getProperty(this.titlePropDef);
                if (i == 0) {
                    firstCharInTitle[0] = title.getStringValue().charAt(0);
                }
                if (firstCharInTitle[0] != title.getStringValue().charAt(0)) {
                    String key = new String(firstCharInTitle).toUpperCase();
                    if (alpthabeticalOrdredResult.get(key) != null) {
                        alpthabeticalOrdredResult.get(key).addAll(tmpFiles);
                    } else {
                        alpthabeticalOrdredResult.put(key, tmpFiles);
                    }
                    firstCharInTitle[0] = title.getStringValue().charAt(0);
                    tmpFiles = new ArrayList<PropertySet>();
                }
                tmpFiles.add(file);
            }
            if (tmpFiles.size() > 0) {
                String key = new String(firstCharInTitle).toUpperCase();
                if (alpthabeticalOrdredResult.get(key) != null) {
                    alpthabeticalOrdredResult.get(key).addAll(tmpFiles);
                } else {
                    alpthabeticalOrdredResult.put(key, tmpFiles);
                }
            }
        }

        model.put("alpthabeticalOrdredResult", alpthabeticalOrdredResult);
        List<URL> urls = ListingPager.generatePageThroughUrls(totalHits, pageLimit, URL.create(request));
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

}
