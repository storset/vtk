/* Copyright (c) 2007, 2008, University of Oslo, Norway
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.search.ResultSet;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.WildcardPropertySelect;
import org.vortikal.repository.search.query.AndQuery;
import org.vortikal.repository.search.query.OrQuery;
import org.vortikal.repository.search.query.TermOperator;
import org.vortikal.repository.search.query.TypeTermQuery;
import org.vortikal.repository.search.query.UriDepthQuery;
import org.vortikal.repository.search.query.UriPrefixQuery;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.decorating.DecoratorRequest;
import org.vortikal.web.decorating.DecoratorResponse;
import org.vortikal.web.service.URL;
import org.vortikal.web.view.components.menu.ListMenu;
import org.vortikal.web.view.components.menu.MenuItem;

/**
 * <p>
 * The following input is evaluated
 * </p>
 * <ul>
 * <li>title - menu title</li>
 * <li>sort - the property to sort results by</li>
 * <li>direction - the sort direction (ascending / descending)</li>
 * <li>result-sets - the number of &lt;ul&gt; lists to split the result into</li>
 * <li>group-result-sets-by - the number of results-sets in grouping divs</li>
 * <li>freeze-at-level - at which level the subfolder-listing should freeze</li>
 * <li>exclude-folders - comma-separated list with relative paths to folders
 * which should be excluded</li>
 * <li>authenticated - default is listing only read-for-all resources</li>
 * <li>depth - specifies number of levels to retrieve subfolders from</li>
 * </ul>
 */
public class SubFolderMenuComponent extends ListMenuComponent {

    private static final String DESCRIPTION = "Lists the child folders of the current folder";

    private static final String PARAMETER_TITLE = "title";
    private static final String PARAMETER_TITLE_DESC = "The menu title";

    private static final String PARAMETER_SORT = "sort";
    private static final String PARAMETER_SORT_DESC = "The name of a property to sort results by. Legal values are ('name', 'title'). "
            + "The default property is 'title'";

    private static final String PARAMETER_SORT_DIRECTION = "direction";
    private static final String PARAMETER_SORT_DIRECTION_DESC = "The sort direction. Legal values are 'asc', 'desc'. The default value is 'asc' ";

    private static final String PARAMETER_RESULT_SETS = "result-sets";
    private static final String PARAMETER_RESULT_SETS_DESC = "The number of result sets to split the result into. The default value is '1'";
    private static final int PARAMETER_RESULT_SETS_MAX_VALUE = 30;

    private static final String PARAMETER_GROUP_RESULT_SETS_BY = "group-result-sets-by";
    private static final String PARAMETER_GROUP_RESULT_SETS_BY_DESC = "The number of results-sets in grouping divs";
    private static final int PARAMETER_GROUP_RESULT_SETS_BY_MAX_VALUE = 30;

    private static final String PARAMETER_FREEZE_AT_LEVEL = "freeze-at-level";
    private static final String PARAMETER_FREEZE_AT_LEVEL_DESC = "At which level the subfolder-listing should freeze and show the same listing further down. The default is none.";

    private static final String PARAMETER_EXCLUDE_FOLDERS = "exclude-folders";
    private static final String PARAMETER_EXCLUDE_FOLDERS_DESC = "Commma-separated list with relative paths to folders which should not be displayed in the list";

    private static final String PARAMETER_URI = "uri";
    private static final String PARAMETER_URI_DESC = "The URI (path) to the selected folder.";

    private static final String PARAMETER_AS_CURRENT_USER = "authenticated";
    private static final String PARAMETER_AS_CURRENT_USER_DESC = "The default is that only resources readable for everyone is listed. "
            + "If this is set to 'true', the listing is done as the currently " + "logged in user (if any)";

    private static final String PARAMETER_DEPTH = "depth";
    private static final String PARAMETER_DEPTH_DESC = "Specifies the number of levels to retrieve subfolders for. The default value is '1' ";

    private static final String PARAMETER_DISPLAY_FROM_LEVEL = "display-from-level";
    private static final String PARAMETER_DISPLAY_FROM_LEVEL_DESC = "Defines the starting URI level for the subfolder-menu";

    private static final String PARAMETER_MAX_NUMBER_OF_CHILDREN = "max-number-of-children";
    private static final String PARAMETER_MAX_NUMBER_OF_CHILDREN_DESC = "Defines the maximum number of children displayed for each element";

