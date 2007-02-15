package org.vortikal.repositoryimpl.query.parser;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repositoryimpl.query.query.Query;

public class QueryParserFactoryImpl implements QueryParserFactory, InitializingBean {

    private ResourceTypeTree resourceTypeTree;

    public Parser getParser() {
        // Generated from QueryParser.jj:
        return new QueryParser(resourceTypeTree);
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
