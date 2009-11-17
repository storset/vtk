package org.vortikal.web.service.provider;

import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.support.RequestContext;
import org.vortikal.repository.Path;
import org.vortikal.repository.Resource;
import org.vortikal.web.service.URL;
import org.vortikal.web.tags.TagsHelper;

public class TagsViewServiceNameProvider implements ServiceNameProvider {

    private String repositoryId;

    public String getLocalizedName(Resource resource, HttpServletRequest request) {

        String tag = request.getParameter(TagsHelper.TAG_PARAMETER);
        boolean noTagSpecified = StringUtils.isBlank(tag);
        try {
            tag = URL.decode(tag, "utf-8");
        } catch (UnsupportedEncodingException e) {
            // Don't break the entire service if uridecoding fails
        }

        RequestContext rc = new RequestContext(request);

        String titleKey = "tags.title";
        String[] resourceParams = request.getParameterValues(TagsHelper.RESOURCE_TYPE_PARAMETER);
        if (resourceParams != null && resourceParams.length == 1) {
            String tmpKey = titleKey + "." + resourceParams[0];
            try {
                rc.getMessage(tmpKey);
                titleKey = tmpKey;
            } catch (Exception e) {
                // key doesn't exist, ignore it
            }
        }

        if (Path.ROOT.equals(resource.getURI())) {
            return rc.getMessage(noTagSpecified ? "tags.serviceTitle" : titleKey, 
                    new Object[] { repositoryId, tag });
        }
        return rc
                .getMessage(noTagSpecified ? "tags.serviceTitle" : titleKey, 
                        new Object[] { resource.getTitle(), tag });

    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

}
