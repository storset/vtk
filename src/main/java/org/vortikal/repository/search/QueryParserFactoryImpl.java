package org.vortikal.repository.search;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.ResourceTypeTree;

public class QueryParserFactoryImpl implements QueryParserFactory, InitializingBean {

    private ResourceTypeTree resourceTypeTree;

    public QueryParser getParser() {
        // Generated from QueryParserImpl.jj:
        return new QueryParserImpl(resourceTypeTree);
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
