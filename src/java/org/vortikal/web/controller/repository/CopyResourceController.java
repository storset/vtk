package org.vortikal.web.controller.repository;


import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
 *   <li><code>resourceName</code> - If this is is specified, create new resource with this name.
 *      Otherwise, use copied resource name</li>
 *   <li><code>templateUri</code> - required - Uri of the resource to copy. 
 *   <li><code>trustedToken</code> - If specified, use this token to copy. 
 *      Otherwise use the current security context.
 * 	 <li><code>successView</code> - default is 'redirect'
 * 	 <li><code>errorView</code> - default is 'admin'
 *  
 * </ul>
 * </p>
 *
 * <p>Model data provided:
 * <ul>
 *   <li><code>resource</code> - the resource to redirect to on success
 * 	 <li><code>error</code> - error message on error
 * </ul>
 */
public class CopyResourceController implements Controller,
        InitializingBean {
    
    private Log logger = LogFactory.getLog(CopyResourceController.class);
    private String trustedToken;
    private Repository repository;
    private String resourceName;
    private String templateUri;
    private String errorView = "admin";
    private String successView = "redirect";
    
    
    public ModelAndView handleRequest(HttpServletRequest req,
            HttpServletResponse resp) throws Exception {

        Map model = new HashMap();

        String uri = RequestContext.getRequestContext().getResourceURI();

        String token = trustedToken;
        if (token == null)
            token = SecurityContext.getSecurityContext().getToken();

        String name = resourceName;
        if (name == null) {
            Resource template = repository.retrieve(token, templateUri,false);
            name = template.getName();
        }

        // ensure the uri won't start with '//'
        String newResourceUri = "/" + name;
        if (! uri.equals("/")) {
            newResourceUri = uri + newResourceUri;
        }

        boolean exists = repository.exists(token, newResourceUri);

        if (exists) {
            model.put("createErrorMessage", "resource.exists");
            return new ModelAndView(errorView, model);
        }
            
        repository.copy(trustedToken, templateUri, newResourceUri, "infinity", false, true);
    	 	Resource newResource = repository.retrieve(trustedToken, newResourceUri, true);
            
        model.put("resource", newResource);
        
        return new ModelAndView(successView, model);
    }

    public void afterPropertiesSet() throws Exception {

        if (repository == null) 
            throw new BeanInitializationException("Property 'repository' required");
        
        if (templateUri == null)
            throw new BeanInitializationException("Property 'templateUri' required");
    
//        if (trustedToken == null)
//            throw new BeanInitializationException("Property 'trustedToken' required");
        
        if (! (trustedToken == null || repository.exists(trustedToken,templateUri)))
            //throw new BeanInitializationException("Property 'templateUri' must specify an existing resource");
            logger.warn("Property 'templateUri' must specify an existing resource");
    }

    public void setErrorView(String errorView) {
        this.errorView = errorView;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public void setSuccessView(String successView) {
        this.successView = successView;
    }

    public void setTrustedToken(String trustedToken) {
        this.trustedToken = trustedToken;
    }

    public void setTemplateUri(String templateUri) {
        this.templateUri = templateUri;
    }
    
    

}
