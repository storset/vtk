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
import java.util.Iterator;
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
import org.vortikal.repository.search.ConfigurablePropertySelect;
import org.vortikal.repository.search.QueryParser;
import org.vortikal.repository.search.ResultSet;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.Searcher;
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

    private static final int DEFAULT_DEPTH = 1;
    private static final int DEFAULT_SEARCH_LIMIT = 500;
    
    private static final String STYLE_NONE = "none";
    private static final String STYLE_VERTICAL = "vertical";
    private static final String STYLE_HORIZONTAL = "horizontal";
    private static final String STYLE_TABS = "tabs";
    
    private static final String DEFAULT_STYLE = STYLE_NONE;

    private static final Set<String> VALID_STYLES;
    static {
        VALID_STYLES = new HashSet<String>();
        VALID_STYLES.add(STYLE_NONE);
        VALID_STYLES.add(STYLE_VERTICAL);
        VALID_STYLES.add(STYLE_HORIZONTAL);
        VALID_STYLES.add(STYLE_TABS);
    }

    private static final String PARAMETER_INCLUDE_CHILDREN = "include-children";
    private static final String PARAMETER_INCLUDE_CHILDREN_DESC
        = "An explicit listing of the child resources to include. (Only applicable for resources at level 1.)";

    private static final String PARAMETER_EXCLUDE_CHILDREN = "exclude-children";
    private static final String PARAMETER_EXCLUDE_CHILDREN_DESC
        = "A listing of child resources to exclude (cannot be used in conjunction with '" + PARAMETER_INCLUDE_CHILDREN + "').";

    private static final String PARAMETER_INCLUDE_PARENT = "include-parent-folder";
    private static final String PARAMETER_INCLUDE_PARENT_DESC = 
        "Whether or not to include the selected folder itself in the menu. Defaults to 'false'.";

    private static final String PARAMETER_STYLE = "style";
    private static final String PARAMETER_STYLE_DESC = 
        "Defines the style of the menu. Must be one of " + VALID_STYLES.toString()
        + ". Defaults to " + DEFAULT_STYLE + ".";

    private static final String PARAMETER_URI = "uri";
    private static final String PARAMETER_URI_DESC = 
        "The URI (path) to the selected folder.";

    private static final String PARAMETER_AUTENTICATED = "authenticated";
    private static final String PARAMETER_AUTENTICATED_DESC = 
        "The default is that only resources readable for everyone is listed. " +
            "If this is set to 'true', the listing is done as the currently " +
            "logged in user (if any).";
    
    private static final String PARAMETER_DEPTH = "depth";
    private static final String PARAMETER_DEPTH_DESC =
        "Specifies the number of levels to retrieve subfolders for. The default value is '1', which retrieves the top level.";
    
    
    private static Log logger = LogFactory.getLog(ListMenuComponent.class);
    
    private QueryParser queryParser;
    private Service viewService;
    private PropertyTypeDefinition titlePropdef;
    private ResourceTypeDefinition collectionResourceType;
    private String modelName = "menu";
    private int searchLimit = DEFAULT_SEARCH_LIMIT;
    private Searcher searcher;


    public void processModel(Map<Object, Object> model, DecoratorRequest request, DecoratorResponse response)
        throws Exception {
        MenuRequest menuRequest = new MenuRequest(request);
        
        // Build main query
        Search mainsearch = buildMainSearch(menuRequest);
        ResultSet mainResultSet = this.searcher.execute(menuRequest.getToken(), mainsearch);
        ListMenu menu = buildMainMenu(mainResultSet, menuRequest);
        
        // Build sub-queries (but only if request is from proper subtree)
        int depth = menuRequest.getDepth();
        String currentURI = menuRequest.getCurrentURI();
        String uri = menuRequest.getURI();
        if (depth > 1 && !currentURI.equals(uri) && currentURI.startsWith(uri)) {
            String[] uris = URLUtil.splitUriIncrementally(currentURI);
            Search subsearch = buildSubSearch(menuRequest, uris, depth);
            if (subsearch != null) {
                ResultSet subResultSet = this.searcher.execute(menuRequest.getToken(), subsearch);
                ListMenu submenu = buildSubMenu(subResultSet, menuRequest);
                MenuItem activeItem = menu.getActiveItem();
                if (submenu != null && menu.getActiveItem() != null) { 
                    menu.getActiveItem().setSubMenu( submenu );
                }                
            }
        }
        model.put(this.modelName, menu);
    }
    
    
    private Search buildMainSearch(MenuRequest menuRequest) {
        String uri = menuRequest.getURI();
        boolean includeParent = menuRequest.isParentIncluded();
        String[] childNames = menuRequest.getChildNames();
        int startDepth = URLUtil.splitUri(uri).length;
        StringBuffer query = new StringBuffer();
        
        if (childNames == null) {
            // List all children based on depth:
            uri = escapeIllegalCharacters(uri);
            query.append("(uri = ").append(uri);
            if (!uri.equals("/")) {
                query.append("/");
            }
            // Main menu query
            query.append("* AND depth = ").append(startDepth).append(")");
            String[] excludedChildren = menuRequest.getExcludedChildren();
            if (excludedChildren != null) {
                // A list of excluded children is provided
                for (int i = 0; i < excludedChildren.length; i++) {
                    String name = excludedChildren[i];
                    // Only exclude top menu folders 
                    if (name.indexOf("/") == -1) {
                        name = name.trim();
                        name = escapeIllegalCharacters(name);
                        query.append(" AND name != ").append(name).append("");
                    }
                }
            }
        } else {
            // An explicit list of child names is provided
            for (int i = 0; i < childNames.length; i++) {
                String name = childNames[i];
                if (name.indexOf("/") != -1) {
                    throw new DecoratorComponentException("Invalid child name: '" + name + "'");
                }
                name = name.trim();
                name = escapeIllegalCharacters(name);
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
        query.insert(0, "type IN " + this.collectionResourceType.getQName() + " AND (");
        query.append(")");
        
        ConfigurablePropertySelect select = new ConfigurablePropertySelect();
        select.addPropertyDefinition(this.titlePropdef);
        if (logger.isDebugEnabled()) {
            logger.debug("About to search using query: '" + query + "'");
        }
        Search search = new Search();
        search.setSorting(null);
        search.setQuery(this.queryParser.parse(query.toString()));
        search.setLimit(this.searchLimit);
        search.setPropertySelect(select);

        return search;
    }
    
    
    private Search buildSubSearch(MenuRequest menuRequest, String[] uris, int depth) {
        int startDepth = URLUtil.splitUri(menuRequest.getURI()).length;
        StringBuilder query = new StringBuilder();
        String uri;
        int maxDepth = startDepth + depth;
        if (maxDepth > 1) {
            --maxDepth; // as 'depth = 1' is defined to only list contents of current folder
        }
        
        for (int i = startDepth; i < uris.length && i < maxDepth; i++) {
            uri = uris[i];
            // Must escape space and parentheses from file/folder names to build proper query string 
            uri = escapeIllegalCharacters(uri);
            
            if (i != startDepth) {
                query.append(" OR ");
            }
            query.append("(uri = ").append(uri);
            query.append(" OR (uri = ").append(uri);
            if (!uri.equals("/")) {
                query.append("/");
            }
            query.append("* AND depth = ").append(i+1).append(")"); // query subtree below current depth
            query.append(")");
        }
                
        String[] excludedChildren = menuRequest.getExcludedChildren();
        StringBuilder excludeQuery = new StringBuilder();
        if (excludedChildren != null) {
            // A list of excluded children is provided
            for (int i = 0; i < excludedChildren.length; i++) {
                String excludedFolder = excludedChildren[i];
                if (excludedFolder.startsWith("/")) {
                    throw new DecoratorComponentException("Parameter '" +
                             PARAMETER_EXCLUDE_CHILDREN + 
                             "' has invalid child name: '" + excludedFolder + "' " +
                             "(folder path must be relative to given 'uri', e.g: [folder,folder/subfolder])");
                }
                
                String searchRootURI = menuRequest.getURI();
                searchRootURI = escapeIllegalCharacters(searchRootURI);
                if (!searchRootURI.endsWith("/")) {
                    searchRootURI += "/";
                }
                
                excludedFolder = excludedFolder.trim();
                // Only add sub-level folders
                if (excludedFolder.indexOf('/') != -1) {
                    excludedFolder = excludedFolder.trim();
                     /*
                     * XXX: Exclude-query still doesn't work properly if folder name contains parentheses or whitespace...
                     */
                    excludedFolder = escapeIllegalCharacters(excludedFolder);
                    excludedFolder = searchRootURI + excludedFolder;
                    excludeQuery.append(" AND uri != ").append(excludedFolder)
                                .append(" AND uri != ").append(excludedFolder).append("/*").append("");
                } 
                // Check if currentURI is in excluded subtree (if so, nullify subsearch)
                else {
                    excludedFolder = escapeIllegalCharacters(excludedFolder);
                    excludedFolder = searchRootURI + excludedFolder;
                    if (menuRequest.getCurrentURI().startsWith(excludedFolder)) {
                        return null;
                    }
                }
            }
        }
                    
        query.insert(0, "type IN " + this.collectionResourceType.getQName() + " AND (");
        query.append(")").append(excludeQuery); // Safe to add even if empty 
                
        ConfigurablePropertySelect select = new ConfigurablePropertySelect();
        select.addPropertyDefinition(this.titlePropdef);
        if (logger.isDebugEnabled()) {
            logger.debug("About to search using query: '" + query + "'");
        }
        
        Search search = new Search();
        search.setSorting(null); 
        search.setQuery(this.queryParser.parse(query.toString()));
        search.setLimit(this.searchLimit);
        search.setPropertySelect(select);
        
        return search;
    }
    
    
    private ListMenu buildMainMenu(ResultSet rs, MenuRequest menuRequest) {
        String currentURI = menuRequest.getCurrentURI();        
        String[] childNames = menuRequest.getChildNames();
        
        MenuItem<String> parent = null;
        List<MenuItem<String>> items = new ArrayList<MenuItem<String>>();
        Map<String, MenuItem<String>> activeMatches = new HashMap<String, MenuItem<String>>();
        Map<String, MenuItem<String>> nameItemMap = new HashMap<String, MenuItem<String>>();
        
        for (int i = 0; i < rs.getSize(); i++) {
            PropertySet resource = rs.getResult(i);
            
            String uri = resource.getURI();
            if (!uri.equals("/")) {
                // Know it's a folder, append "/"
                uri += "/";
            }
            
            String url = this.viewService.constructLink(uri);
            String name = resource.getName().replace(' ', '-');
            if (name.equals("/")) {
                name = "root-folder";
            }
            Property titleProperty = resource.getProperty(this.titlePropdef);
            String title = titleProperty != null ? titleProperty.getStringValue() : name;
            
            MenuItem<String> item = new MenuItem<String>();
            item.setUrl(url);
            item.setTitle(title);
            item.setLabel(name);
            item.setActive(false);
            
            if (currentURI.startsWith(resource.getURI())) {
                activeMatches.put(resource.getURI(), item);
                
            }
            if (resource.getURI().equals(menuRequest.getURI())) {
                item.setLabel(name + " parent-folder");
                parent = item;
            } else {
                nameItemMap.put(resource.getName(), item);
                items.add(item);
            }
        }
        
        // Find the active menu item:
        String[] incrementalPath = URLUtil.splitUriIncrementally(currentURI);
        MenuItem activeItem = null;
        for (int i = incrementalPath.length - 1; i >= 0; i--) {
            String uri = incrementalPath[i];
            if (activeMatches.containsKey(uri)) {
                activeItem = activeMatches.get(uri);
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

        ListMenu<String> menu = new ListMenu<String>();
        menu.addAllItems(items);
        menu.setLabel(menuRequest.getStyle());
        if(activeItem!=null)
            menu.setActiveItem(activeItem);
        return menu;
    }
    
    
    private List<MenuItem<String>> sortSpecifiedOrder(String[] childNames, Map<String, MenuItem<String>> nameItemMap) {
        List<MenuItem<String>> result = new ArrayList<MenuItem<String>>();
        for (int i = 0; i < childNames.length; i++) {
            String name = childNames[i].trim();
            MenuItem<String> item = nameItemMap.get(name);
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }
    
    private List<MenuItem<String>> sortRegularOrder(List<MenuItem<String>> items, Locale locale) {
        Collections.sort(items, new ItemTitleComparator(locale));
        return items;
    }
    
    
    private ListMenu buildSubMenu(ResultSet rs, MenuRequest menuRequest) {
        String requestURI = menuRequest.getCurrentURI();
        Map<String, List<PropertySet>> childMap = new HashMap<String, List<PropertySet>>();
        List<PropertySet> childList = new ArrayList<PropertySet>();
                        
        String rootURI = null;
        PropertySet rootResource = null;
        
        for (int i = 0; i < rs.getSize(); i++) {
            PropertySet resource = rs.getResult(i);
            String uri = resource.getURI();
            String parentURI = URIUtil.getParentURI(uri);
            
            if(rootURI == null) {
                rootURI = uri;
                rootResource = resource;
            } else {
                if (URLUtil.splitUri(uri).length < URLUtil.splitUri(rootURI).length) {
                    rootURI = uri;
                    rootResource = resource;
                }
            }
                        
            if (!childMap.containsKey(parentURI)) {
                childMap.put(parentURI, new ArrayList<PropertySet>());
            }
            childList = childMap.get(parentURI);
            if (childList == null) {
                childList = new ArrayList<PropertySet>();
                childMap.put(parentURI, childList);
            }
            childList.add(resource);
        }
        
        if(childMap.isEmpty()) {
            return null;
        }
        else {
            return buildSubItems(rootResource, childMap, requestURI, menuRequest.getLocale());
        }
    }
    
    
    private ListMenu buildSubItems(PropertySet rootResource, Map<String, List<PropertySet>> childMap, String requestURI, Locale locale) {
        List<MenuItem<String>> items = new ArrayList<MenuItem<String>>();
        List<PropertySet> childList = childMap.get(rootResource.getURI());
        // if current-child is excluded etc.
        if(childList == null) {
            return null;
        }
        for (Iterator iterator = childList.iterator(); iterator.hasNext();) {
            PropertySet subResource = (PropertySet) iterator.next();
            MenuItem<String> item = buildItem(subResource, childMap);
            if (childMap.containsKey(subResource.getURI())) {
                item.setSubMenu(buildSubItems(subResource, childMap, requestURI, locale));
                item.setActive(true);
            } else {
                // Necessary to set active the deepest expanded folder for the current subtree
                // (which will have no pointer to a child list)
                if (requestURI.startsWith(subResource.getURI())) {
                  item.setActive(true);
                }
            }
            items.add(item);
        }
        
        /*
        // Sort children:
        if (childNames != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Sorting items based on specified order: "
                             + java.util.Arrays.asList(childNames));
            }
            items = sortSpecifiedOrder(childNames, nameItemMap);
        }
        */
        /*
         * XXX: Only default sort for sub-menu 
         */
        if (logger.isDebugEnabled()) {
            logger.debug("Sorting items based on title");
        }
        items = sortRegularOrder(items, locale);
        
        ListMenu<String> submenu = new ListMenu<String>();
        submenu.addAllItems(items);
        //submenu.setLabel(rootResource.getName().replace(' ', '-')); // no need to CSS-class sub-UL 
        return submenu;
    }
    
        
    private MenuItem<String> buildItem(PropertySet resource, Map<String, List<PropertySet>> childMap) {
        MenuItem<String> item = new MenuItem<String>();
        String uri = resource.getURI();
        if (!uri.equals("/")) {
            // Know it's a folder, append "/"
            uri += "/";
        }
        String url = this.viewService.constructLink(uri);
        String name = resource.getName().replace(' ', '-');
        Property titleProperty = resource.getProperty(this.titlePropdef);
        String title = titleProperty != null ? titleProperty.getStringValue() : name;
        item.setUrl(url);
        item.setTitle(title);
        item.setLabel(name);
        item.setActive(false);
        return item;
    }
    
    
    // Helper method
    private String escapeIllegalCharacters(String s) {
        return s.replace(" ", "\\ ").replace("(", "\\(").replace(")", "\\)");
    }
    

    private class MenuRequest {
        private String uri;
        private String currentURI;
        private boolean parentIncluded;
        private String[] childNames;
        private String style;
        private int depth = DEFAULT_DEPTH;
        private Locale locale;
        private String token;
        private String[] excludedChildren;
        
        public MenuRequest(DecoratorRequest request) {
            String uri = request.getStringParameter(PARAMETER_URI);
            if (uri == null) {
                throw new DecoratorComponentException("Parameter 'uri' not specified");
            }
            this.uri = uri;

            boolean authenticated = "true".equals(
                request.getStringParameter(PARAMETER_AUTENTICATED));
            if (authenticated) {
                SecurityContext securityContext = SecurityContext.getSecurityContext();
                this.token = securityContext.getToken();
            }

            this.style = request.getStringParameter(PARAMETER_STYLE);
            if (this.style == null) {
                this.style = DEFAULT_STYLE;
            } else if (!VALID_STYLES.contains(this.style)) {
                throw new DecoratorComponentException(
                        "Invalid value for parameter 'style': must be one of "
                        + VALID_STYLES.toString());
            }

            this.parentIncluded = "true".equals(request.getStringParameter(
                                                    PARAMETER_INCLUDE_PARENT));

            String includeChildrenParam = request.getStringParameter(PARAMETER_INCLUDE_CHILDREN);
            if (includeChildrenParam != null) {
                childNames = includeChildrenParam.split(",");
                if (childNames.length == 0) {
                    throw new DecoratorComponentException(
                        "Invalid value for parameter '" + PARAMETER_INCLUDE_CHILDREN
                        + "': must provide at least one child name");
                }
            }
            String excludeChildrenParam = request.getStringParameter(PARAMETER_EXCLUDE_CHILDREN);
            if (excludeChildrenParam != null) {
                if (this.childNames != null) {
                    throw new DecoratorComponentException(
                            "Cannot use both parameters '" + PARAMETER_INCLUDE_CHILDREN
                            + "' and '" + PARAMETER_EXCLUDE_CHILDREN + "'");
                }
                this.excludedChildren = excludeChildrenParam.split(",");
                if (this.excludedChildren.length == 0) {
                    throw new DecoratorComponentException(
                        "Invalid value for parameter '" + PARAMETER_EXCLUDE_CHILDREN
                        + "': must provide at least one child name");
                }
            }
            String depthStr = request.getStringParameter(PARAMETER_DEPTH);
            if (depthStr != null) {
                try {
                    int depth = Integer.parseInt(depthStr);
                    if (depth < 1) {
                        throw new IllegalArgumentException("Depth must be an integer >= 1");
                    }
                    this.depth = depth;
                } catch (Throwable t) {
                    throw new DecoratorComponentException(
                        "Illegal value for parameter '" + PARAMETER_DEPTH + "': "
                        + depthStr);
                }
            }
            RequestContext requestContext = RequestContext.getRequestContext();
            this.currentURI = requestContext.getResourceURI();
            this.locale = request.getLocale();
        }
        
        public String[] getExcludedChildren() {
            return this.excludedChildren;
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

        public int getDepth() {
            return this.depth;
        }
        
        public Locale getLocale() {
            return this.locale;
        }

        public String getToken() {
            return token;
        }
    }


    private class ItemTitleComparator implements Comparator<MenuItem<String>> {

        private Collator collator;

        public ItemTitleComparator(Locale locale) {
            this.collator = Collator.getInstance(locale);
        }
        

        public int compare(MenuItem<String> i1, MenuItem<String>i2) {
            return this.collator.compare(i1.getTitle(), i2.getTitle());
        }
    }
    

    public void setQueryParser(QueryParser queryParser) {
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
    

    protected Map<String, String> getParameterDescriptionsInternal() {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(PARAMETER_URI, PARAMETER_URI_DESC);
        map.put(PARAMETER_STYLE, PARAMETER_STYLE_DESC);
        map.put(PARAMETER_INCLUDE_CHILDREN, PARAMETER_INCLUDE_CHILDREN_DESC);
        map.put(PARAMETER_EXCLUDE_CHILDREN, PARAMETER_EXCLUDE_CHILDREN_DESC);
        map.put(PARAMETER_INCLUDE_PARENT, PARAMETER_INCLUDE_PARENT_DESC);
        map.put(PARAMETER_AUTENTICATED, PARAMETER_AUTENTICATED_DESC);
        map.put(PARAMETER_DEPTH, PARAMETER_DEPTH_DESC);
        return map;
    }
}
