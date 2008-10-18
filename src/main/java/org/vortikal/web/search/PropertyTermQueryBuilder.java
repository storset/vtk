package org.vortikal.web.search;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.search.query.PropertyTermQuery;
import org.vortikal.repository.search.query.Query;
import org.vortikal.repository.search.query.TermOperator;

public class PropertyTermQueryBuilder implements QueryBuilder {

    private String parameterName;
    private PropertyTypeDefinition propertyTypeDefinition;
    
    public Query build(Resource base, HttpServletRequest request) {
        return new PropertyTermQuery(propertyTypeDefinition, request.getParameter(parameterName), TermOperator.EQ_IGNORECASE);
    }

    @Required
    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }

    @Required
    public void setPropertyTypeDefinition(
            PropertyTypeDefinition propertyTypeDefinition) {
        this.propertyTypeDefinition = propertyTypeDefinition;
    }

}
