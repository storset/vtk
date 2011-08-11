package org.vortikal.web.referencedata.provider;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.vortikal.web.referencedata.ReferenceDataProvider;

public class SessionBeanProvider implements ReferenceDataProvider {

    private String attributeName;
    private String modelName;
    
    
    
    @Override
    public void referenceData(Map model, HttpServletRequest request) throws Exception {
        System.out.println("*************************************** HELLOOOOOO");
        HttpSession session = request.getSession(true);
        if (session == null) return;
        Object o = session.getAttribute(this.attributeName);
        model.put(this.modelName, o);
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

}