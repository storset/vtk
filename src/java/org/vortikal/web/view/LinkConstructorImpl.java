package org.vortikal.web.view;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;

public class LinkConstructorImpl implements LinkConstructor,
		ApplicationContextAware, InitializingBean {

    private Log logger = LogFactory.getLog(this.getClass());
    
	private Repository repository;
	private ApplicationContext context;
	
	public String construct(String resourceURI, String parametersCSV, String serviceName) {

        logger.debug("About to construct link to service '" + serviceName + "' for resource '"
                + resourceURI + "' with parameters '" + parametersCSV + "'");
        
        String token = SecurityContext.getSecurityContext().getToken();
		Principal principal = SecurityContext.getSecurityContext().getPrincipal();

		Service service = null;
		Resource resource = null;
		Map parameters = null;

		if (resourceURI == null || resourceURI.equals(""))
			resourceURI = RequestContext.getRequestContext().getResourceURI();
		
		if (serviceName == null || serviceName.equals(""))
			service = RequestContext.getRequestContext().getService();
		
		try {
            
			resource = repository.retrieve(token, resourceURI, true);

			if (service == null) {
				service = getService(serviceName);
            }
            
            if (parametersCSV != null && !parametersCSV.trim().equals("")) {
                logger.debug("Trying to get parameters");
                parameters = getParametersMap(parametersCSV);
            }
            
                logger.debug("Trying to construct link to service '" + service.getName()
                        + "' for resource '" + resource.getURI() + "'");

			return service.constructLink(resource, principal, parameters, false);
		
		
		} catch (Exception e) {
            logger.warn("Caught exception on link construction", e);
            return "";
		}
	}

	private Service getService(String serviceName) {
        return (Service) context.getBean(serviceName, Service.class);
    }

	private Map getParametersMap(String parametersCSV) {
		Map parameters = new HashMap();
		String[] mappings = parametersCSV.split(",");
		for (int i = 0; i < mappings.length; i++) {
			if (mappings[i].indexOf("=") == -1) {
				throw new IllegalArgumentException(
						"Each entry in the parameters string must be in the format "
						+ "'<paramname>=<paramvalue>'");
			}	

			String parameterName = mappings[i].substring(0, mappings[i].indexOf("=")).trim();
			
			String parameterValue = mappings[i].substring(mappings[i].lastIndexOf("=") + 1).trim();
			parameters.put(parameterName, parameterValue);
		}
		return parameters;
	}

	public void afterPropertiesSet() throws Exception {
		if (context == null) throw new BeanInitializationException(
				"Property 'applicationContext' must be set");
		if (repository == null) throw new BeanInitializationException(
		"Property 'repository' must be set");
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}
	
	public void setApplicationContext(ApplicationContext context)
	throws BeansException {
		this.context = context;	
	}


}
