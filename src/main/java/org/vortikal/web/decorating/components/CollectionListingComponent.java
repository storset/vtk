/* Copyright (c) 2011, University of Oslo, Norway
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of the University of Oslo nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *      
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.vortikal.web.decorating.components;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.decorating.DecoratorRequest;
import org.vortikal.web.decorating.DecoratorResponse;
import org.vortikal.web.search.SearchComponent;

public class CollectionListingComponent extends ViewRenderingDecoratorComponent {

    private SearchComponent search;

    private final static String PARAMETER_URI = "uri";
    private final static String PARAMETER_URI_DESCRIPTION = "Uri to the folder. This is a required parameter";
    private final static String PARAMETER_MAX_ITEMS = "max-items";
    private final static String PARAMETER_MAX_ITEMS_DESCRIPTION = "Defines how many items from the folder that will be visable in the list. Any defined value must be above 0 else the default value is 10";
    private final static String PARAMETER_GO_TO_FOLDER_LINK = "go-to-folder-link";
    private final static String PARAMETER_GO_TO_FOLDER_LINK_DESCRIPTION = "Set to 'true' to show 'Go to folder' link. Default is false";
    private final static String PARAMETER_FOLDER_TITLE = "folder-title";
    private final static String PARAMETER_FOLDER_TITLE_DESCRIPTION = "Set to 'true' to show folder title. Default is false";
    private final static String PARAMETER_COMPACT_VIEW = "compact-view";
    private final static String PARAMETER_COMPACT_VIEW_DESCRIPTION = "Set to 'true' to show compact view. Default is false";

    protected void processModel(Map<String, Object> model, DecoratorRequest request, DecoratorResponse response)
            throws Exception {

        Map<String, Object> conf = new HashMap<String, Object>();

        String uri = request.getStringParameter(PARAMETER_URI);
        if (uri == null)
            throw new DecoratorComponentException("Component parameter 'uri' is required");

        Repository r = RequestContext.getRequestContext().getRepository();
        String token = SecurityContext.getSecurityContext().getToken();

        Resource collection;
        try {
            collection = r.retrieve(token, Path.fromString(uri), false);
        } catch (Exception e) {
            collection = null;
        }

        if (collection == null || !collection.isCollection())
            throw new DecoratorComponentException(uri + " is not a folder");

        int maxItems = 10;
        try {
            if ((maxItems = Integer.parseInt(request.getStringParameter(PARAMETER_MAX_ITEMS))) <= 0)
                maxItems = 10;
        } catch (Exception ignore) {
        }

        boolean goToFolderLink, folderTitle = parameterHasValue(PARAMETER_FOLDER_TITLE, "true", request);
        if (goToFolderLink = parameterHasValue(PARAMETER_GO_TO_FOLDER_LINK, "true", request)) {
            model.put("goToFolderLink", uri);
            model.put("folderTitle", collection.getTitle());
        } else if (folderTitle)
            model.put("folderTitle", collection.getTitle());

        conf.put("goToFolderLink", goToFolderLink);
        conf.put("folderTitle", folderTitle);

        conf.put("compactView", parameterHasValue(PARAMETER_COMPACT_VIEW, "true", request));

        model.put("list", search.execute(request.getServletRequest(), collection, 1, maxItems, 0).getFiles());
        model.put("conf", conf);
    }

    protected Map<String, String> getParameterDescriptionsInternal() {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(PARAMETER_URI, PARAMETER_URI_DESCRIPTION);
        map.put(PARAMETER_MAX_ITEMS, PARAMETER_MAX_ITEMS_DESCRIPTION);
        map.put(PARAMETER_GO_TO_FOLDER_LINK, PARAMETER_GO_TO_FOLDER_LINK_DESCRIPTION);
        map.put(PARAMETER_FOLDER_TITLE, PARAMETER_FOLDER_TITLE_DESCRIPTION);
        map.put(PARAMETER_COMPACT_VIEW, PARAMETER_COMPACT_VIEW_DESCRIPTION);
        return map;
    }

    private boolean parameterHasValue(String param, String includeParamValue, DecoratorRequest request) {
        String itemDescriptionString = request.getStringParameter(param);
        if (itemDescriptionString != null && includeParamValue.equalsIgnoreCase(itemDescriptionString)) {
            return true;
        }
        return false;
    }

    @Required
    public void setSearch(SearchComponent search) {
        this.search = search;
    }

    protected String getDescriptionInternal() {
        return "Inserts a folder item list component on the page";
    }
}
