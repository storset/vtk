package org.vortikal.repositoryimpl.query.parser;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repositoryimpl.query.query.Query;

public class QueryParserFactoryImpl implements Parser, InitializingBean {

    private ResourceTypeTree resourceTypeTree;

    public Query parse(String query) {
        QueryParser parser = new QueryParser(resourceTypeTree);
        return parser.parse(query);
    }

    public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
    }

    public void afterPropertiesSet() throws Exception {
        if (this.resourceTypeTree == null) {
            throw new BeanInitializationException("Java bean property 'resourceTypeTree' must be set");
        }
    }


}
