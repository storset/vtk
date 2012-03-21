package org.vortikal.web.display.collection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.repository.Path;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Repository;
import org.vortikal.repository.RepositoryAction;
import org.vortikal.repository.Resource;
import org.vortikal.resourcemanagement.edit.SimpleStructuredEditor;
import org.vortikal.security.Principal;
import org.vortikal.web.RequestContext;
import org.vortikal.web.search.Listing;
import org.vortikal.web.service.URL;

public class MessageListingController extends CollectionListingController {

    @SuppressWarnings("unchecked")
    public void runSearch(HttpServletRequest request, Resource collection, Map<String, Object> model, int pageLimit)
            throws Exception {
        super.runSearch(request, collection, model, pageLimit);

        RequestContext requestContext = RequestContext.getRequestContext();
        Repository repository = requestContext.getRepository();
        String token = requestContext.getSecurityToken();
        Principal principal = requestContext.getPrincipal();

        Path uriParameter = request.getParameter("uri") != null ? Path.fromString(request.getParameter("uri")) : null;
        String actionParameter = request.getParameter("action");

        /*
         * Manipulate the result set to compensate for slow index
         */
        if (uriParameter != null && actionParameter != null) {
            List<Listing> results;
            results = (List<Listing>) model.get(MODEL_KEY_SEARCH_COMPONENTS);
            if (results.isEmpty()) {
                results.add(new Listing(resourceManager.createResourceWrapper(collection), null,
                        "messageListing.defaultListing", 0));
            }
            Listing l = results.get(0);

            if (SimpleStructuredEditor.ACTION_PARAMETER_VALUE_NEW.equals(actionParameter)
                    && repository.exists(token, uriParameter)) {
                l.addFile(repository.retrieve(token, uriParameter, true));
            } else if (SimpleStructuredEditor.ACTION_PARAMETER_VALUE_UPDATE.equals(actionParameter)
                    && repository.exists(token, uriParameter)) {
                l.updateFile(repository.retrieve(token, uriParameter, true));
            } else if (SimpleStructuredEditor.ACTION_PARAMETER_VALUE_DELETE.equals(actionParameter)) {
                List<Path> remove = new ArrayList<Path>();
                for (PropertySet p : l.getFiles()) {
                    if (!repository.exists(token, p.getURI())) {
                        remove.add(p.getURI());
                    }
                }
                for (Path p : remove) {
                    l.removeFile(p);
                }
            }
            model.put("edit", helper.isAuthorized(repository, token, principal, l.getTotalHits(), results));
        }

        model.put("editCurrentResource", repository.isAuthorized(
                repository.retrieve(token, URL.create(request).getPath(), false), RepositoryAction.READ_WRITE,
                principal, false));
    }
}
