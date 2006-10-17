package org.vortikal.edit.xml;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

public final class Util {

    public static Map getRequestParameterMap(HttpServletRequest request) {
        Map parameterMap = new HashMap();
        for (Enumeration e = request.getParameterNames(); e.hasMoreElements();) {
            String key = (String) e.nextElement();
    
            parameterMap.put(key, request.getParameter(key));
        }
        return parameterMap;
    
    }

    public final static void setXsltParameter(Map model, String key, Object value) {
        Map parameters = (Map)model.get("xsltParameters");
        if (parameters == null) {
            parameters = new HashMap();
            model.put("xsltParameters", parameters);
        }
        parameters.put(key, value);
    }

}
