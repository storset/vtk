package org.vortikal.web.display.media;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.web.RequestContext;
import org.vortikal.web.decorating.components.ResourceMediaPlayerComponent;

public class DisplayMediaController implements Controller {

    private ResourceMediaPlayerComponent mediaPlayerComponent;
    private String viewName;  
    private PropertyTypeDefinition descriptionPropDef;
    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        
        Map<Object,Object> model = new HashMap<Object,Object>();
        
        RequestContext requestContext = RequestContext.getRequestContext();
        String token = requestContext.getSecurityToken();
        Path uri = requestContext.getResourceURI();
        Repository repository = requestContext.getRepository();
        Resource resource = repository.retrieve(token, uri, true);
        
        getMediaPlayerComponent().addMediaPlayer(model, token, repository, uri.toString());
        
        Property descriptionProp = resource.getProperty(getDescriptionPropDef());
        
        if (descriptionProp != null)
            model.put("description", descriptionProp.getStringValue());
        
        return new ModelAndView(viewName,model);
    }
    public void setViewName(String viewName) {
        this.viewName = viewName;
    }
    public String getViewName() {
        return viewName;
    }
    public void setMediaPlayerComponent(ResourceMediaPlayerComponent mediaPlayerComponent) {
        this.mediaPlayerComponent = mediaPlayerComponent;
    }
    public ResourceMediaPlayerComponent getMediaPlayerComponent() {
        return mediaPlayerComponent;
    }
    public void setDescriptionPropDef(PropertyTypeDefinition descriptionPropDef) {
        this.descriptionPropDef = descriptionPropDef;
    }
    public PropertyTypeDefinition getDescriptionPropDef() {
        return descriptionPropDef;
    }

}