    private static Log logger = LogFactory.getLog(SubFolderMenuComponent.class);

    private ResourceTypeTree resourceTypeTree;

    public void processModel(Map<Object, Object> model, DecoratorRequest request, DecoratorResponse response)
            throws Exception {
        MenuRequest menuRequest = new MenuRequest(request);

        int currentLevel = menuRequest.getCurrentCollectionUri().getDepth() + 1;

        if (menuRequest.getDisplayFromLevel() != -1) {
            if (currentLevel < menuRequest.getDisplayFromLevel()) {
                return;
            }
        }

        Search search = buildSearch(menuRequest);
        String token = menuRequest.getToken();
        ResultSet rs = this.repository.search(token, search);
        if (logger.isDebugEnabled()) {
            logger.debug("Executed search: " + search + ", hits: " + rs.getSize());
        }
        ListMenu<PropertySet> menu = buildListMenu(rs, menuRequest);
        Map<String, Object> menuModel = buildMenuModel(menu, menuRequest);
        model.put(this.modelName, menuModel);
        if (logger.isDebugEnabled()) {
            logger.debug("Built model: " + model + " from menu: " + menu);
        }
    }

    private Map<String, Object> buildMenuModel(ListMenu<PropertySet> menu, MenuRequest menuRequest) {
        List<ListMenu<PropertySet>> resultList = new ArrayList<ListMenu<PropertySet>>();

        int resultSets = menuRequest.getResultSets();
        List<MenuItem<PropertySet>> allItems = menu.getItemsSorted();

        int groupResultSetsBy = menuRequest.getGroupResultSetsBy();

        if (resultSets > allItems.size()) {
            resultSets = allItems.size();
        }

        int itemsPerResultSet = Math.round((float) allItems.size() / (float) resultSets);
        int remainder = allItems.size() - (resultSets * itemsPerResultSet);

        // Moving startIdx and endIdx when remainder > 0
        int startMov = 0;
        int endMov = 0;

        boolean lastOne = false;

        for (int i = 0; i < resultSets; i++) {
            int startIdx = i * itemsPerResultSet;
            int endIdx = startIdx + itemsPerResultSet;

            /*
             * Old code for remainder, places them last if (i == resultSets - 1
             * && remainder > 0) { endIdx += remainder; }
             */

            if (endIdx > allItems.size()) {
                endIdx = allItems.size();
            } else {

                if (lastOne == true) {
                    startMov++;
                    lastOne = false;
                }

                if (remainder > 0) {

                    if (i > 0) {
                        startMov++;
                    }

                    endMov++;
                    remainder--;

                    if (remainder == 0) {
                        lastOne = true;
                    }

                }

                startIdx = startIdx + startMov;
                endIdx = endIdx + endMov;

            }

            List<MenuItem<PropertySet>> subList = allItems.subList(startIdx, endIdx);
            ListMenu<PropertySet> m = new ListMenu<PropertySet>();
            m.setComparator(new ListMenuComparator(menuRequest.getLocale(), menuRequest.getImportancePropDef(),
                    this.navigationTitlePropDef));
            m.setTitle(menu.getTitle());
            m.setLabel(menu.getLabel());
            m.addAllItems(subList);
            resultList.add(m);
        }

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("resultSets", resultList);
        if (groupResultSetsBy > 0) {
            model.put("groupResultSetsBy", groupResultSetsBy);
        }
        model.put("size", new Integer(menu.getItems().size()));
        model.put("title", menu.getTitle());
        return model;
    }

