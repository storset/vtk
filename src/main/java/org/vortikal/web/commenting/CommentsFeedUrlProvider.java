package org.vortikal.web.commenting;

import java.util.Map;

import org.vortikal.repository.Path;
import org.vortikal.repository.Privilege;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.decorating.DecoratorRequest;
import org.vortikal.web.decorating.DecoratorResponse;
import org.vortikal.web.decorating.components.ViewRenderingDecoratorComponent;

public class CommentsFeedUrlProvider extends ViewRenderingDecoratorComponent {

    protected void processModel(Map<Object, Object> model, DecoratorRequest request, DecoratorResponse response)
            throws Exception {
        super.processModel(model, request, response);
        String token = SecurityContext.getSecurityContext().getToken();
        Path uri = RequestContext.getRequestContext().getResourceURI();
        Resource resource = repository.retrieve(token, uri, true);
        boolean commentsEnabled = false;
        if(resource != null)
            commentsEnabled = resource.getAcl().getActions().contains(Privilege.ADD_COMMENT);
        model.put("commentsEnabled", commentsEnabled);
    }

}
