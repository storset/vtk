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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanInitializationException;

import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import org.vortikal.repository.search.ConfigurablePropertySelect;
import org.vortikal.repository.search.PropertySortField;
import org.vortikal.repository.search.QueryParser;
import org.vortikal.repository.search.ResultSet;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.Searcher;
import org.vortikal.repository.search.SortField;
import org.vortikal.repository.search.SortFieldDirection;
import org.vortikal.repository.search.SortingImpl;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.web.URLUtil;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.view.components.menu.ListMenu;
import org.vortikal.web.view.components.menu.MenuItem;
import org.vortikal.web.view.decorating.DecoratorRequest;
import org.vortikal.web.view.decorating.DecoratorResponse;


public class SubFolderMenuComponent extends ViewRenderingDecoratorComponent {

    private static final String DESCRIPTION
        = "Lists the child folders of the current folder";

    private static final String PARAMETER_TITLE = "title";
    private static final String PARAMETER_TITLE_DESC = "The menu title";

    private static final String PARAMETER_SORT = "sort";
    private static final String PARAMETER_SORT_DESC =
        "The name of a property to sort results by. The default sort property is 'title'.";

    private static final String PARAMETER_SORT_DIRECTION = "direction";
    private static final String PARAMETER_SORT_DIRECTION_DESC =
        "The sort direction. Legal values are 'asc', 'desc'. The default value is 'asc' ";

    private static final String PARAMETER_RESULT_SETS = "result-sets";
    private static final String PARAMETER_RESULT_SETS_DESC =
        "The number of result sets to split the result into. The default value is '1'";
    private static final int PARAMETER_RESULT_SETS_MAX_VALUE = 30;

    private static final String PARAMETER_AS_CURRENT_USER = "authenticated";
    private static final String PARAMETER_AS_CURRENT_USER_DESC = 
        "The default is that only resources readable for everyone is listed. " +
            "If this is set to 'true', the listing is done as the currently " +
            "logged in user (if any)";

    private static Log logger = LogFactory.getLog(SubFolderMenuComponent.class);
    
    private QueryParser queryParser;
    private Service viewService;
    private PropertyTypeDefinition titlePropDef;
    private ResourceTypeDefinition collectionResourceType;
    private String modelName = "menu";
    private int searchLimit = 200;
    private Searcher searcher;
    private ResourceTypeTree resourceTypeTree;
    


    public void processModel(Map model, DecoratorRequest request, DecoratorResponse response)
        throws Exception {
        MenuRequest menuRequest = new MenuRequest(request);
        Search search = buildSearch(menuRequest);
        String token = menuRequest.getToken();
        ResultSet rs = this.searcher.execute(token, search);
        if (logger.isDebugEnabled()) {
            logger.debug("Executed search: " + search + ", hits: " + rs.getSize());
        }
        ListMenu<String> menu = buildListMenu(rs, menuRequest);
        Map<String, Object> menuModel = buildMenuModel(menu, menuRequest);
        model.put(this.modelName, menuModel);
        if (logger.isDebugEnabled()) {
            logger.debug("Built model: : " + model + " from menu: " + menu);
        }
    }
    

    private Map<String, Object> buildMenuModel(ListMenu<String> menu, MenuRequest menuRequest) {
        List<ListMenu<String>> resultList = new ArrayList<ListMenu<String>>();
        int resultSets = menuRequest.getResultSets();
        List<MenuItem<String>> allItems = menu.getItems();
        if (resultSets > allItems.size()) {
            resultSets = allItems.size();
        }


        int itemsPerResultSet = Math.round((float) allItems.size() / (float) resultSets);
        int remainder = allItems.size() - (resultSets * itemsPerResultSet);

        for (int i = 0; i < resultSets; i++) {
            int startIdx = i * itemsPerResultSet;
            int endIdx = startIdx + itemsPerResultSet;

            if (i == resultSets - 1 && remainder > 0) {
                endIdx += remainder;
            }
            if (endIdx > allItems.size()) {
                endIdx = allItems.size();
            }

            List<MenuItem<String>> subList = allItems.subList(startIdx, endIdx);
            ListMenu<String> m = new ListMenu<String>();
            m.setTitle(menu.getTitle());
            m.setLabel(menu.getLabel());
            m.addAllItems(subList);
            resultList.add(m);
        }

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("resultSets", resultList);
        model.put("size", new Integer(menu.getItems().size()));
        model.put("title", menu.getTitle());
        return model;
    }
    

