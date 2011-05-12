package org.vortikal.web.display.media;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.web.RequestContext;

public class DisplayMediaController implements Controller {

    private String viewName;
    private PropertyTypeDefinition descriptionPropDef;
    private MediaPlayer mediaPlayer;

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Map<Object, Object> model = new HashMap<Object, Object>();

        RequestContext requestContext = RequestContext.getRequestContext();
        String token = requestContext.getSecurityToken();
        Path uri = requestContext.getResourceURI();
        Repository repository = requestContext.getRepository();
        Resource resource = repository.retrieve(token, uri, true);

        mediaPlayer.addMediaPlayer(model, uri.toString());

        Property descriptionProp = resource.getProperty(descriptionPropDef);
        
        if (descriptionProp != null)
            model.put("description", descriptionProp.getStringValue());

        return new ModelAndView(this.viewName, model);
    }

    @Required
    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    @Required
    public void setDescriptionPropDef(PropertyTypeDefinition descriptionPropDef) {
        this.descriptionPropDef = descriptionPropDef;
    }

    @Required
    public void setMediaPlayer(MediaPlayer mediaPlayer) {
        this.mediaPlayer = mediaPlayer;
    }

}
