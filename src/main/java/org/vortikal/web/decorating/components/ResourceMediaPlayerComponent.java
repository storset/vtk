package org.vortikal.web.decorating.components;

import java.util.Map;

import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.web.RequestContext;
import org.vortikal.web.decorating.DecoratorRequest;
import org.vortikal.web.decorating.DecoratorResponse;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

public class ResourceMediaPlayerComponent extends ViewRenderingDecoratorComponent {

    protected Map<String, String> extentionToMimetype;
    protected Service viewService;

    protected void processModel(Map<Object, Object> model, DecoratorRequest request, DecoratorResponse response)
            throws Exception {

        RequestContext requestContext = RequestContext.getRequestContext();
        String token = requestContext.getSecurityToken();
        Path uri = requestContext.getResourceURI();
        Repository repository = requestContext.getRepository();
        Resource currentDocument = repository.retrieve(token, uri, true);

        Property mediaProperty = currentDocument.getProperty(Namespace.DEFAULT_NAMESPACE, "media");
        if (mediaProperty == null) {
            mediaProperty = currentDocument.getProperty(Namespace.STRUCTURED_RESOURCE_NAMESPACE, "media");
        }
        if(mediaProperty == null){
            return;
        }
        String media = mediaProperty.getStringValue();

        Resource mediaResource = null;
        try {
            mediaResource = repository.retrieve(token, Path.fromString(media), false);
        } catch (Exception e) {
        }

        model.put("extension", getExtension(media));
        model.put("autoplay", "false");
        
        if (mediaResource != null) {
            model.put("contentType", mediaResource.getContentType());
        } else {
            model.put("contentType", extentionToMimetype.get(getExtension(media)));
        }

        createLocalUrlToMediaFile(media, model);
    }

    public String getExtension(String url) {
        if (url != null && url.contains(".")) {
            String[] s = url.split("\\.");
            return s[s.length - 1];
        }
        return "";
    }

    public void createLocalUrlToMediaFile(String mediaUri, Map<Object, Object> model) {
        Path uri = null;
        URL localURL = null;
        try {
            uri = Path.fromString(mediaUri);
            localURL = getViewService().constructURL(uri);
        } catch (Exception e) {
        }
        if (localURL != null) {
            model.put("media", localURL);
        } else {
            model.put("media", mediaUri);
        }
    }

    public Map<String, String> getExtentionToMimetype() {
        return extentionToMimetype;
    }

    public void setExtentionToMimetype(Map<String, String> extentionToMimetype) {
        this.extentionToMimetype = extentionToMimetype;
    }

    public void setViewService(Service viewService) {
        this.viewService = viewService;
    }

    public Service getViewService() {
        return viewService;
    }
}
