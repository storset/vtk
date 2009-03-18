package org.vortikal.web.service.provider;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.support.RequestContext;
import org.vortikal.repository.Path;
import org.vortikal.repository.Resource;

public class TagsViewServiceNameProvider implements ServiceNameProvider {
	
	private String repositoryId;

	public String getLocalizedName(Resource resource, HttpServletRequest request) {
		
		Path uri = resource.getURI();
		RequestContext rc = new RequestContext(request);
		
		String tag = request.getParameter("tag");
		boolean noTagSpecified = StringUtils.isBlank(tag);
        if (Path.ROOT.equals(uri)) {
            return rc.getMessage(noTagSpecified ? "tags.serviceTitle" : "tags.title", new Object[] { repositoryId, tag });
        }
        return rc.getMessage(noTagSpecified ? "tags.serviceTitle" : "tags.scopedTitle", new Object[] { resource.getTitle(), tag });
		
	}
	
	public void setRepositoryId(String repositoryId) {
		this.repositoryId = repositoryId;
	}
	

}
