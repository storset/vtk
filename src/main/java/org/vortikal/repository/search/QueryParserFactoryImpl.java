package org.vortikal.repository.search;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.ResourceTypeTree;

public class QueryParserFactoryImpl implements QueryParserFactory {

    private ResourceTypeTree resourceTypeTree;

    public QueryParser getParser() {
        // Generated from QueryParserImpl.jj:
        return new QueryParserImpl(resourceTypeTree);
    }

    @Required
    public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
    }

}
