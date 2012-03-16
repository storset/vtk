package org.vortikal.web.display.collection;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.repository.Repository;
import org.vortikal.repository.RepositoryAction;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.URL;

public class MessageListingController extends CollectionListingController {

    public void runSearch(HttpServletRequest request, Resource collection, Map<String, Object> model, int pageLimit)
            throws Exception {

        super.runSearch(request, collection, model, pageLimit);

        RequestContext requestContext = RequestContext.getRequestContext();
        Repository repository = requestContext.getRepository();
        String token = requestContext.getSecurityToken();
        Principal principal = requestContext.getPrincipal();

        model.put("editCurrentResource", repository.isAuthorized(
                repository.retrieve(token, URL.create(request).getPath(), false), RepositoryAction.READ_WRITE,
                principal, false));

    }
}
