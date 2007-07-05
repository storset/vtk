package org.vortikal.edit.xml;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

public final class Util {

    public static Map<String, String> getRequestParameterMap(HttpServletRequest request) {
        Map<String, String> parameterMap = new HashMap<String, String>();
        for (Enumeration<String> e = request.getParameterNames(); e.hasMoreElements();) {
            String key = e.nextElement();
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
