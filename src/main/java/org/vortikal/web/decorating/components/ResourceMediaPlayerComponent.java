package org.vortikal.web.decorating.components;

import java.util.Map;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.web.RequestContext;
import org.vortikal.web.decorating.DecoratorRequest;
import org.vortikal.web.decorating.DecoratorResponse;
import org.vortikal.web.display.media.MediaPlayer;

public class ResourceMediaPlayerComponent extends ViewRenderingDecoratorComponent {

    protected MediaPlayer mediaPlayer;

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

        if (mediaProperty == null) {
            return;
        }

        String resourceReferance = mediaProperty.getStringValue();
        mediaPlayer.addMediaPlayer(model, resourceReferance);
    }

    @Required
    public void setMediaPlayer(MediaPlayer mediaPlayer) {
        this.mediaPlayer = mediaPlayer;
    }
}
