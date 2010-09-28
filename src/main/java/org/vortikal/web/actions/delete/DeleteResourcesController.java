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
        // TODO Auto-generated method stub

        SecurityContext securityContext = SecurityContext.getSecurityContext();

        Enumeration e = request.getParameterNames();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();

            // Need to replace this with a better check...
            if (name.startsWith("/")) {
                try {
                    Path uri = Path.fromString(name);
                    repository.delete(securityContext.getToken(), uri, true);
                } catch (Exception exception) {
                }
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
