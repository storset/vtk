package org.vortikal.web.decorating.components;

import java.util.Map;

import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.search.ResultSet;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.query.AndQuery;
import org.vortikal.repository.search.query.TermOperator;
import org.vortikal.repository.search.query.TypeTermQuery;
import org.vortikal.repository.search.query.UriPrefixQuery;
import org.vortikal.web.RequestContext;
import org.vortikal.web.decorating.DecoratorRequest;
import org.vortikal.web.decorating.DecoratorResponse;

public class NumberOfResourcesComponent extends ViewRenderingDecoratorComponent {

    private static final String DEFAULT_RESOURCE_TYPE = "file";

    @Override
    protected void processModel(Map<String, Object> model, DecoratorRequest request, DecoratorResponse response)
            throws Exception {

        RequestContext requestContext = RequestContext.getRequestContext();
        String token = requestContext.getSecurityToken();

        String resourceType = DEFAULT_RESOURCE_TYPE;
        String requestedResourceType = request.getStringParameter("resource-type");
        resourceType = requestedResourceType != null ? requestedResourceType : resourceType;

        // Current folder
        Path folder = requestContext.getCurrentCollection();

        // Check for any other requested folder
        String requestedFolderString = request.getStringParameter("folder");
        try {
            Path requestedFolder = Path.fromString(requestedFolderString);
            folder = requestedFolder;
        } catch (IllegalArgumentException iae) {
            // Invalid requested folder, ignore and default to current folder
        }

        AndQuery query = new AndQuery();
        query.add(new UriPrefixQuery(folder.toString()));
        query.add(new TypeTermQuery(resourceType, TermOperator.IN));

        // No sorting required, we just want count
        Search search = new Search();
        search.setLimit(1);
        search.setQuery(query);

        Repository repository = requestContext.getRepository();

        ResultSet results = repository.search(token, search);
        model.put("numberOfResources", results.getTotalHits());
    }

}
