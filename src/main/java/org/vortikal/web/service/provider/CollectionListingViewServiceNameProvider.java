package org.vortikal.web.service.provider;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.support.RequestContext;
import org.vortikal.repository.Path;
import org.vortikal.repository.Resource;

public class CollectionListingViewServiceNameProvider implements ServiceNameProvider {

    public String getLocalizedName(Resource resource, HttpServletRequest request) {

        String pageParam = request.getParameter("page");
        boolean isPreviousPageParam = false;
        if (StringUtils.isBlank(pageParam)) {
            pageParam = request.getParameter("p-page");
            isPreviousPageParam = true;
        }
        
        pageParam = checkPageParameter(pageParam, isPreviousPageParam);
        if (StringUtils.isBlank(pageParam)) {
            return null;
        }
        
        Path uri = resource.getURI();
        RequestContext rc = new RequestContext(request);
        
        if (Path.ROOT.equals(uri)) {
            return rc.getMessage("viewCollectionListing.serviceTitle.root", new Object[] { pageParam });
        }
        
        return rc.getMessage("viewCollectionListing.serviceTitle", new Object[] { resource.getTitle(), pageParam });
    }

    private String checkPageParameter(String pageParam, boolean isPreviousPageParam) {
        try {
            int page = Integer.parseInt(pageParam);
            if (isPreviousPageParam) {
                page++;
            }
            if (page > 1) {
                return String.valueOf(page);
            }
            return null;
        } catch (NumberFormatException nfe) {
            return null;
        }
    }
}