    private Search buildSearch(MenuRequest menuRequest) {

        Path uri = menuRequest.getCurrentCollectionUri();

        int depth = uri.getDepth() + 1;

        AndQuery mainQuery = new AndQuery();
        mainQuery.add(new UriPrefixQuery(uri.toString()));

        if (menuRequest.getDepth() > 1) {
            // Needs search support for this:
            // query.add(new UriDepthTermQuery).append(menuRequest.getDepth(),
            // TermOperator.GT);
            OrQuery depthQuery = new OrQuery();
            for (int i = 0; i < menuRequest.getDepth(); i++) {
                depthQuery.add(new UriDepthQuery(depth + i));
            }
            mainQuery.add(depthQuery);
        } else {
            mainQuery.add(new UriDepthQuery(depth));
        }

        /**
         * TODO: Maybe add inversion filter for whole set rather than and'ing a
         * ton of inverted queries?
         */
        if (menuRequest.getExcludeURIs() != null && !menuRequest.getExcludeURIs().isEmpty()) {
            for (Iterator<Path> i = menuRequest.getExcludeURIs().iterator(); i.hasNext();) {
                Path exUri = i.next();
                mainQuery.add(new UriPrefixQuery(exUri.toString(), true));
            }
        }

        mainQuery.add(new TypeTermQuery(this.collectionResourceType.getName(), TermOperator.IN));

        WildcardPropertySelect select = new WildcardPropertySelect();

        Search search = new Search();
        search.setQuery(mainQuery);
        search.setLimit(this.searchLimit);
        search.setPropertySelect(select);
        return search;
    }

    private ListMenu<PropertySet> buildListMenu(ResultSet rs, MenuRequest menuRequest) {
        ListMenu<PropertySet> menu = new ListMenu<PropertySet>();
        Map<Path, List<PropertySet>> childMap = new HashMap<Path, List<PropertySet>>();
        List<PropertySet> toplevel = new ArrayList<PropertySet>();
        for (int i = 0; i < rs.getSize(); i++) {
            PropertySet resource = rs.getResult(i);

            // Hidden?
            if (this.hiddenPropDef != null && resource.getProperty(this.hiddenPropDef) != null) {
                continue;
            }

            Path parentURI = resource.getURI().getParent();
            if (parentURI.equals(menuRequest.getCurrentCollectionUri())) {
                toplevel.add(resource);
            }
            List<PropertySet> childList = childMap.get(parentURI);

            if (childList == null) {
                childList = new ArrayList<PropertySet>();
                childMap.put(parentURI, childList);
            }
            childList.add(resource);
        }

        List<MenuItem<PropertySet>> toplevelItems = new ArrayList<MenuItem<PropertySet>>();

        for (PropertySet resource : toplevel) {
            toplevelItems.add(buildItem(resource, childMap, menuRequest));
        }

        menu.setComparator(new ListMenuComparator(menuRequest.getLocale(), menuRequest.getImportancePropDef(),
                this.navigationTitlePropDef, menuRequest.isAscendingSort(), menuRequest.isSortByName(), menuRequest
                        .getSortProperty()));

        menu.addAllItems(toplevelItems);
        menu.setTitle(menuRequest.getTitle());
        menu.setLabel(this.modelName);
        return menu;
    }

    private MenuItem<PropertySet> buildItem(PropertySet resource, Map<Path, List<PropertySet>> childMap,
            MenuRequest menuRequest) {
        Path uri = resource.getURI();
        URL url = this.viewService.constructURL(uri);
        url.setCollection(true);

        Property titleProperty = resource.getProperty(this.navigationTitlePropDef);
        titleProperty = titleProperty == null ? resource.getProperty(this.titlePropDef) : titleProperty;
        Value title = titleProperty != null ? titleProperty.getValue() : new Value(resource.getName(),
                PropertyType.Type.STRING);

        MenuItem<PropertySet> item = new MenuItem<PropertySet>(resource);
        item.setUrl(url);
        item.setTitle(titleProperty.getFormattedValue());
        item.setLabel(title.getStringValue());
        item.setActive(false);

        List<PropertySet> children = childMap.get(resource.getURI());
        if (children != null) {
            ListMenu<PropertySet> subMenu = new ListMenu<PropertySet>();

            subMenu.setComparator(new ListMenuComparator(menuRequest.getLocale(), menuRequest.getImportancePropDef(),
                    this.navigationTitlePropDef, menuRequest.isAscendingSort(), menuRequest.isSortByName(),
                    menuRequest.getSortProperty()));

            for (PropertySet child : children) {
                subMenu.addItem(buildItem(child, childMap, menuRequest));
            }

            URL moreUrl = this.viewService.constructURL(resource.getURI());
            subMenu.setMoreUrl(moreUrl);
            subMenu.setMaxNumberOfItems(menuRequest.getMaxNumberOfChildren());
            item.setSubMenu(subMenu);
        }
        return item;
    }

    private class MenuRequest {

