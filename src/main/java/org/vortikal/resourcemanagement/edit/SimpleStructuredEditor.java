package org.vortikal.resourcemanagement.edit;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.Repository.Depth;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.Principal;
import org.vortikal.util.text.JSON;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

public class SimpleStructuredEditor implements Controller {

    public static final String ACTION_PARAMETER_VALUE_CANCEL = "cancel";
    public static final String ACTION_PARAMETER_VALUE_DELETE = "delete";
    public static final String ACTION_PARAMETER_VALUE_NEW = "new";
    public static final String ACTION_PARAMETER_VALUE_UPDATE = "update";
 
    private static final String TITLE_PARAMETER = "title";
    private String viewName;
    private PropertyTypeDefinition publishDatePropDef;
    private Service viewService;

    private String resourceType = "structured-message";
    private String[] properties = { "message", TITLE_PARAMETER };

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Map<String, Object> model = new HashMap<String, Object>();

        RequestContext requestContext = RequestContext.getRequestContext();
        String token = requestContext.getSecurityToken();
        Repository repository = requestContext.getRepository();

        Path uri = URL.toPath(request);
        model.put("url", URL.create(request));

        Resource currentResource = repository.retrieve(token, uri, false);
        model.put("isCollection", currentResource.isCollection());

        if ("POST".equals(request.getMethod())) {
            if (request.getParameter(ACTION_PARAMETER_VALUE_CANCEL) != null) {
                repository.unlock(token, uri, null);
                setRedirect(response, currentResource.isCollection() ? uri : uri.getParent(), null, null);
            } else if (request.getParameter(ACTION_PARAMETER_VALUE_DELETE) != null) {
                // This is going to be replaced later
                repository.unlock(token, uri, null);
                repository.delete(token, uri, true);
                setRedirect(response, uri.getParent(), uri, ACTION_PARAMETER_VALUE_DELETE);
            } else if (currentResource.isCollection()) {
                Path newUri = createNewDocument(request, repository, token, uri);
                setRedirect(response, uri, newUri, ACTION_PARAMETER_VALUE_NEW);
            } else if (resourceType.equals(currentResource.getResourceType())) {
                updateDocument(request, token, repository, uri);
                repository.unlock(token, uri, null);
                setRedirect(response, uri.getParent(), uri, ACTION_PARAMETER_VALUE_UPDATE);
            }
        } else if (resourceType.equals(currentResource.getResourceType())) {
            // Edit some document
            Principal principal = requestContext.getPrincipal();
            repository.lock(token, uri, principal.getQualifiedName(), Depth.ZERO, 600, null);
            JSONObject document = JSON.getResource(uri);
            model.put("properties", document.get("properties"));
        }
        return new ModelAndView(viewName, model);
    }

    private void setRedirect(HttpServletResponse response, Path collection, Path document, String action)
            throws IOException, Exception {
        response.addIntHeader("Refresh", 0);

        URL url = viewService.constructURL(collection);
        if (action != null || document != null) {
            url.addParameter("action", action);
            url.addParameter("uri", document.toString());
        }
        response.sendRedirect(url.toString());
    }

    private void updateDocument(HttpServletRequest request, String token, Repository repository, Path uri)
            throws Exception, UnsupportedEncodingException {
        JSONObject document = JSON.getResource(uri);
        Map<String, String> propertyValues = (Map<String, String>) document.get("properties");
        for (String propertyName : properties) {
            propertyValues.put(propertyName, request.getParameter(propertyName));
        }
        document.put("properties", propertyValues);
        InputStream is = new ByteArrayInputStream(document.toString().getBytes("UTF-8"));
        repository.storeContent(token, uri, is);
    }

    private Path createNewDocument(HttpServletRequest request, Repository repository, String token, Path uri)
            throws Exception {
        JSONObject document = new JSONObject();
        document.put("resourcetype", resourceType);
        Map<String, String> propertyValues = new HashMap<String, String>();
        for (String propertyName : properties) {
            propertyValues.put(propertyName, request.getParameter(propertyName));
        }
        document.put("properties", propertyValues);

        InputStream is = new ByteArrayInputStream(document.toString().getBytes("UTF-8"));
        Path newUri = generateFilename(request, repository, token, uri);

        Resource resource = repository.createDocument(token, newUri, is);
        publishResource(resource, token, repository);
        return newUri;
    }

    private Path generateFilename(HttpServletRequest request, Repository repository, String token, Path uri)
            throws AuthorizationException, AuthenticationException, Exception {

        String name = "message";
        if (request.getParameter(TITLE_PARAMETER) != null && !"".equals(request.getParameter(TITLE_PARAMETER))) {
            name = request.getParameter(TITLE_PARAMETER).toLowerCase();
            name = name.replace("æ", "ae").replace("ø", "o").replace("å", "aa");
            name = name.replaceAll("[^A-Za-z0-9 ]", "");
            name = name.replaceAll(" ", "-");
            if (name.length() > 40) {
                name = name.substring(0, 40);
            }
        }

        Path p = uri.expand(name + ".html");
        for (int i = 2; repository.exists(token, p); i++) {
            p = uri.expand(name + "-" + i + ".html");
        }

        return p;
    }

    private void publishResource(Resource resource, String token, Repository repository) throws Exception {
        Property publishDateProp = resource.getProperty(this.publishDatePropDef);
        if (publishDateProp == null) {
            publishDateProp = this.publishDatePropDef.createProperty();
            resource.addProperty(publishDateProp);
        }
        publishDateProp.setDateValue(Calendar.getInstance().getTime());
        repository.store(token, resource);
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public void setPublishDatePropDef(PropertyTypeDefinition publishDatePropDef) {
        this.publishDatePropDef = publishDatePropDef;
    }

    public void setViewService(Service viewService) {
        this.viewService = viewService;
    }

}
