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

    private Service viewAllProjects;
    private Repository repository;

    @SuppressWarnings("unchecked")
    @Override
    public void referenceData(Map model, HttpServletRequest request) throws Exception {

        String token = SecurityContext.getSecurityContext().getToken();
        Path resourceURI = RequestContext.getRequestContext().getResourceURI();

        Resource resource = this.getRepository().retrieve(token, resourceURI, true);

        String link = getViewAllProjects().constructLink(resource.getURI());
        boolean isViewAllService = true;
        List<Assertion> serviceAssertions = getViewAllProjects().getAssertions();
        for (Assertion assertion : serviceAssertions) {
            if (!assertion.matches(request, resource, SecurityContext.getSecurityContext().getPrincipal())) {
                isViewAllService = false;
                break;
            }
        }

        if (!isViewAllService) {
            model.put("viewAllProjectsLink", link);
        } else {
            model.put("viewOngoingProjectsLink", resource.getURI().toString());
        }
    }

    public void setViewAllProjects(Service viewAllProjects) {
        this.viewAllProjects = viewAllProjects;
    }

    public Service getViewAllProjects() {
        return viewAllProjects;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public Repository getRepository() {
        return repository;
    }

}