        private Path currentCollectionUri;
        private String title;
        private PropertyTypeDefinition sortProperty;
        private boolean ascendingSort = true;
        private boolean sortByName = false;
        private int resultSets = 1;
        private int groupResultSetsBy = 0;
        private int freezeAtLevel = 0;
        private int depth = 1;
        private int displayFromLevel = -1;
        private int maxNumberOfChildren = Integer.MAX_VALUE;
        private ArrayList<Path> excludeURIs;
        private Locale locale;
        private String token;

        public MenuRequest(DecoratorRequest request) {

            RequestContext requestContext = RequestContext.getRequestContext();
            this.currentCollectionUri = requestContext.getCurrentCollection();

            boolean asCurrentUser = "true".equals(request.getStringParameter(PARAMETER_AS_CURRENT_USER));
            if (asCurrentUser) {
                SecurityContext securityContext = SecurityContext.getSecurityContext();
                this.token = securityContext.getToken();
            }

            this.title = request.getStringParameter(PARAMETER_TITLE);

            initSortField(request);

            if (request.getStringParameter(PARAMETER_RESULT_SETS) != null) {
                try {
                    this.resultSets = Integer.parseInt(request.getStringParameter(PARAMETER_RESULT_SETS));
                } catch (Throwable t) {
                    throw new DecoratorComponentException("Illegal value for parameter '" + PARAMETER_RESULT_SETS
                            + "': " + request.getStringParameter(PARAMETER_RESULT_SETS));
                }
                if (this.resultSets <= 0 || this.resultSets > PARAMETER_RESULT_SETS_MAX_VALUE) {
                    throw new DecoratorComponentException("Illegal value for parameter '" + PARAMETER_RESULT_SETS
                            + "': " + this.resultSets + ": must be a positive number between 1 and "
                            + PARAMETER_RESULT_SETS_MAX_VALUE);
                }
            }

            if (request.getStringParameter(PARAMETER_RESULT_SETS) != null) {
                try {
                    this.resultSets = Integer.parseInt(request.getStringParameter(PARAMETER_RESULT_SETS));
                } catch (Throwable t) {
                    throw new DecoratorComponentException("Illegal value for parameter '" + PARAMETER_RESULT_SETS
                            + "': " + request.getStringParameter(PARAMETER_RESULT_SETS));
                }
                if (this.resultSets <= 0 || this.resultSets > PARAMETER_RESULT_SETS_MAX_VALUE) {
                    throw new DecoratorComponentException("Illegal value for parameter '" + PARAMETER_RESULT_SETS
                            + "': " + this.resultSets + ": must be a positive number between 1 and "
                            + PARAMETER_RESULT_SETS_MAX_VALUE);
                }
            }

            if (request.getStringParameter(PARAMETER_GROUP_RESULT_SETS_BY) != null) {
                try {
                    this.groupResultSetsBy = Integer.parseInt(request
                            .getStringParameter(PARAMETER_GROUP_RESULT_SETS_BY));
                } catch (Throwable t) {
                    throw new DecoratorComponentException("Illegal value for parameter '"
                            + PARAMETER_GROUP_RESULT_SETS_BY + "': "
                            + request.getStringParameter(PARAMETER_GROUP_RESULT_SETS_BY));
                }
                if (this.groupResultSetsBy <= 0 || this.groupResultSetsBy > PARAMETER_GROUP_RESULT_SETS_BY_MAX_VALUE) {
                    throw new DecoratorComponentException("Illegal value for parameter '"
                            + PARAMETER_GROUP_RESULT_SETS_BY + "': " + this.groupResultSetsBy
                            + ": must be a positive number between 1 and " + PARAMETER_GROUP_RESULT_SETS_BY_MAX_VALUE);
                }
            }

            String uri = request.getStringParameter(PARAMETER_URI);
            String displayFromLevel = request.getStringParameter(PARAMETER_DISPLAY_FROM_LEVEL);

            if ((uri != null && !"".equals(uri)) && (displayFromLevel != null && !"".equals(displayFromLevel))) {
                throw new DecoratorComponentException("At most one of parameters '" + PARAMETER_URI + "' or '"
                        + PARAMETER_DISPLAY_FROM_LEVEL + "' can be specified");
            }

            if (uri != null && !"".equals(uri.trim())) {
                try {
                    if (uri.startsWith("http://") || uri.startsWith("https://")) {

                    } else {
                        if (!"/".equals(uri) && uri.endsWith("/")) {
                            uri = uri.substring(0, uri.length() - 1);
                        }
                    }

                    this.currentCollectionUri = Path.fromString(uri); // override
                } catch (Throwable t) {
                    throw new DecoratorComponentException("Illegal value for parameter '" + PARAMETER_URI + "': "
                            + request.getStringParameter(PARAMETER_URI));
                }
            }

            try {
                setMaxNumberOfChildren(Integer.parseInt(request.getStringParameter(PARAMETER_MAX_NUMBER_OF_CHILDREN)));
            } catch (NumberFormatException e) {
                // Not a required parameter
            }

            if (displayFromLevel != null && !"".equals(displayFromLevel.trim())) {
                try {
                    this.displayFromLevel = Integer.parseInt(displayFromLevel);
                } catch (Throwable t) {
                    throw new DecoratorComponentException("Illegal value for parameter '"
                            + PARAMETER_DISPLAY_FROM_LEVEL + "': "
                            + request.getStringParameter(PARAMETER_DISPLAY_FROM_LEVEL));
                }

                if (this.displayFromLevel <= 0) {
                    throw new DecoratorComponentException("Illegal value for parameter '"
                            + PARAMETER_DISPLAY_FROM_LEVEL + "': " + this.displayFromLevel
                            + ": must be a positive number between larger than 0");
                }

                if (this.displayFromLevel <= currentCollectionUri.getPaths().size()) {
                    this.currentCollectionUri = currentCollectionUri.getPaths().get(this.displayFromLevel - 1);
                }
            }

            if (request.getStringParameter(PARAMETER_FREEZE_AT_LEVEL) != null) {
                try {
                    this.freezeAtLevel = Integer.parseInt(request.getStringParameter(PARAMETER_FREEZE_AT_LEVEL));
                } catch (Throwable t) {
                    throw new DecoratorComponentException("Illegal value for parameter '" + PARAMETER_FREEZE_AT_LEVEL
                            + "': " + request.getStringParameter(PARAMETER_FREEZE_AT_LEVEL));
                }
                if (this.freezeAtLevel <= 0) {
                    throw new DecoratorComponentException("Illegal value for parameter '" + PARAMETER_FREEZE_AT_LEVEL
                            + "': " + this.freezeAtLevel + ": must be a positive number larger than 0");
                } else {
                    if (this.currentCollectionUri.getDepth() > (this.freezeAtLevel - 1)) {
                        this.currentCollectionUri = currentCollectionUri.getPaths().get(this.freezeAtLevel - 1);
                    }
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
                    throw new DecoratorComponentException("Illegal value for parameter '" + PARAMETER_DEPTH + "': "
                            + depthStr);
                }
            }

            // ArrayList excludeFolders = new ArrayList<String>();
            String excludeFolders = request.getStringParameter(PARAMETER_EXCLUDE_FOLDERS);
            if (excludeFolders != null) {
                try {
                    StringTokenizer excludeFoldersTokenized = new StringTokenizer(excludeFolders, ",");
                    ArrayList<Path> excludeUIRs = new ArrayList<Path>();
                    while (excludeFoldersTokenized.hasMoreTokens()) {
                        String excludedFolder = excludeFoldersTokenized.nextToken().trim();
                        Path theUri = this.currentCollectionUri.extend(excludedFolder);
                        excludeUIRs.add(theUri);
                    }
                    this.excludeURIs = excludeUIRs;
                } catch (Throwable t) {
                    throw new DecoratorComponentException("Illegal value for parameter '" + PARAMETER_EXCLUDE_FOLDERS
                            + "': " + depthStr);
                }
            }

            this.locale = request.getLocale();
        }

