/* Copyright (c) 2010, University of Oslo, Norway
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
package org.vortikal.web.actions.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.support.RequestContext;
import org.vortikal.repository.Path;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import org.vortikal.repository.search.ResultSet;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.SortingImpl;
import org.vortikal.repository.search.query.AndQuery;
import org.vortikal.repository.search.query.OrQuery;
import org.vortikal.repository.search.query.TermOperator;
import org.vortikal.repository.search.query.TypeTermQuery;
import org.vortikal.repository.search.query.UriDepthQuery;
import org.vortikal.repository.search.query.UriPrefixQuery;
import org.vortikal.web.decorating.components.SubFolderMenuComponent;
import org.vortikal.web.decorating.components.SubFolderMenuComponent.MenuRequest;
import org.vortikal.web.service.Service;
import org.vortikal.web.view.components.menu.ListMenu;

public class CollectionStructureReporter extends AbstractReporter {

    private Service viewService;
    private Service reportService;
    private PropertyTypeDefinition titlePropDef;
    private PropertyTypeDefinition hiddenPropDef;
    private PropertyTypeDefinition importancePropDef;
    private PropertyTypeDefinition navigationTitlePropDef;
    private ResourceTypeDefinition collectionResourceType;

    public Map<String, Object> getReportContent(String token, Resource currentResource, HttpServletRequest request) {
        AndQuery query = new AndQuery();
        query.add(new TypeTermQuery("collection", TermOperator.IN));
        query.add(new UriPrefixQuery(currentResource.getURI().toString()));
        OrQuery depthQuery = new OrQuery();
        for (int i = 0; i < 3; i++) {
            depthQuery.add(new UriDepthQuery(i + currentResource.getURI().getDepth()));
        }
        query.add(depthQuery);
        
        Search search = new Search();
        search.setLimit(Integer.MAX_VALUE);
        SortingImpl sorting = new SortingImpl();
        search.setSorting(sorting);

        search.setQuery(query);

        ResultSet rs = this.searcher.execute(token, search);
        Map<String, Object> result = new HashMap<String, Object>();

        Locale locale = new RequestContext(request).getLocale(); 

        Map<String, Object> menu = getSubfolderMenu(rs, currentResource.getURI(), token, locale);
        result.put("subFolderMenu", menu);

        return result;
    }

    private Map<String, Object> getSubfolderMenu(ResultSet rs, Path currentCollectionUri, String token, Locale locale) {
        String title = null;
        PropertyTypeDefinition sortProperty = null;
        boolean ascendingSort = true;
        boolean sortByName = false;
        int resultSets = 1;
        int groupResultSetsBy = 0;
        int freezeAtLevel = 0;
        int depth = 2;
        int displayFromLevel = -1;
        int maxNumberOfChildren = Integer.MAX_VALUE;
        ArrayList<Path> excludeURIs = new ArrayList<Path>();
        int searchLimit = Integer.MAX_VALUE;
        boolean structuredCollectionReportLink = true;

        SubFolderMenuComponent subfolderMenu = new SubFolderMenuComponent();
        subfolderMenu.setViewService(viewService);
        subfolderMenu.setReportService(reportService);
        subfolderMenu.setNavigationTitlePropDef(navigationTitlePropDef);
        subfolderMenu.setTitlePropDef(titlePropDef);
        subfolderMenu.setImportancePropDef(importancePropDef);
        subfolderMenu.setHiddenPropDef(hiddenPropDef);
        subfolderMenu.setCollectionResourceType(collectionResourceType);

        MenuRequest menuRequest = subfolderMenu.getNewMenuReqeust(currentCollectionUri, title, sortProperty,
                ascendingSort, sortByName, resultSets, groupResultSetsBy, freezeAtLevel, depth, displayFromLevel,
                maxNumberOfChildren, excludeURIs, locale, token, searchLimit, structuredCollectionReportLink);

        ListMenu<PropertySet> menu = subfolderMenu.buildListMenu(rs, menuRequest);
        return subfolderMenu.buildMenuModel(menu, menuRequest);
    }

    public void setViewService(Service viewService) {
        this.viewService = viewService;
    }
    
    public void setReportService(Service reportService) {
        this.reportService = reportService;
    }

    public void setTitlePropDef(PropertyTypeDefinition titlePropDef) {
        this.titlePropDef = titlePropDef;
    }

    public void setHiddenPropDef(PropertyTypeDefinition hiddenPropDef) {
        this.hiddenPropDef = hiddenPropDef;
    }

    public void setImportancePropDef(PropertyTypeDefinition importancePropDef) {
        this.importancePropDef = importancePropDef;
    }

    public void setNavigationTitlePropDef(PropertyTypeDefinition navigationTitlePropDef) {
        this.navigationTitlePropDef = navigationTitlePropDef;
    }

    public void setCollectionResourceType(ResourceTypeDefinition collectionResourceType) {
        this.collectionResourceType = collectionResourceType;
    }

}