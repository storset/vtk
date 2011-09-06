package org.vortikal.web.commenting;

import java.util.Map;

import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.RepositoryAction;
import org.vortikal.repository.Resource;
import org.vortikal.web.RequestContext;
import org.vortikal.web.RequestContext.RepositoryTraversal;
import org.vortikal.web.RequestContext.TraversalCallback;
import org.vortikal.web.decorating.DecoratorRequest;
import org.vortikal.web.decorating.DecoratorResponse;
import org.vortikal.web.decorating.components.ViewRenderingDecoratorComponent;

public class CommentsFeedUrlProvider extends ViewRenderingDecoratorComponent {

    protected void processModel(final Map<Object, Object> model, DecoratorRequest request, DecoratorResponse response)
            throws Exception {
        super.processModel(model, request, response);
        RequestContext requestContext = RequestContext.getRequestContext();
        Repository repository = requestContext.getRepository();
        String token = requestContext.getSecurityToken();
        Path uri = RequestContext.getRequestContext().getResourceURI();
        Resource resource = repository.retrieve(token, uri, true);
        boolean commentsAllowed = false;
        if (resource != null) {
            commentsAllowed = repository.isAuthorized(resource, RepositoryAction.ADD_COMMENT, 
                    requestContext.getPrincipal(), false);
        }
        model.put("commentsAllowed", commentsAllowed);
        
        model.put("commentsEnabled", false);
        RepositoryTraversal traversal = requestContext.rootTraversal(token, uri);
        traversal.traverse(new TraversalCallback() {
            @Override
            public boolean callback(Resource resource) {
                for (Property p: resource.getProperties()) {
                    if (p.getDefinition().getName().equals("commentsEnabled")) {
                        model.put("commentsEnabled", p.getBooleanValue());
                        return false;
                    }
                }
                return true;
            }
            @Override
            public boolean error(Path uri, Throwable error) {
                return false;
            }
        });
    }

}
