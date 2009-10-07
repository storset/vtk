package org.vortikal.web.decorating.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.search.Searcher;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.decorating.DecoratorRequest;
import org.vortikal.web.decorating.DecoratorResponse;
import org.vortikal.web.referencedata.provider.BreadCrumbProvider;
import org.vortikal.web.referencedata.provider.BreadcrumbElement;
import org.vortikal.web.service.Service;

public class BreadcrumbMenuComponent extends ViewRenderingDecoratorComponent {
    private static final String PARAMETER_AUTENTICATED = "authenticated";
    private static final String PARAMETER_AUTENTICATED_DESC = "The default is that only resources readable for everyone is listed. "
            + "If this is set to 'true', the listing is done as the currently "
            + "logged in user (if any).";

    private static final String PARAMETER_DISPLAY_FROM_LEVEL = "display-from-level";
    private static final String PARAMETER_DISPLAY_FROM_LEVEL_DESC = "Defines the starting URI level for the menu";

    private Repository repository;
    private Service service;
    private int displayFromLevel = -1;
    private String token;
    private PropertyTypeDefinition navigationTitlePropDef;

    public void processModel(Map<Object, Object> model,DecoratorRequest request, DecoratorResponse response) throws Exception {
        initRequestParameters(request);
        Path uri = RequestContext.getRequestContext().getResourceURI();
        Principal principal = SecurityContext.getSecurityContext().getPrincipal();
        boolean isIndexFile = RequestContext.getRequestContext().isIndexFile();
        
        List<BreadcrumbElement> breadCrumbElements = getBreadcrumbElements();
        if (displayFromLevel > breadCrumbElements.size()) {
            return;
        }
        for (int i = 0; i < displayFromLevel; i++) {
            breadCrumbElements.remove(i);
        }

        Resource currentResource = repository.retrieve(token, uri, false);
        if (!currentResource.isCollection()) {
            currentResource = repository.retrieve(token, uri.getParent(), false);
            if (!isIndexFile) { // Files that are not index files shall not be displayed in the menu.
                breadCrumbElements.remove(breadCrumbElements.size() - 1);
            }
        }
        String markedUrl = this.service.constructLink(currentResource,principal, false);

        Map<String, String> childElements = generateChildElements( currentResource.getChildURIs(), principal);        
        // If there is no children of the current resource, then we shall instead display the children of the parent node. 
        if (childElements.size() == 0) { 
            Resource childResource = repository.retrieve(token, currentResource.getURI().getParent(), false);
            childElements = generateChildElements(childResource.getChildURIs(), principal);
            breadCrumbElements.remove(breadCrumbElements.size() - 1); 
        }
        model.put("breadcrumb", breadCrumbElements);
        model.put("children", childElements);
        model.put("markedurl", markedUrl);
    }
    
    private List<BreadcrumbElement> getBreadcrumbElements() throws Exception{
        List<BreadcrumbElement> list = new ArrayList<BreadcrumbElement>();
        String breadcrumbName = "breadcrumb"; 
        BreadCrumbProvider p = new BreadCrumbProvider();
        p.setService(service);
        p.setRepository(repository);
        p.setBreadcrumbName(breadcrumbName);
        p.setSkipIndexFile(true);
        p.afterPropertiesSet();
        HashMap map = new HashMap();
        p.referenceData(map, RequestContext.getRequestContext().getServletRequest());
        BreadcrumbElement[] l = (BreadcrumbElement[]) map.get(breadcrumbName);
        for(int i = 0;i < l.length;i++){
            list.add(l[i]);
        }
        return list;
    }

    private Map<String, String> generateChildElements(List<Path> children, Principal principal) throws Exception {
        Map<String, String> childElements = new LinkedHashMap<String, String>();
        for (int i = 0; i < children.size(); i++) {
            Resource childResource = repository.retrieve(token,children.get(i), false);
            if (!childResource.isCollection()) {
                continue;
            }
            String url = this.service.constructLink(childResource, principal,false);
            childElements.put(url, getMenuTitle(childResource)); 
        }
        return childElements;
    }

    private String getMenuTitle(Resource resource) {
        Property prop = resource.getProperty(this.navigationTitlePropDef);
        if (prop != null) {
            return prop.getStringValue();
        }
        return resource.getTitle();
    }
    
    private void initRequestParameters(DecoratorRequest request) {
        String displayFromLevel = request.getStringParameter(PARAMETER_DISPLAY_FROM_LEVEL);
        if (displayFromLevel != null && !"".equals(displayFromLevel.trim())) {
            int level = Integer.parseInt(displayFromLevel);
            if (level <= 0) {
                throw new DecoratorComponentException("Parameter '"
                        + PARAMETER_DISPLAY_FROM_LEVEL
                        + "' must be an integer > 0");
            }
            this.displayFromLevel = level;
        }
        boolean authenticated = "true".equals(request.getStringParameter(PARAMETER_AUTENTICATED));
        if (authenticated) {
            SecurityContext securityContext = SecurityContext.getSecurityContext();
            this.token = securityContext.getToken();
        }
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public Service getService() {
        return service;
    }

    public void setNavigationTitlePropDef(PropertyTypeDefinition navigationTitlePropDef) {
        this.navigationTitlePropDef = navigationTitlePropDef;
    }

    public PropertyTypeDefinition getNavigationTitlePropDef() {
        return navigationTitlePropDef;
    }
}
