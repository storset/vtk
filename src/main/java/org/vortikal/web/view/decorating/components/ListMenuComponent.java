/* Copyright (c) 2007, University of Oslo, Norway
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
package org.vortikal.web.view.decorating.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.search.HashSetPropertySelect;
import org.vortikal.repository.search.ResultSet;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.Searcher;
import org.vortikal.repository.search.query.Parser;
import org.vortikal.util.repository.URIUtil;
import org.vortikal.util.web.URLUtil;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.view.components.menu.ListMenu;
import org.vortikal.web.view.components.menu.MenuItem;
import org.vortikal.web.view.decorating.DecoratorRequest;
import org.vortikal.web.view.decorating.DecoratorResponse;


/**
 * XXX: How should we handle authorization?
 * TODO: sorting
 */
public class ListMenuComponent extends ViewRenderingDecoratorComponent {

    private static final String STYLE_VERTICAL = "vertical";
    private static final String STYLE_HORIZONTAL = "horizontal";
    private static final String STYLE_TABS = "tabs";

    private static final String DEFAULT_STYLE = STYLE_VERTICAL;

    private static final Set VALID_STYLES;
    static {
        VALID_STYLES = new HashSet();
        VALID_STYLES.add(STYLE_VERTICAL);
        VALID_STYLES.add(STYLE_HORIZONTAL);
        VALID_STYLES.add(STYLE_TABS);
    }

    private static final String PARAMETER_INCLUDE_CHILDREN = "includeChildren";
    private static final String PARAMETER_INCLUDE_PARENT = "includeParentFolder";
    private static final String PARAMETER_INCLUDE_PARENT_DESC = 
        "Defaults to 'false'";
    private static final String PARAMETER_STYLE = "style";
    private static final String PARAMETER_STYLE_DESC = 
        "One of " + VALID_STYLES.toString() + ". Defaults to " + DEFAULT_STYLE;
    private static final String PARAMETER_URI = "uri";
    private static final String PARAMETER_URI_DESC = 
        "What about it?";

    private static Log logger = LogFactory.getLog(IncludeComponent.class);
    
    private Parser queryParser;
    private Service viewService;
    private PropertyTypeDefinition titlePropdef;
    private String modelName = "menu";
    private int searchLimit = 10;
    private Searcher searcher;

    public void processModel(Map model, DecoratorRequest request, DecoratorResponse response)
        throws Exception {

        try {
            String uri = request.getStringParameter(PARAMETER_URI);
            if (uri == null) {
                throw new DecoratorComponentException("Parameter 'uri' not specified");
            }

            String style = request.getStringParameter(PARAMETER_STYLE);
            if (style == null) {
                style = DEFAULT_STYLE;
            } else {
                if (!VALID_STYLES.contains(style)) {
                    throw new DecoratorComponentException(
                        "Invalid value for parameter 'style': must be one of "
                        + VALID_STYLES.toString());
                }
            }

            boolean includeParent = "true".equals(request.getStringParameter(
                                                      PARAMETER_INCLUDE_PARENT));
            String[] splitChildNames = null;
            String includeChildrenParam = request.getStringParameter(PARAMETER_INCLUDE_CHILDREN);
            if (includeChildrenParam != null) {
                splitChildNames = includeChildrenParam.split(",");
            }

            RequestContext requestContext = RequestContext.getRequestContext();
            String currentURI = requestContext.getResourceURI();
            String token = null;
            Search search = buildSearch(uri, includeParent, splitChildNames);
            ResultSet rs = this.searcher.execute(token, search);
            ListMenu menu = buildListMenu(rs, currentURI, style);
            
            model.put(this.modelName, menu);
        } catch (Throwable t) {
            logger.warn("foo: ", t);
            throw new RuntimeException(t);
        }
    }


    private Search buildSearch(String uri, boolean includeParent, String[] childList) {
        int depth = URLUtil.splitUri(uri).length;
        StringBuffer query = new StringBuffer();

        if (childList == null || childList.length == 0) {
            // List all children based on depth:
            query.append("(uri = ").append(uri).append("/*");
            query.append(" AND depth = ").append(depth).append(")");

        } else {
            // An explicit list of child names is provided
            for (int i = 0; i < childList.length; i++) {
                String name = childList[i];
                if (name.indexOf("/") != -1) {
                    throw new DecoratorComponentException("Invalid child name: '" + name + "'");
                }
                name = name.trim();
                // XXX: need to escape white space in names:
                //name = name.replaceAll(" ", "\\\\ ");
                String childURI = URIUtil.makeAbsoluteURI(name, uri);
                query.append("uri = ").append(childURI).append("");
                if (i < childList.length - 1) {
                    query.append(" OR ");
                }
            }
        }
        if (includeParent) {
            query.insert(0, "(");
            query.append(")");
            query.append(" OR uri = ").append(uri);
        }
        query.insert(0, "type IN collection AND (");
        query.append(")");

        HashSetPropertySelect select = new HashSetPropertySelect();
        if (logger.isDebugEnabled()) {
            logger.debug("About to search using query: '" + query + "'");
        }
        Search search = new Search();
        search.setQuery(this.queryParser.parse(query.toString()));
        search.setLimit(this.searchLimit);
        search.setPropertySelect(select);

        return search;
    }
    

    private ListMenu buildListMenu(ResultSet rs, String currentURI, String label) {
        
        List items = new ArrayList();
        Map activeMatches = new HashMap();

        for (int i = 0; i < rs.getSize(); i++) {
            PropertySet resource = (PropertySet) rs.getResult(i);
            
            String url = this.viewService.constructLink(resource.getURI());
            Property titleProperty = resource.getProperty(this.titlePropdef);
            String title = titleProperty != null ?
                titleProperty.getStringValue() : resource.getName();

            MenuItem item = new MenuItem();
            item.setUrl(url);
            item.setTitle(title);
            item.setLabel(title);
            item.setActive(false);
            if (currentURI.startsWith(resource.getURI())) {
                activeMatches.put(resource.getURI(), item);
            }

            items.add(item);
        }

        // Find the active menu item:
        String[] incrementalPath = URLUtil.splitUriIncrementally(currentURI);
        for (int i = incrementalPath.length - 1; i >= 0; i--) {
            String uri = incrementalPath[i];
            if (activeMatches.containsKey(uri)) {
                MenuItem activeItem = (MenuItem) activeMatches.get(uri);
                activeItem.setActive(true);
                break;
            }
        }

        
//         Collections.sort(items, new ItemComparator(parentURI, ..));

        ListMenu menu = new ListMenu();
        menu.setItems((MenuItem[]) items.toArray(new MenuItem[items.size()]));
        menu.setLabel(label);
        return menu;
    }

    public void setQueryParser(Parser queryParser) {
        this.queryParser = queryParser;
    }

    public void setViewService(Service viewService) {
        this.viewService = viewService;
    }
    
    public void setTitlePropdef(PropertyTypeDefinition titlePropdef) {
        this.titlePropdef = titlePropdef;
    }
    
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
    
    public void setSearcher(Searcher searcher) {
        this.searcher = searcher;
    }

    public void setSearchLimit(int searchLimit) {
        this.searchLimit = searchLimit;
    }
    

    protected String getDescriptionInternal() {
        return null;
    }


    protected Map getParameterDescriptionsInternal() {
        Map map = new HashMap();
        map.put(PARAMETER_URI, PARAMETER_URI_DESC);
        map.put(PARAMETER_STYLE, PARAMETER_STYLE_DESC);
        map.put(PARAMETER_INCLUDE_PARENT, PARAMETER_INCLUDE_PARENT_DESC);
        return map;
                
    }



}
