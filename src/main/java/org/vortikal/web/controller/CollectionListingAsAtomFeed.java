package org.vortikal.web.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.search.PropertySortField;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.Searcher;
import org.vortikal.repository.search.SortField;
import org.vortikal.repository.search.SortingImpl;
import org.vortikal.repository.search.query.AndQuery;
import org.vortikal.repository.search.query.Query;
import org.vortikal.repository.search.query.UriDepthQuery;
import org.vortikal.repository.search.query.UriPrefixQuery;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.repository.URIUtil;
import org.vortikal.web.RequestContext;
import org.vortikal.web.controller.search.SearchComponent;
import org.vortikal.web.service.Service;

public class CollectionListingAsAtomFeed implements Controller {

    private static Namespace NS = Namespace.DEFAULT_NAMESPACE;

    private Repository repository;
    private Service viewService;
    private Abdera abdera;
    private List<SearchComponent> searchComponents;

    public ModelAndView handleRequest(HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        Feed feed = abdera.newFeed();

        String token = SecurityContext.getSecurityContext().getToken();
        String uri = RequestContext.getRequestContext().getResourceURI();

        Resource resource = this.repository.retrieve(token, uri, true);
        
        if (!resource.isCollection()) {
            return null;
        }

        String viewUrl = viewService.constructLink(uri);
        String host = viewService.constructURL(uri).getHost();

        String prefix = "tag:" + host + ",2008:";
        feed.setId(prefix + uri);
        feed.setTitle(resource.getTitle());
        feed.setUpdated(resource.getLastModified());
        feed.addAuthor(resource.getModifiedBy().getDescription());
        feed.addLink(viewUrl, "self");
        
        Map<String, Object> searchResult = new HashMap<String, Object>();
        for (SearchComponent searchComponent : searchComponents) {
            searchResult.putAll(searchComponent.execute(request, resource));
        }
        
        List<PropertySet> files = (List<PropertySet>) searchResult.get("files");
        for (PropertySet child : files) {
            Entry entry = feed.addEntry();
            entry.setId(child.getURI());
            entry.setTitle(child.getName());
            entry.addCategory(child.getResourceType());
            Property prop = child.getProperty(NS, PropertyType.TITLE_PROP_NAME);
            entry.setTitle(prop.getFormattedValue("name", null));

            prop = child.getProperty(NS, "description");
            if (prop != null) {
                entry.setSummary(prop.getFormattedValue());
            }

            prop = child.getProperty(NS, PropertyType.LASTMODIFIED_PROP_NAME);
            entry.setUpdated(prop.getDateValue());

            prop = child.getProperty(NS, PropertyType.CREATIONTIME_PROP_NAME);
            entry.setPublished(prop.getDateValue());

            prop = child.getProperty(NS, PropertyType.MODIFIEDBY_PROP_NAME);
            entry.addAuthor(prop.getFormattedValue("name", null));

            entry.addLink(viewService.constructLink(child.getURI()));
        }

        response.setContentType("application/atomxml;charset=utf-8");
        feed.writeTo(response.getWriter());

        return null;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @Required
    public void setViewService(Service viewService) {
        this.viewService = viewService;
    }

    @Required
    public void setAbdera(Abdera abdera) {
        this.abdera = abdera;
    }

    @Required
    public void setSearchComponents(List<SearchComponent> searchComponents) {
        this.searchComponents = searchComponents;
    }

}