    private Search buildSearch(MenuRequest menuRequest) {
        String uri = menuRequest.getURI();
        int depth = URLUtil.splitUri(uri).length;
        StringBuffer query = new StringBuffer();
        
        query.append("uri = ").append(uri).append("/*");
        query.append(" AND depth = ").append(depth);
        query.append(" AND type IN ").append(this.collectionResourceType.getName());

        ConfigurablePropertySelect select = new ConfigurablePropertySelect();
        select.addPropertyDefinition(this.titlePropDef);
        Search search = new Search();
        search.setQuery(this.queryParser.parse(query.toString()));
        search.setLimit(this.searchLimit);
        search.setPropertySelect(select);
        SortingImpl sorting = new SortingImpl();
        sorting.addSortField(menuRequest.getSortField());
        search.setSorting(sorting);
        return search;
    }
    

    private ListMenu<String> buildListMenu(ResultSet rs, MenuRequest menuRequest) {
        
        List<MenuItem<String>> items = new ArrayList<MenuItem<String>>();

        for (int i = 0; i < rs.getSize(); i++) {
            PropertySet resource = rs.getResult(i);
            
            String url = this.viewService.constructLink(resource.getURI());
            Property titleProperty = resource.getProperty(this.titlePropDef);
            String title = titleProperty != null ?
                titleProperty.getStringValue() : resource.getName();

            MenuItem<String> item = new MenuItem<String>();
            item.setUrl(url);
            item.setTitle(title);
            item.setLabel(title);
            item.setActive(false);
            items.add(item);
        }

        // Sort children (if not already sorted):
        if (!menuRequest.isUserSpecifiedSorting()) {
            items = sortByTitleOrName(items, menuRequest.getLocale());
        }

        ListMenu<String> menu = new ListMenu<String>();
        menu.addAllItems(items);
        menu.setTitle(menuRequest.getTitle());
        menu.setLabel(this.modelName);
        return menu;
    }


    
    private List<MenuItem<String>> sortByTitleOrName(List<MenuItem<String>> items, Locale locale) {
        Collections.sort(items, new ItemTitleComparator(locale));
        return items;
    }
    

    private class MenuRequest {
        private String uri;
        private String title;
        private SortField sortField;
        private boolean userSpecifiedSorting = false;
        private int resultSets = 1;
        private Locale locale;
        private String token;

        public MenuRequest(DecoratorRequest request) {

            RequestContext requestContext = RequestContext.getRequestContext();            
            this.uri = requestContext.getResourceURI();
            this.uri = requestContext.getCurrentCollection();

            boolean asCurrentUser = "true".equals(
                request.getStringParameter(PARAMETER_AS_CURRENT_USER));
            if (asCurrentUser) {
                SecurityContext securityContext = SecurityContext.getSecurityContext();
                this.token = securityContext.getToken();
            }

            this.title = request.getStringParameter(PARAMETER_TITLE);

            initSortField(request);
            
            if (request.getStringParameter(PARAMETER_RESULT_SETS) != null) {
                try {
                    this.resultSets = Integer.parseInt(
                        request.getStringParameter(PARAMETER_RESULT_SETS));
                } catch (Throwable t) {
                    throw new DecoratorComponentException(
                        "Illegal value for parameter '" + PARAMETER_RESULT_SETS + "': "
                        + request.getStringParameter(PARAMETER_RESULT_SETS));
                }
                if (this.resultSets <= 0 || this.resultSets > PARAMETER_RESULT_SETS_MAX_VALUE) {
                    throw new DecoratorComponentException(
                        "Illegal value for parameter '" + PARAMETER_RESULT_SETS
                        + "': " + this.resultSets + ": must be a positive number between 1 and "
                        + PARAMETER_RESULT_SETS_MAX_VALUE);
                }
            }
            this.locale = request.getLocale();
        }
        
