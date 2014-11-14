package vtk.web.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import vtk.repository.Resource;
import vtk.repository.search.QueryParser;
import vtk.repository.search.Search;
import vtk.repository.search.Sorting;
import vtk.repository.search.query.Query;
import vtk.web.RequestContext;
import vtk.web.service.URL;

public class QueryReporter extends DocumentReporter {
    
    private QueryParser parser;
    private static String DEFAULT_QUERY = "type IN resource";
    
    public QueryReporter(QueryParser parser) {
        super();
        this.parser = parser;
    }
    
    @Override
    public Map<String, Object> getReportContent(String token, Resource resource, HttpServletRequest request) {
        Map<String, Object> model = super.getReportContent(token, resource, request);
        model.put("specificCollectionReporter", true);

        Map<String, Object> form = new HashMap<String, Object>();
        URL action = new URL(RequestContext.getRequestContext().getRequestURL());
        
        String query = action.getParameter("query");
        if (query == null || "".equals(query.trim())) query = DEFAULT_QUERY;
        
        List<Object> inputs = new ArrayList<Object>();
        inputs.add(formElement("query", query, "text"));
        
        for (String name: action.getParameterNames()) {
            if (!"query".equals(name) && !"page".equals(name)) {
                inputs.add(formElement(name, action.getParameter(name), "hidden"));
            }
        }
        inputs.add(formElement("submit", "Update", "submit"));
        form.put("action", action);
        form.put("inputs", inputs);
        model.put("form", form);
        return model;
    }

    @Override
    protected Search getSearch(String token, Resource currentResource,
            HttpServletRequest request) {
        String queryStr = request.getParameter("query");
        if (queryStr == null || "".equals(queryStr.trim())) queryStr = DEFAULT_QUERY;
        
        Search search = new Search();
        Sorting sorting = new Sorting();
        //sorting.addSortField(new PropertySortField(this.sortPropDef, this.sortOrder));
        //search.setSorting(sorting);
        
        Query query = parser.parse(queryStr);
        search.setQuery(query);
        return search;
    }

    private Object formElement(String name, String value, String type) {
        Map<String, Object> element = new HashMap<String, Object>();
        element.put("name", name);
        element.put("value", value);
        element.put("type", type);
        return element;
    }
    
}
