package org.vortikal.web.controller.autocomplete;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

public class AutoCompleteHandler implements Controller {
    
    private AutoCompleteDataProvider dataProvider;
    private String fieldName;

    public ModelAndView handleRequest(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        
        String query = request.getParameter(fieldName);
        
        //Map<String, List<Object>> result = dataProvider.getData(fieldName, query);
        
        
        // TODO -> remove, mock data for testing
        Map<String, List<Object>> result = new HashMap<String, List<Object>>();
        List<Object> tags = new ArrayList<Object>();
        tags.add("java");
        tags.add("c++");
        result.put(fieldName, tags);
        // END remove
        
        
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(result);
        
        response.setContentType("application/json");
        jsonObject.write(response.getWriter());
        
        return null;
    }
    
//    @Required
//    public void setDataProvider(AutoCompleteDataProvider dataProvider) {
//        this.dataProvider = dataProvider;
//    }
    
    @Required
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }
    
}
