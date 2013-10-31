package org.vortikal.web.actions.copymove;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

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
            badRequest(e, response);
            return null;
        }
        if (uri == null) {
            return null;
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

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/plain;charset=utf-8");
        PrintWriter writer = response.getWriter();
        try {
            writer.print("{ 'uri': '" + resourceCopyDestUri + "' }");
        } finally {
            writer.close();
        }
        
        return null;
    }
    
    private void badRequest(Throwable e, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        PrintWriter writer = response.getWriter();
        try {
            writer.write(e.getMessage());
        } finally {
            writer.close();
        }
    }

    @Required
    public void setCopyHelper(CopyHelper copyHelper) {
        this.copyHelper = copyHelper;
    }

}