        public String getURI() {
            return this.uri;
        }

        public String getTitle() {
            return this.title;
        }

        public SortField getSortField() {
            return this.sortField;
        }

        public boolean isUserSpecifiedSorting() {
            return this.userSpecifiedSorting;
        }

        public int getResultSets() {
            return this.resultSets;
        }

        public Locale getLocale() {
            return this.locale;
        }
        
        public String getToken() {
            return this.token;
        }
        
        private void initSortField(DecoratorRequest request) {
            String sortFieldParam = request.getStringParameter(PARAMETER_SORT);
            if (sortFieldParam == null) {
                this.sortField = new PropertySortField(titlePropDef, SortFieldDirection.ASC);
                return;
            }
            String prefix;
            String name;
                
            String[] splitValues = sortFieldParam.split(":");
            if (splitValues.length == 2) {
                prefix = splitValues[0];
                name = splitValues[1];
            } else if (splitValues.length == 1) {
                prefix = null;
                name = splitValues[0];
            } else {
                throw new DecoratorComponentException(
                    "Invalid sort field: '" + sortFieldParam + "'");
            }
            PropertyTypeDefinition sortProperty =
                resourceTypeTree.getPropertyDefinitionByPrefix(prefix, name);
            if (sortProperty == null) {
                throw new DecoratorComponentException("Invalid sort field: '" + sortFieldParam 
                                                      + "': matches no property definition");
            }
            this.userSpecifiedSorting = true;

            SortFieldDirection direction;
            String sortDirectionParam = request.getStringParameter(PARAMETER_SORT_DIRECTION);
            if (sortDirectionParam == null) {
                sortDirectionParam = "asc";
            }
            if ("asc".equals(sortDirectionParam)) {
                direction = SortFieldDirection.ASC;
            } else if ("desc".equals(sortDirectionParam)) {
                direction = SortFieldDirection.DESC;
            } else {
                throw new DecoratorComponentException(
                    "Illegal value for parameter '" + PARAMETER_SORT_DIRECTION
                    + "': '" + sortDirectionParam + "' (must be one of 'asc', 'desc')");
            }
            this.sortField = new PropertySortField(sortProperty, direction);
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
    

    public void setQueryParser(QueryParser queryParser) {
        this.queryParser = queryParser;
    }

    public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
    }

    public void setViewService(Service viewService) {
        this.viewService = viewService;
    }
    
    public void setTitlePropDef(PropertyTypeDefinition titlePropDef) {
        this.titlePropDef = titlePropDef;
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
        return DESCRIPTION;
    }


    protected Map<String, String> getParameterDescriptionsInternal() {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(PARAMETER_TITLE, PARAMETER_TITLE_DESC);
        map.put(PARAMETER_SORT, PARAMETER_SORT_DESC);
        map.put(PARAMETER_SORT_DIRECTION, PARAMETER_SORT_DIRECTION_DESC);
        map.put(PARAMETER_RESULT_SETS, PARAMETER_RESULT_SETS_DESC);
        map.put(PARAMETER_AS_CURRENT_USER, PARAMETER_AS_CURRENT_USER_DESC);
        return map;
    }


    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        if (this.queryParser == null) {
            throw new BeanInitializationException(
                    "JavaBean property 'queryParser' not set");
        }
        if (this.viewService == null) {
            throw new BeanInitializationException(
                    "JavaBean property 'viewService set");
        }
        if (this.searcher == null) {
            throw new BeanInitializationException(
                    "JavaBean property 'searcher' not set");
        }
        if (this.resourceTypeTree == null) {
            throw new BeanInitializationException(
                    "JavaBean property 'resourceTypeTree' not set");
        }
        if (this.titlePropDef == null) {
            throw new BeanInitializationException(
                    "JavaBean property 'titlePropDef' not set");
        }
        if (this.modelName == null) {
            throw new BeanInitializationException(
                    "JavaBean property 'modelName' not set");
        }
        if (this.searchLimit <= 0) {
            throw new BeanInitializationException(
                "JavaBean property '" + searchLimit + "' must be a positive integer");
        }
    }
    
}
