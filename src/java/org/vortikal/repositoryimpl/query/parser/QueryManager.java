package org.vortikal.repositoryimpl.query.parser;

import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repositoryimpl.PropertyManagerImpl;
import org.vortikal.repositoryimpl.query.query.Query;

public class QueryManager implements InitializingBean {

    private Parser parser;
    private Searcher searcher;
    private PropertyManagerImpl propertyManager;
    
    public void afterPropertiesSet() throws Exception {
    }

    public void setParser(Parser parser) {
        this.parser = parser;
    }

    public ResultSet execute(String token, String queryString) throws QueryException {
        Query q = parser.parse(queryString);
        
        return execute(token, q); 
    }
    
    public ResultSet execute(String token, Query query) {

        validateQuery(query);
        
        return searcher.execute(token, query);
    }
    
    private void validateQuery(Query query) {
       
    }

    public void setPropertyManager(PropertyManagerImpl propertyManager) {
        this.propertyManager = propertyManager;
    }
}
