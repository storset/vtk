package org.vortikal.web.controller.repository;


import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;


/**
 * Controller that copies another resource.
 * TODO: Make this generic
 * 
 * <p>Configurable properties:
 * <ul>
 *   <li><code>repository</code> - the repository is required</li>
 * 	 <li><code>viewName</code> - required - likely to be a redirect to the parent
 *  
 * </ul>
 * </p>
 *
 * <p>Model data provided:
 * <ul>
 *   <li><code>resource</code> - the parent of the resources
 * </ul>
 */
public class DuplicateResourceController implements Controller,
        InitializingBean {
    
    private Repository repository;
    private String viewName;
    
    
    public ModelAndView handleRequest(HttpServletRequest req,
            HttpServletResponse resp) throws Exception {

        Map model = new HashMap();

        String uri = RequestContext.getRequestContext().getResourceURI();
        
        String token = SecurityContext.getSecurityContext().getToken();

        Resource resource = repository.retrieve(token, uri, false);
        
        int index=1;
        boolean cont = true;

        // Need to insert numbering before file-ending like "file(1).txt"
        String startOfUri = "";
        String endOfUri = "";
        int dot = uri.lastIndexOf(".");
        
        if (dot != -1) {
        		startOfUri = uri.substring(0,dot);
        		endOfUri = "." + uri.substring(dot+1);
        } else {
        		startOfUri = uri;
        }	
        
        while (cont) {
        		cont = repository.exists(token, startOfUri + "(" + index + ")" + endOfUri);	
        		if (cont) index++;
        }

        	String newResourceUri = startOfUri + "(" + index + ")" + endOfUri;
        
        repository.copy(token, uri, newResourceUri, "infinity", false, true);
        Resource parent = repository.retrieve(token, resource.getParent(), false);
            
        model.put("resource", parent);
        
        return new ModelAndView(viewName, model);
    }

    public void afterPropertiesSet() throws Exception {

        if (repository == null) 
            throw new BeanInitializationException("Property 'repository' required");
        if (viewName == null)
            throw new BeanInitializationException("Property 'viewName' required");
    }
    
    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

}
