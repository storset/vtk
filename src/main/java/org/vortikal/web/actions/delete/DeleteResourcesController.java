package org.vortikal.web.actions.delete;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.web.RequestContext;

public class DeleteResourcesController implements Controller {

    private String viewName;

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        Repository repository = requestContext.getRepository();
        String token = requestContext.getSecurityToken();

        boolean recoverable = true;
        String permanent = request.getParameter("permanent");
        if ("true".equals(permanent)) {
            recoverable = false;
        }

        @SuppressWarnings("rawtypes")
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
            if (repository.exists(token, uri)) {
                repository.delete(token, uri, recoverable);
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
}
