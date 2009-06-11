package org.vortikal.web.service.provider;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.support.RequestContext;
import org.vortikal.repository.Path;
import org.vortikal.repository.Resource;
import org.vortikal.web.display.collection.AbstractCollectionListingController;

public class CollectionListingViewServiceNameProvider implements ServiceNameProvider {

    public String getLocalizedName(Resource resource, HttpServletRequest request) {

        String page = request.getParameter(AbstractCollectionListingController.USER_DISPLAY_PAGE);
        if (StringUtils.isBlank(page)) {
            return null;
        }
        try {
            int p = Integer.parseInt(page);
            if (p < 2) {
                return null;
            }
        } catch (NumberFormatException nfe) {
            return null;
        }

        Path uri = resource.getURI();
        RequestContext rc = new RequestContext(request);

        if (Path.ROOT.equals(uri)) {
            return rc.getMessage("viewCollectionListing.serviceTitle.root", new Object[] { page });
        }

        return rc.getMessage("viewCollectionListing.serviceTitle", new Object[] { resource.getTitle(), page });
    }

}
