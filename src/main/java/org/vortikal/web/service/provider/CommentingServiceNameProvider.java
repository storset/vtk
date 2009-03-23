package org.vortikal.web.service.provider;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.support.RequestContext;
import org.vortikal.repository.Path;
import org.vortikal.repository.Resource;

public class CommentingServiceNameProvider implements ServiceNameProvider {

    private String repositoryId;

    public String getLocalizedName(Resource resource, HttpServletRequest request) {

        Path uri = resource.getURI();
        RequestContext rc = new RequestContext(request);

        if (Path.ROOT.equals(uri)) {
            return rc.getMessage("commenting.comments", new Object[] { repositoryId });
        }
        return rc.getMessage("commenting.comments", new Object[] { resource.getTitle() });

    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

}
