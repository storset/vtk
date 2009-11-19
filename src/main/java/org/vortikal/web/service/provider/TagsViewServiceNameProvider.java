package org.vortikal.web.service.provider;

import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.vortikal.repository.Resource;
import org.vortikal.web.service.URL;
import org.vortikal.web.tags.TagsHelper;

public class TagsViewServiceNameProvider implements ServiceNameProvider {

    private TagsHelper tagsHelper;

    public String getLocalizedName(Resource resource, HttpServletRequest request) {

        String tag = request.getParameter(TagsHelper.TAG_PARAMETER);
        if (!StringUtils.isBlank(tag)) {
            try {
                tag = URL.decode(tag, "utf-8");
            } catch (UnsupportedEncodingException e) {
                // Don't break the entire service if uridecoding fails
            }
        }

        return this.tagsHelper.getTitle(request, resource, tag);

    }

    public void settagsHelper(TagsHelper tagsHelper) {
        this.tagsHelper = tagsHelper;
    }

}