        public Path getCurrentCollectionUri() {
            return this.currentCollectionUri;
        }

        public String getTitle() {
            return this.title;
        }

        public PropertyTypeDefinition getSortProperty() {
            return this.sortProperty;
        }

        public PropertyTypeDefinition getImportancePropDef() {
            return importancePropDef;
        }

        public boolean isAscendingSort() {
            return this.ascendingSort;
        }

        public int getResultSets() {
            return this.resultSets;
        }

        public int getGroupResultSetsBy() {
            return this.groupResultSetsBy;
        }

        public int getDisplayFromLevel() {
            return this.displayFromLevel;
        }

        public int getFreezeAtLevel() {
            return this.freezeAtLevel;
        }

        public Locale getLocale() {
            return this.locale;
        }

        public String getToken() {
            return this.token;
        }

        public int getDepth() {
            return this.depth;
        }

        public ArrayList<Path> getExcludeURIs() {
            return excludeURIs;
        }

        private void initSortField(DecoratorRequest request) {
            String sortFieldParam = "title";
            if (request.getStringParameter(PARAMETER_SORT) != null) {
                sortFieldParam = request.getStringParameter(PARAMETER_SORT);
            }
            if ("title".equals(sortFieldParam)) {
                this.sortProperty = titlePropDef;
            } else if ("name".equals(sortFieldParam)) {
                this.sortProperty = null;
                this.setSortByName(true);
            } else if (!"name".equals(sortFieldParam)) {
                throw new DecoratorComponentException("Illegal value for parameter '" + PARAMETER_SORT
                        + "': must be one of ('name', 'title')");
            }

            String sortDirectionParam = request.getStringParameter(PARAMETER_SORT_DIRECTION);
            if (sortDirectionParam == null) {
                sortDirectionParam = "asc";
            }

            if ("asc".equals(sortDirectionParam)) {
                this.ascendingSort = true;
            } else if ("desc".equals(sortDirectionParam)) {
                this.ascendingSort = false;
            } else {
                throw new DecoratorComponentException("Illegal value for parameter '" + PARAMETER_SORT_DIRECTION
                        + "': '" + sortDirectionParam + "' (must be one of 'asc', 'desc')");
            }
        }

