package org.vortikal.web.decorating.components;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.decorating.DecoratorRequest;
import org.vortikal.web.decorating.DecoratorResponse;
import org.vortikal.web.service.URL;

public class ResourceMediaPlayerComponent extends ViewRenderingDecoratorComponent {

    private Repository repository;
    Map<String, String> extentionToMimetype;

    protected void processModel(Map<Object, Object> model, DecoratorRequest request, DecoratorResponse response)
            throws Exception {

        String token = SecurityContext.getSecurityContext().getToken();
        Path uri = RequestContext.getRequestContext().getResourceURI();

        Resource currentDocument = repository.retrieve(token, uri, false);

        Property mediaProperty = currentDocument.getProperty(Namespace.DEFAULT_NAMESPACE, "media");
        if (mediaProperty == null) {
            mediaProperty = currentDocument.getProperty(Namespace.STRUCTURED_RESOURCE_NAMESPACE, "media");
        }
        String media = mediaProperty.getStringValue();

        Resource mediaResource = null;
        try {
            mediaResource = repository.retrieve(token, Path.fromString(media), false);
        } catch (Exception e) {
        }

        model.put("extension", getExtension(media));

        if (mediaResource != null) {
            model.put("contentType", mediaResource.getContentType());
        } else {
            model.put("contentType", extentionToMimetype.get(getExtension(media)));
        }

        createLocalUrlToMediaFile(request.getServletRequest(), media, model);
    }

    public String getExtension(String url) {
        if (url != null && url.contains(".")) {
            String[] s = url.split("\\.");
            return s[s.length - 1];
        }
        return "";
    }

    public void createLocalUrlToMediaFile(HttpServletRequest request, String mediaUri, Map<Object, Object> model) {
        Path uri = null;
        try {
            uri = Path.fromString(mediaUri);
        } catch (Exception e) {
        }
        URL localURL = null;
        String token = SecurityContext.getSecurityContext().getToken();
        String name = request.getServerName();
        String protocol = null;
        if (request.getProtocol().contains("HTTPS")) {
            protocol = "https";
        } else {
            protocol = "http";
        }
        Resource mediaResource = null;
        try {
            mediaResource = repository.retrieve(token, uri, false);
        } catch (Exception e) {
        }
        if (mediaResource != null) {
            localURL = new URL(protocol, name, uri);
            localURL.setPort(request.getLocalPort());
        }
        if (localURL != null) {
            model.put("media", localURL);
        } else {
            model.put("media", mediaUri);
        }
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public Map<String, String> getExtentionToMimetype() {
        return extentionToMimetype;
    }

    public void setExtentionToMimetype(Map<String, String> extentionToMimetype) {
        this.extentionToMimetype = extentionToMimetype;
    }
}
