package org.vortikal.web.search;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;

public interface SearchComponent {

    public Listing execute(HttpServletRequest request, Resource collection,
            int page, int pageLimit, int baseOffset) throws Exception;

    public Listing execute(HttpServletRequest request, Resource collection,
            int page, int pageLimit, int baseOffset, Boolean pRecursive) throws Exception;

    public PropertyTypeDefinition getPublishedDatePropDef();

    public PropertyTypeDefinition getAuthorPropDef();

}