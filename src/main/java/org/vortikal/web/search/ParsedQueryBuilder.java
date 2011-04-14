package org.vortikal.web.search;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Resource;
import org.vortikal.repository.search.QueryParser;
import org.vortikal.repository.search.query.Query;

public class ParsedQueryBuilder implements QueryBuilder {

    protected String queryString;
    protected QueryParser queryParser;
    
    public Query build(Resource base, HttpServletRequest request) {
        return this.queryParser.parse(this.queryString);
    }

    @Required
    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    @Required
    public void setQueryParser(QueryParser queryParser) {
        this.queryParser = queryParser;
    }


}