        public void setMaxNumberOfChildren(int maxNumberOfChildren) {
            this.maxNumberOfChildren = maxNumberOfChildren;
        }

        public int getMaxNumberOfChildren() {
            return maxNumberOfChildren;
        }

        public void setSortByName(boolean sortByName) {
            this.sortByName = sortByName;
        }

        public boolean isSortByName() {
            return sortByName;
        }
    }

    public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
    }

    public void setImportancePropDef(PropertyTypeDefinition importancePropDef) {
        this.importancePropDef = importancePropDef;
    }

    public void setSearchLimit(int searchLimit) {
        this.searchLimit = searchLimit;
    }

    protected String getDescriptionInternal() {
        return DESCRIPTION;
    }

    protected Map<String, String> getParameterDescriptionsInternal() {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(PARAMETER_TITLE, PARAMETER_TITLE_DESC);
        map.put(PARAMETER_SORT, PARAMETER_SORT_DESC);
        map.put(PARAMETER_SORT_DIRECTION, PARAMETER_SORT_DIRECTION_DESC);
        map.put(PARAMETER_RESULT_SETS, PARAMETER_RESULT_SETS_DESC);
        map.put(PARAMETER_GROUP_RESULT_SETS_BY, PARAMETER_GROUP_RESULT_SETS_BY_DESC);
        map.put(PARAMETER_DISPLAY_FROM_LEVEL, PARAMETER_DISPLAY_FROM_LEVEL_DESC);
        map.put(PARAMETER_URI, PARAMETER_URI_DESC);
        map.put(PARAMETER_FREEZE_AT_LEVEL, PARAMETER_FREEZE_AT_LEVEL_DESC);
        map.put(PARAMETER_EXCLUDE_FOLDERS, PARAMETER_EXCLUDE_FOLDERS_DESC);
        map.put(PARAMETER_AS_CURRENT_USER, PARAMETER_AS_CURRENT_USER_DESC);
        map.put(PARAMETER_DEPTH, PARAMETER_DEPTH_DESC);
        map.put(PARAMETER_MAX_NUMBER_OF_CHILDREN, PARAMETER_MAX_NUMBER_OF_CHILDREN_DESC);
        return map;
    }

    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();

        if (this.viewService == null) {
            throw new BeanInitializationException("JavaBean property 'viewService set");
        }
        if (this.resourceTypeTree == null) {
            throw new BeanInitializationException("JavaBean property 'resourceTypeTree' not set");
        }
        if (this.titlePropDef == null) {
            throw new BeanInitializationException("JavaBean property 'titlePropDef' not set");
        }
        if (this.modelName == null) {
            throw new BeanInitializationException("JavaBean property 'modelName' not set");
        }
        if (this.searchLimit <= 0) {
            throw new BeanInitializationException("JavaBean property '" + searchLimit + "' must be a positive integer");
        }
    }

}
