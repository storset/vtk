package org.vortikal.repositoryimpl.queryparser;

import org.springframework.beans.factory.InitializingBean;

public class Test implements InitializingBean {

    private Parser parser;
    private String query;

    public void afterPropertiesSet() throws Exception {
        System.out.println("Query: '" + query + "' produced:");
        parser.parse(query).dump("");
    }

    public void setParser(Parser parser) {
        this.parser = parser;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
