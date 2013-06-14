package org.vortikal.web.decorating.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
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

public class ResourceListComponent extends ViewRenderingDecoratorComponent {

    private static final String DEFAULT_RESOURCE_TYPE = "file";

    @Override
    protected void processModel(Map<String, Object> model, DecoratorRequest request, DecoratorResponse response)
            throws Exception {

        String includeFolders = request.getStringParameter("folders");
        if (StringUtils.isBlank(includeFolders)) {
            throw new DecoratorComponentException("Parameter \"folders\" is required.");
        }

        String resourceType = DEFAULT_RESOURCE_TYPE;
        String requestedResourceType = request.getStringParameter("resource-type");
        resourceType = requestedResourceType != null ? requestedResourceType : resourceType;
        String maxItems = request.getStringParameter("max-items");
        String goToFolderLink = request.getStringParameter("go-to-folder-link");
        String resultSets = request.getStringParameter("result-sets");

        List<Path> validFolders = getValidFolders(includeFolders);

        model.put("folders", validFolders);
        model.put("numberOfFolders", validFolders.size());

        for (Path folder : validFolders) {

            AndQuery query = new AndQuery();
            query.add(new TypeTermQuery(resourceType, TermOperator.IN));
            query.add(new UriPrefixQuery(folder.toString()));

            Search search = new Search();
            if (maxItems != null) {
                try {
                    search.setLimit(Integer.parseInt(maxItems));
                } catch (NumberFormatException e) {
                }
            }
            if (resultSets != null) {
                model.put("resultSets", Integer.parseInt(resultSets));
            } else {
                model.put("resultSets", 2);
            }

            // XXX Sorting?
            // XXX Default limit?
            search.setQuery(query);

            RequestContext requestContext = RequestContext.getRequestContext();
            String token = requestContext.getSecurityToken();
            Repository repository = requestContext.getRepository();
            ResultSet results = repository.search(token, search);

            model.put(folder.toString(), results.getAllResults());

            if ("true".equals(goToFolderLink)) {
                model.put("goToFolderLink", "true");
            }
        }

    }

    private List<Path> getValidFolders(String includeFolders) {
        List<Path> validPaths = new ArrayList<Path>();
        String[] folders = includeFolders.split(",");
        for (String folder : folders) {
            try {
                Path validPath = Path.fromString(folder.trim());
                validPaths.add(validPath);
            } catch (IllegalArgumentException iae) {
                // Invalid path, ignore
            }
        }
        return validPaths;
    }

}
