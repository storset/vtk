package org.vortikal.web.display.collection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Comment;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;
import org.vortikal.web.tags.RepositoryTagElementsDataProvider;
import org.vortikal.web.tags.TagElement;

public class BlogListingController extends CollectionListingController {

    private RepositoryTagElementsDataProvider tagElementsProvider;
    private Service viewService;
    private Service commentingService;

    protected void runSearch(HttpServletRequest request, Resource collection, Map<String, Object> model, int pageLimit)
            throws Exception {
        super.runSearch(request, collection, model, pageLimit);

        String token = SecurityContext.getSecurityContext().getToken();
        List<TagElement> tagElements = tagElementsProvider.getTagElements(collection.getURI(), token, 1, 5, 20, 1);
        model.put("tagElements", tagElements);
        List<Comment> comments = repository.getComments(token, collection, true, 4);

        Map<String, Resource> resourceMap = new HashMap<String, Resource>();
        Map<String, URL> urlMap = new HashMap<String, URL>();

        for (Comment comment : comments) {
            Resource r = this.repository.retrieve(token, comment.getURI(), true);
            resourceMap.put(r.getURI().toString(), r);
            urlMap.put(r.getURI().toString(), this.getViewService().constructURL(r.getURI()));
        }

        model.put("comments", comments);
        model.put("urlMap", urlMap);
        model.put("resourceMap", resourceMap);
        model.put("moreCommentsUrl", commentingService.constructLink(collection.getURI()));
    }

    @Required
    public void setTagElementsProvider(RepositoryTagElementsDataProvider tagElementsProvider) {
        this.tagElementsProvider = tagElementsProvider;
    }

    @Required
    public RepositoryTagElementsDataProvider getTagElementsProvider() {
        return tagElementsProvider;
    }

    public void setViewService(Service viewService) {
        this.viewService = viewService;
    }

    public Service getViewService() {
        return viewService;
    }

    public void setCommentingService(Service commentingService) {
        this.commentingService = commentingService;
    }

    public Service getCommentingService() {
        return commentingService;
    }
}
