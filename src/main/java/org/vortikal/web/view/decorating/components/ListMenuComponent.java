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

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanInitializationException;

import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import org.vortikal.repository.search.HashSetPropertySelect;
import org.vortikal.repository.search.ResultSet;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.Searcher;
import org.vortikal.repository.search.query.Parser;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.repository.URIUtil;
import org.vortikal.util.web.URLUtil;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.view.components.menu.ListMenu;
import org.vortikal.web.view.components.menu.MenuItem;
import org.vortikal.web.view.decorating.DecoratorRequest;
import org.vortikal.web.view.decorating.DecoratorResponse;


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
    private static final String PARAMETER_INCLUDE_CHILDREN_DESC
        = "An explicit listing of the child resources to include.";

    private static final String PARAMETER_INCLUDE_PARENT = "includeParentFolder";
    private static final String PARAMETER_INCLUDE_PARENT_DESC = 
        "Whether or not to include the selected folder itself in the menu. Defaults to 'false'";

    private static final String PARAMETER_STYLE = "style";
    private static final String PARAMETER_STYLE_DESC = 
        "Defines the style of the menu. Must be one of " + VALID_STYLES.toString()
        + ". Defaults to " + DEFAULT_STYLE;

    private static final String PARAMETER_URI = "uri";
    private static final String PARAMETER_URI_DESC = 
        "The URI (path) to the selected folder";

    private static final String PARAMETER_AS_CURRENT_USER = "authenticated";
    private static final String PARAMETER_AS_CURRENT_USER_DESC = 
        "The default is that only resources readable for everyone is listed. " +
            "If this is set to 'true', the listing is done as the currently " +
            "logged in user (if any)";

    private static Log logger = LogFactory.getLog(ListMenuComponent.class);
    
    private Parser queryParser;
    private Service viewService;
    private PropertyTypeDefinition titlePropdef;
    private ResourceTypeDefinition collectionResourceType;
    private String modelName = "menu";
    private int searchLimit = 10;
    private Searcher searcher;


    public void processModel(Map model, DecoratorRequest request, DecoratorResponse response)
        throws Exception {
        MenuRequest menuRequest = new MenuRequest(request);
        Search search = buildSearch(menuRequest);
        String token = null;
        ResultSet rs = this.searcher.execute(token, search);
        ListMenu menu = buildListMenu(rs, menuRequest);
        model.put(this.modelName, menu);
    }
    

    private Search buildSearch(MenuRequest menuRequest) {
        String uri = menuRequest.getURI();
        boolean includeParent = menuRequest.isParentIncluded();
        String[] childNames = menuRequest.getChildNames();
        int depth = URLUtil.splitUri(uri).length;
        StringBuffer query = new StringBuffer();

        if (childNames == null) {
            // List all children based on depth:
            query.append("(uri = ").append(uri).append("/*");
            query.append(" AND depth = ").append(depth).append(")");

        } else {
            // An explicit list of child names is provided
            for (int i = 0; i < childNames.length; i++) {
                String name = childNames[i];
                if (name.indexOf("/") != -1) {
                    throw new DecoratorComponentException("Invalid child name: '" + name + "'");
                }
                name = name.trim();
                // XXX: need to escape white space in names:
                //name = name.replaceAll(" ", "\\\\ ");
                String childURI = URIUtil.makeAbsoluteURI(name, uri);
                query.append("uri = ").append(childURI).append("");
                if (i < childNames.length - 1) {
                    query.append(" OR ");
                }
            }
        }
        if (includeParent) {
            query.insert(0, "(");
            query.append(")");
            query.append(" OR uri = ").append(uri);
        }
        query.insert(0, "type IN " + this.collectionResourceType.getName() + " AND (");
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
    

    private ListMenu buildListMenu(ResultSet rs, MenuRequest menuRequest) {
        
        String currentURI = menuRequest.getCurrentURI();        
        String[] childNames = menuRequest.getChildNames();

        MenuItem parent = null;
        List items = new ArrayList();
        Map activeMatches = new HashMap();
        Map nameItemMap = new HashMap();

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
            if (resource.getURI().equals(menuRequest.getURI())) {
                parent = item;
            } else {
                nameItemMap.put(resource.getName(), item);
                items.add(item);
            }
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

        // Sort children:
        if (childNames != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Sorting items based on specified order: "
                             + java.util.Arrays.asList(childNames));
            }
            items = sortSpecifiedOrder(childNames, nameItemMap);
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Sorting items based on title");
            }
            items = sortRegularOrder(items, menuRequest.getLocale());
        }

        // Insert parent first in list if exists:
        if (parent != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Found parent item: " + parent);
            }

            items.add(0, parent);
        }

        ListMenu menu = new ListMenu();
        menu.setItems((MenuItem[]) items.toArray(new MenuItem[items.size()]));
        menu.setLabel(menuRequest.getStyle());
        return menu;
    }


    private List sortSpecifiedOrder(String[] childNames, Map nameItemMap) {
        List result = new ArrayList();
        for (int i = 0; i < childNames.length; i++) {
            String name = childNames[i].trim();
            MenuItem item = (MenuItem) nameItemMap.get(name);
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }
    
    private List sortRegularOrder(List items, Locale locale) {
        Collections.sort(items, new ItemTitleComparator(locale));
        return items;
    }
    

    private class MenuRequest {
        private String uri;
        private String currentURI;
        private boolean parentIncluded;
        private String[] childNames;
        private String style;
        private Locale locale;
        private String token;
        
        public MenuRequest(DecoratorRequest request) {
            String uri = request.getStringParameter(PARAMETER_URI);
            if (uri == null) {
                throw new DecoratorComponentException("Parameter 'uri' not specified");
            }
            this.uri = uri;

            boolean asCurrentUser = "true".equals(
                request.getStringParameter(PARAMETER_AS_CURRENT_USER));
            if (asCurrentUser) {
                SecurityContext securityContext = SecurityContext.getSecurityContext();
                this.token = securityContext.getToken();
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
            this.style = style;

            this.parentIncluded = "true".equals(request.getStringParameter(
                                                    PARAMETER_INCLUDE_PARENT));
            String[] splitChildNames = null;
            String includeChildrenParam = request.getStringParameter(PARAMETER_INCLUDE_CHILDREN);
            if (includeChildrenParam != null) {
                splitChildNames = includeChildrenParam.split(",");
                if (splitChildNames.length == 0) {
                    throw new DecoratorComponentException(
                        "Invalid value for parameter '" + PARAMETER_INCLUDE_CHILDREN
                        + "': must provide at least one child name");
                }
            }
            this.childNames = splitChildNames;
            RequestContext requestContext = RequestContext.getRequestContext();
            this.currentURI = requestContext.getResourceURI();
            this.locale = request.getLocale();
        }
        
        public String getURI() {
            return this.uri;
        }
        
        public String getCurrentURI() {
            return this.currentURI;
        }
        
        public boolean isParentIncluded() {
            return this.parentIncluded;
        }
        
        public String[] getChildNames() {
            return this.childNames;
        }
        
        public String getStyle() {
            return this.style;
        }

        public Locale getLocale() {
            return this.locale;
        }
    }


    private class ItemTitleComparator implements Comparator {

        private Collator collator;

        public ItemTitleComparator(Locale locale) {
            this.collator = Collator.getInstance(locale);
        }
        

        public int compare(Object o1, Object o2) {
            MenuItem i1 = (MenuItem) o1;
            MenuItem i2 = (MenuItem) o2;
            return this.collator.compare(i1.getTitle(), i2.getTitle());
        }
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
    
    public void setCollectionResourceType(ResourceTypeDefinition collectionResourceType) {
        this.collectionResourceType = collectionResourceType;
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


    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        if (this.queryParser == null) {
            throw new BeanInitializationException(
                "JavaBean property '" + queryParser + "' not set");
        }
        if (this.viewService == null) {
            throw new BeanInitializationException(
                "JavaBean property '" + viewService + "' not set");
        }
        if (this.titlePropdef == null) {
            throw new BeanInitializationException(
                "JavaBean property '" + titlePropdef + "' not set");
        }
        if (this.collectionResourceType == null) {
            throw new BeanInitializationException(
                "JavaBean property '" + collectionResourceType + "' not set");
        }
        if (this.modelName == null) {
            throw new BeanInitializationException(
                "JavaBean property '" + modelName + "' not set");
        }
        if (this.searcher == null) {
            throw new BeanInitializationException(
                "JavaBean property '" + searcher + "' not set");
        }
        if (this.searchLimit <= 0) {
            throw new BeanInitializationException(
                "JavaBean property '" + searchLimit + "' must be a positive integer");
        }

    }
    

    protected Map getParameterDescriptionsInternal() {
        Map map = new LinkedHashMap();
        map.put(PARAMETER_URI, PARAMETER_URI_DESC);
        map.put(PARAMETER_STYLE, PARAMETER_STYLE_DESC);
        map.put(PARAMETER_INCLUDE_CHILDREN, PARAMETER_INCLUDE_CHILDREN_DESC);
        map.put(PARAMETER_INCLUDE_PARENT, PARAMETER_INCLUDE_PARENT_DESC);
        map.put(PARAMETER_AS_CURRENT_USER, PARAMETER_AS_CURRENT_USER_DESC);
        return map;
                
    }
}
