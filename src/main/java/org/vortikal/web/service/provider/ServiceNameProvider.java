package org.vortikal.web.service.provider;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.repository.Resource;

public interface ServiceNameProvider {

    public String getLocalizedName(Resource resource, HttpServletRequest request);

}
