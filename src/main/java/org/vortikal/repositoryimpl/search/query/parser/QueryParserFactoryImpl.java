package org.vortikal.repositoryimpl.search.query.parser;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.search.query.Parser;
import org.vortikal.repository.search.query.QueryParserFactory;

public class QueryParserFactoryImpl implements QueryParserFactory, InitializingBean {

    private ResourceTypeTree resourceTypeTree;

    public Parser getParser() {
        // Generated from PreprocessingQueryParser.jj:
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
