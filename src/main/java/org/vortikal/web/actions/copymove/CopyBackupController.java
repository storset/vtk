package org.vortikal.web.actions.copymove;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.web.RequestContext;


public class CopyBackupController implements Controller {
    
    private CopyHelper copyHelper;

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String uri = null;
        try {
            uri = (String) request.getParameter("uri");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
        if (uri == null) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
        int uriLen = uri.length() - 1;
        if(uri.lastIndexOf("/") == uriLen) {
            uri = uri.substring(0, uriLen);
        }
        
        // Retrieve resource
        RequestContext requestContext = RequestContext.getRequestContext();
        Path resourceToCopySrcUri = Path.fromString(uri);
        String token = requestContext.getSecurityToken();
        Repository repository = requestContext.getRepository();
        Resource resource = repository.retrieve(token, resourceToCopySrcUri, false);
        
        // Copy resource
        Path resourceCopyDestUri = copyHelper.makeDestUri(resourceToCopySrcUri, repository, token, resource);
        repository.copy(token, resourceToCopySrcUri, resourceCopyDestUri, false, true);

        // Return 201 with destination uri in the Location-header
        response.setStatus(HttpServletResponse.SC_CREATED);
        response.setHeader("Location", resourceCopyDestUri.toString());
        
        return null;
    }

    @Required
    public void setCopyHelper(CopyHelper copyHelper) {
        this.copyHelper = copyHelper;
    }

}
