package org.vortikal.web.decorating.components;

import java.util.Map;

import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceNotFoundException;
import org.vortikal.web.RequestContext;
import org.vortikal.web.decorating.DecoratorRequest;
import org.vortikal.web.decorating.DecoratorResponse;
import org.vortikal.web.service.Service;

public class PersonPressPhotoLinkComponent extends ViewRenderingDecoratorComponent {

    private static final String PRESS_PHOTO_PROROPERTY_NAME = "pressPhoto";

    private Service viewAsWebpage;

    protected void processModel(Map<Object, Object> model, DecoratorRequest request, DecoratorResponse response)
            throws Exception {

        RequestContext requestContext = RequestContext.getRequestContext();
        String token = requestContext.getSecurityToken();
        Path uri = requestContext.getResourceURI();
        Repository repository = requestContext.getRepository();
        Resource currentDocument = repository.retrieve(token, uri, true);

        Property picture = currentDocument.getProperty(Namespace.STRUCTURED_RESOURCE_NAMESPACE,
                PRESS_PHOTO_PROROPERTY_NAME);

        Path imageUri = null;
        Resource pictureResource = null;
        try {
            imageUri = Path.fromString(picture.getStringValue());
            pictureResource = repository.retrieve(token, imageUri, true);
        } catch (Exception e) {
            model.put(PRESS_PHOTO_PROROPERTY_NAME, picture.getStringValue());
            return;
        }

        if (pictureResource != null && "image".equals(pictureResource.getResourceType())) {
            model.put(PRESS_PHOTO_PROROPERTY_NAME, getViewAsWebpage().constructLink(imageUri));
        } else {
            model.put(PRESS_PHOTO_PROROPERTY_NAME, picture.getStringValue());
        }

    }

    public void setViewAsWebpage(Service viewAsWebpage) {
        this.viewAsWebpage = viewAsWebpage;
    }

    public Service getViewAsWebpage() {
        return viewAsWebpage;
    }
}
