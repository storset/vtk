package org.vortikal.repositoryimpl.queryparser;

import java.util.Iterator;

import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repositoryimpl.PropertyManagerImpl;

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
        QueryNode q = parser.parse(queryString);
        Query query = buildQuery(q);
        
        return execute(token, query); 
    }
    
    public ResultSet execute(String token, Query query) {

        validateQuery(query);
        
        return searcher.execute(token, query);
    }
    
    private Query buildQuery(QueryNode node) {
        if (node.getNodeName().equals(AndQuery.class.getName())) {
            AndQuery and = new AndQuery();
            for (Iterator iter = node.getChildren().iterator(); iter.hasNext();) {
                QueryNode child = (QueryNode) iter.next();
                and.add(buildQuery(child));
            }
            return and;
        } else if (node.getNodeName().equals(OrQuery.class.getName())) {
            OrQuery or = new OrQuery();
            for (Iterator iter = node.getChildren().iterator(); iter.hasNext();) {
                QueryNode child = (QueryNode) iter.next();
                or.add(buildQuery(child));
            }
            return or;
        } else if (node.getNodeName().equals(PropertyQuery.class.getName())) {
            return parsePropertyQuery(node);
        }
        
        throw new QueryException("Unknown query node type '" + node.getNodeName() + "'");
    }
    
    private Query parsePropertyQuery(QueryNode node) {
        return null;
    }
    
    private void validateQuery(Query query) {
       
    }

    public void setPropertyManager(PropertyManagerImpl propertyManager) {
        this.propertyManager = propertyManager;
    }
}
