/* Copyright (c) 2008, University of Oslo, Norway
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
package org.vortikal.web.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.edit.editor.ResourceWrapperManager;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import org.vortikal.repository.search.PropertySortField;
import org.vortikal.repository.search.QueryParser;
import org.vortikal.repository.search.ResultSet;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.Searcher;
import org.vortikal.repository.search.SortField;
import org.vortikal.repository.search.SortFieldDirection;
import org.vortikal.repository.search.SortingImpl;
import org.vortikal.repository.search.query.AndQuery;
import org.vortikal.repository.search.query.Query;
import org.vortikal.repository.search.query.UriDepthQuery;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.web.URLUtil;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

public class CollectionListingController implements Controller {

	
	private int defaultPageLimit = 20;
	
	private Repository repository;
	private Searcher searcher;
	private ResourceWrapperManager resourceManager;
    private Service viewService;
    
	private String viewName;

	private String query;
	private QueryParser queryParser;
	
	private ResourceTypeDefinition resourceType;
	private List<Query> appendedQueries;
	
	private PropertyTypeDefinition defaultSortPropDef;
	private Map<String, PropertyTypeDefinition> sortPropertyMapping;

	private SortFieldDirection defaultSortOrder;
	private Map<String, SortFieldDirection> sortOrderMapping;
	
    private PropertyTypeDefinition pageLimitPropDef;
    private PropertyTypeDefinition recursivePropDef;
    private PropertyTypeDefinition sortPropDef;

	private List<PropertyDisplayConfig> listableProperties;
        

	public ModelAndView handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String uri = RequestContext.getRequestContext().getResourceURI();
		String token = SecurityContext.getSecurityContext().getToken();
		Resource collection = this.repository.retrieve(token, uri, true);
		
		Resource[] children = this.repository.listChildren(token,	uri, true);
		List<Resource> collections = new ArrayList<Resource>();
		for (Resource r: children) {
			if (r.isCollection()) {
				collections.add(r);
			}
		}
		
		boolean recursive = false;
		if (collection.getProperty(this.recursivePropDef) != null) {
			recursive = collection.getProperty(this.recursivePropDef).getBooleanValue();
		}
		
		int pageLimit = this.defaultPageLimit;
		Property rPageLimit = collection.getProperty(this.pageLimitPropDef);
		if (rPageLimit != null) {
			pageLimit = rPageLimit.getIntValue();
		}
		
		int page = 0;
		if (request.getParameter("page") != null) {
			try {
				page = Integer.parseInt(request.getParameter("page"));	
				if (page < 0) {
					page = 0;
				}
			} catch (Throwable t) { }			
		}
		int offset = page * pageLimit;

		PropertyTypeDefinition sortProp = this.defaultSortPropDef;
		SortFieldDirection sortFieldDirection = this.defaultSortOrder;

		if (collection.getProperty(this.sortPropDef) != null) {
			String sortString = collection.getProperty(this.sortPropDef).getStringValue();
			if (this.sortPropertyMapping.containsKey(sortString)) {
				sortProp = this.sortPropertyMapping.get(sortString);
			}
			if (this.sortOrderMapping.containsKey(sortString)) {
				sortFieldDirection = this.sortOrderMapping.get(sortString);
			}
		}
		
		Search search = new Search();
		Query query = this.queryParser.parse(this.query);

		AndQuery andQuery = new AndQuery();
		andQuery.add(query);
		if (!recursive) {
			int depth = URLUtil.splitUri(uri).length;
			andQuery.add(new UriDepthQuery(depth));
		}

		search.setQuery(andQuery);
		search.setLimit(pageLimit + 1);
		search.setCursor(offset);
		
		List<SortField> sortFields = new ArrayList<SortField>();
		sortFields.add(new PropertySortField(sortProp, sortFieldDirection));
		search.setSorting(new SortingImpl(sortFields));
		ResultSet result = this.searcher.execute(token, search);

		boolean more = result.getSize() == pageLimit + 1;
		int num = result.getSize();
		if (more) num--;
		
		Map<String, URL> urls = new HashMap<String, URL>();
		List<PropertySet> files = new ArrayList<PropertySet>();
		for (int i = 0; i < num; i++) {
			PropertySet res = result.getResult(i);
			files.add(res);
			URL url = this.viewService.constructURL(res.getURI());
			urls.put(res.getURI(), url);
		}
		
		Map<String, Object> model = new HashMap<String, Object>();
		
		URL nextURL = null;
		if (more && pageLimit > 0) {
			nextURL = URL.create(request);
			nextURL.setParameter("page", String.valueOf(page + 1));
		}
		URL prevURL = null;
		if (page > 0 && pageLimit > 0) {
			prevURL = URL.create(request);
			if (page == 1) {
				prevURL.removeParameter("page");
			} else {
				prevURL.setParameter("page", String.valueOf(page - 1));
			}
		}

		List<PropertyTypeDefinition> displayPropDefs = new ArrayList<PropertyTypeDefinition>();
		for (PropertyDisplayConfig config: this.listableProperties) {
			Property hide = null;
			if (config.getPreventDisplayProperty() != null) {
				hide = collection.getProperty(config.getPreventDisplayProperty());
			}
			if (hide == null) {
				displayPropDefs.add(config.getDisplayProperty());
			}
		}
		
		model.put("resource", this.resourceManager.createResourceWrapper(collection.getURI()));
		model.put("files", files);
		model.put("collections", collections);
		
		model.put("urls", urls);
		model.put("nextURL", nextURL);
		model.put("prevURL", prevURL);
		
		model.put("displayPropDefs", displayPropDefs);

		Map<String, Object> mainModel = new HashMap<String, Object>();
		mainModel.put("collectionListing", model);
		return new ModelAndView(this.viewName, mainModel);
	}

	@Required public void setRepository(Repository repository) {
		this.repository = repository;
	}

	@Required public void setSearcher(Searcher searcher) {
		this.searcher = searcher;
	}

	@Required public void setViewName(String viewName) {
		this.viewName = viewName;
	}

	@Required public void setViewService(Service viewService) {
		this.viewService = viewService;
	}

	@Required public void setResourceManager(ResourceWrapperManager resourceManager) {
		this.resourceManager = resourceManager;
	}

	@Required public void setResourceType(ResourceTypeDefinition resourceType) {
		this.resourceType = resourceType;
	}

	@Required public void setPageLimitPropDef(PropertyTypeDefinition pageLimitPropDef) {
		this.pageLimitPropDef = pageLimitPropDef;
	}

	@Required public void setRecursivePropDef(PropertyTypeDefinition recursivePropDef) {
		this.recursivePropDef = recursivePropDef;
	}

	@Required public void setSortPropDef(PropertyTypeDefinition sortPropDef) {
		this.sortPropDef = sortPropDef;
	}

	@Required public void setDefaultSortPropDef(PropertyTypeDefinition defaultSortPropDef) {
		this.defaultSortPropDef = defaultSortPropDef;
	}

	@Required public void setListableProperties(List<PropertyDisplayConfig> listableProperties) {
		this.listableProperties = listableProperties;
	}

	@Required public void setSortPropertyMapping(Map<String, PropertyTypeDefinition> sortPropertyMapping) {
		this.sortPropertyMapping = sortPropertyMapping;
	}

	@Required public void setSortOrderMapping(Map<String, SortFieldDirection> sortOrderMapping) {
		this.sortOrderMapping = sortOrderMapping;
	}

	@Required public void setDefaultSortOrder(SortFieldDirection defaultSortOrder) {
		this.defaultSortOrder = defaultSortOrder;
	}

	public void setAppendedQueries(List<Query> appendedQueries) {
		this.appendedQueries = appendedQueries;
	}

	@Required public void setQuery(String query) {
		this.query = query;
	}

	@Required public void setQueryParser(QueryParser queryParser) {
		this.queryParser = queryParser;
	}


	public static class PropertyDisplayConfig {
		private PropertyTypeDefinition displayProperty;
		private PropertyTypeDefinition preventDisplayProperty;

		public void setDisplayProperty(PropertyTypeDefinition displayProperty) {
			this.displayProperty = displayProperty;
		}
		public PropertyTypeDefinition getDisplayProperty() {
			return displayProperty;
		}
		public void setPreventDisplayProperty(PropertyTypeDefinition preventDisplayProperty) {
			this.preventDisplayProperty = preventDisplayProperty;
		}
		public PropertyTypeDefinition getPreventDisplayProperty() {
			return preventDisplayProperty;
		}
	}


}
