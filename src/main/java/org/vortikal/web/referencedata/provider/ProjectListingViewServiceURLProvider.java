package org.vortikal.web.referencedata.provider;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.referencedata.ReferenceDataProvider;
import org.vortikal.web.service.Assertion;
import org.vortikal.web.service.Service;

public class ProjectListingViewServiceURLProvider implements ReferenceDataProvider {

    private Service service;
    private Repository repository;
    
    private String viewAll;
    private String viewSubset;

    @SuppressWarnings("unchecked")
    @Override
    public void referenceData(Map model, HttpServletRequest request) throws Exception {

        String token = SecurityContext.getSecurityContext().getToken();
        Path resourceURI = RequestContext.getRequestContext().getResourceURI();

        Resource resource = this.getRepository().retrieve(token, resourceURI, true);

        String link = getService().constructLink(resource.getURI());
        boolean isViewAllService = true;
        List<Assertion> serviceAssertions = getService().getAssertions();
        for (Assertion assertion : serviceAssertions) {
            if (!assertion.matches(request, resource, SecurityContext.getSecurityContext().getPrincipal())) {
                isViewAllService = false;
                break;
            }
        }

        if (!isViewAllService) {
            model.put(viewAll, link);
        } else {
            model.put(viewSubset, resource.getURI().toString());
        }
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public Service getService() {
        return service;
    }

    public void setViewAll(String viewAll) {
        this.viewAll = viewAll;
    }

    public String getViewAll() {
        return viewAll;
    }

    public void setViewSubset(String viewSubset) {
        this.viewSubset = viewSubset;
    }

    public String getViewSubset() {
        return viewSubset;
    }

}
