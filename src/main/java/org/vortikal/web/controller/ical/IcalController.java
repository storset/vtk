package org.vortikal.web.controller.ical;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;

public class IcalController implements Controller {
	
	private Repository repository;

	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		String token = SecurityContext.getSecurityContext().getToken();
        Path uri = RequestContext.getRequestContext().getResourceURI();

        Resource resource = this.repository.retrieve(token, uri, true);
        
        String iCalfileName = getICalFileName(resource);
        String iCal = createICal(resource);
        
        ServletOutputStream out = response.getOutputStream();
		response.setContentType("text/calendar");
		response.setHeader("Content-Disposition","filename=" + iCalfileName + ".ics");
		out.print(iCal);
		out.close();
		
		return null;
	}

	private String getICalFileName(Resource resource) {
		String resourceUri = resource.getURI().toString();
		return resourceUri.substring(resourceUri.lastIndexOf("/") + 1, resourceUri.lastIndexOf("."));
	}

	private String createICal(Resource resource) {
		StringBuilder sb = new StringBuilder();
		
		// TODO create ical for event
		
		return sb.toString();
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

}
