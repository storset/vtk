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
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repositoryimpl.query.HashSetPropertySelect;
import org.vortikal.repositoryimpl.query.parser.QueryManager;
import org.vortikal.repositoryimpl.query.parser.ResultSet;
import org.vortikal.repositoryimpl.query.query.SimpleSortField;
import org.vortikal.repositoryimpl.query.query.SortingImpl;
import org.vortikal.util.web.URLUtil;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.view.components.menu.ListMenu;
import org.vortikal.web.view.components.menu.MenuItem;
import org.vortikal.web.view.decorating.DecoratorRequest;
import org.vortikal.web.view.decorating.DecoratorResponse;



/**
 * XXX: How should we handle authorization?
 */
public class ListMenuComponent extends ViewRenderingDecoratorComponent {

    private static Log logger = LogFactory.getLog(IncludeComponent.class);
    
    private QueryManager queryManager;
    private Service viewService;
    private PropertyTypeDefinition titlePropdef;
    private String modelName = "menu";

    public void setQueryManager(QueryManager queryManager) {
        this.queryManager = queryManager;
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
    

    public void processModel(Map model, DecoratorRequest request, DecoratorResponse response)
        throws Exception {

        String uri = request.getStringParameter("uri");
        
        if (uri == null) {
            throw new DecoratorComponentException("Parameter 'uri' not specified");
        }

        RequestContext requestContext = RequestContext.getRequestContext();
        String currentURI = requestContext.getResourceURI();

        int depth = URLUtil.splitUri(uri).length;

        String token = null;
        String query = "uri = " + uri + " OR (uri = " + uri + 
            "/* AND type IN collection AND depth = " + depth + ")";
        SortingImpl sorting = new SortingImpl();
        sorting.addSortField(new SimpleSortField("uri"));
        
        HashSetPropertySelect select = new HashSetPropertySelect();
        
        ResultSet rs = this.queryManager.execute(token, query, sorting, 10, select);

        MenuItem activeItem = null;
        List items = new ArrayList();
        
        for (int i = 0; i < rs.getSize(); i++) {
            PropertySet res = (PropertySet) rs.getResult(i);
            
            String url = viewService.constructLink(res.getURI());
            Property titleProperty = res.getProperty(this.titlePropdef);
            String title = titleProperty != null ?
                titleProperty.getStringValue() : res.getName();

            MenuItem item = new MenuItem();
            item.setUrl(url);
            item.setLabel(this.getName() + "_item");
            item.setTitle(title);
            item.setActive(false);
            if (currentURI.equals(res.getURI())) {
                activeItem = item;
            } else if (currentURI.startsWith(res.getURI()) && activeItem == null) {
                activeItem = item;
            }

            items.add(item);
        }

        activeItem.setActive(true);

        ListMenu menu = new ListMenu();
        menu.setItems((MenuItem[]) items.toArray(new MenuItem[items.size()]));
        menu.setLabel(this.getName());
        model.put(this.modelName, menu);
    }

}
