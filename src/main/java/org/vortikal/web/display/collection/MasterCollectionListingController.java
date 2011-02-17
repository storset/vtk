package org.vortikal.web.display.collection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.decorating.components.ListRelatedPersonsComponent;
import org.vortikal.web.decorating.components.ListRelatedPersonsComponent.RelatedPerson;
import org.vortikal.web.display.listing.ListingPager;
import org.vortikal.web.search.Listing;
import org.vortikal.web.search.SearchComponent;

public class MasterCollectionListingController extends AlphabeticalCollectionListingController {

    private ListRelatedPersonsComponent p;

    protected int defaultPageLimit = 100;

    @Override
    public void runSearch(HttpServletRequest request, Resource collection, Map<String, Object> model, int pageLimit)
            throws Exception {
        Property type = collection.getProperty(getDisplayTypePropDef());

        super.runSearch(request, collection, model, defaultPageLimit);

        if (!(type != null && "alphabetical".equals(type.getStringValue()))) {

            if ("completed".equals(request.getParameter("view"))) {
                super.getAlphabeticalOrdredProjects(request, collection, model, pageLimit);
                model.put("overrideListingType", "alphabetical");
            }

            addPersonsRelatedToMaster(request, collection, model, defaultPageLimit);
        }
    }

    public void addPersonsRelatedToMaster(HttpServletRequest request, Resource collection, Map<String, Object> model,
            int pageLimit) throws Exception {
        int page = ListingPager.getPage(request, ListingPager.UPCOMING_PAGE_PARAM);
        int limit = pageLimit;
        String token = SecurityContext.getSecurityContext().getToken();

        Map<String, List<RelatedPerson>> personsRelatedToMaster = new HashMap<String, List<RelatedPerson>>();

        for (SearchComponent component : this.searchComponents) {
            Listing listing = component.execute(request, collection, page, limit, 0);

            List<PropertySet> files = listing.getFiles();

            for (PropertySet file : files) {
                Resource currentResource = repository.retrieve(token, file.getURI(), true);
                List<RelatedPerson> persons = getP().getRelatedPersons(request, token, currentResource, limit);
                personsRelatedToMaster.put(file.toString(), persons);
            }

        }

        model.put("personsRelatedToMaster", personsRelatedToMaster);
    }

    public void setP(ListRelatedPersonsComponent p) {
        this.p = p;
    }

    public ListRelatedPersonsComponent getP() {
        return p;
    }
}
