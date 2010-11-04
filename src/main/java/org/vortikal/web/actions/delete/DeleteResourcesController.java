package org.vortikal.web.actions.delete;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.security.SecurityContext;

public class DeleteResourcesController implements Controller {

    private String viewName;
    private Repository repository;

    @SuppressWarnings("unchecked")
    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        SecurityContext securityContext = SecurityContext.getSecurityContext();

        Enumeration e = request.getParameterNames();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();

            Path uri = null;
            try {
                uri = Path.fromString(name);
            } catch (IllegalArgumentException iae) {
                // Not a path, ignore it, try next one
                continue;
            }

            String token = securityContext.getToken();
            if (this.repository.exists(token, uri)) {
                repository.delete(token, uri, true);
            }

        }

        return new ModelAndView(this.viewName);
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public String getViewName() {
        return viewName;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public Repository getRepository() {
        return repository;
    }

}
