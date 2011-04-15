package org.vortikal.web.display.image;

import java.util.HashMap;

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

public class DisplayImageWithInfoController implements Controller {

    private String viewName;
    private PropertyTypeDefinition titlePropDef;
    private PropertyTypeDefinition descriptionPropDef;

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        Path uri = requestContext.getResourceURI();
        Repository repository = requestContext.getRepository();
        String token = requestContext.getSecurityToken();
        Resource resource = repository.retrieve(token, uri, true);

        Property titleProp = resource.getProperty(titlePropDef);
        Property descriptionProp = resource.getProperty(descriptionPropDef);

        HashMap<String, Object> model = new HashMap<String, Object>();
        model.put("src", uri);
        
        model.put("resource", resource);   
        if (titleProp != null)
            model.put("title", titleProp.getStringValue());
        if (descriptionProp != null)
            model.put("description", descriptionProp.getStringValue());
        
        return new ModelAndView(getViewName(), model);
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public String getViewName() {
        return viewName;
    }

    public void setTitlePropDef(PropertyTypeDefinition titlePropDef) {
        this.titlePropDef = titlePropDef;
    }

    public PropertyTypeDefinition getTitlePropDef() {
        return titlePropDef;
    }

    public void setDescriptionPropDef(PropertyTypeDefinition descriptionPropDef) {
        this.descriptionPropDef = descriptionPropDef;
    }

    public PropertyTypeDefinition getDescriptionPropDef() {
        return descriptionPropDef;
    }

}
