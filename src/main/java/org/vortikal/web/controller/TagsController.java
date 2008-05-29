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
package org.vortikal.web.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.search.ResultSet;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.Searcher;
import org.vortikal.repository.search.WildcardPropertySelect;
import org.vortikal.repository.search.query.AndQuery;
import org.vortikal.repository.search.query.PropertyTermQuery;
import org.vortikal.repository.search.query.Query;
import org.vortikal.repository.search.query.TermOperator;
import org.vortikal.repository.search.query.UriOperator;
import org.vortikal.repository.search.query.UriPrefixQuery;
import org.vortikal.repository.search.query.UriTermQuery;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;

public class TagsController implements Controller {

    private Repository repository;
    private Searcher searcher;
    private String viewName;
    private PropertyTypeDefinition propDef;
    
    @SuppressWarnings("unchecked")
    public ModelAndView handleRequest(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        String token = SecurityContext.getSecurityContext().getToken();
        Search search = new Search();
        String tag = request.getParameter("tag");

        Map model = new HashMap();

        if (tag == null || tag.trim().equals("")) {
            model.put("error", "No tags specified");
            return new ModelAndView(this.viewName, model);
        }
        
        Query query = new PropertyTermQuery(this.propDef, tag, TermOperator.EQ_IGNORECASE);

        Resource scopedResource = null;
        String scope = request.getParameter("scope");
        if (scope != null && !scope.trim().equals("")) {
            if (".".equals(scope)) {
                scope = RequestContext.getRequestContext().getCurrentCollection();
            }
            if (scope.startsWith("/")) {
                scopedResource = this.repository.retrieve(token, scope, true);

                AndQuery andQuery = new AndQuery(); 
                andQuery.add(query);
                andQuery.add(new UriPrefixQuery(scope));
                query = andQuery;
            }
        }
        
        search.setQuery(query);
        search.setPropertySelect(WildcardPropertySelect.WILDCARD_PROPERTY_SELECT);
        
        ResultSet rs = searcher.execute(token, search);

        model.put("tag", tag);
        model.put("scope", scopedResource);
        
        List<String> urls = new ArrayList<String>();
        model.put("urls", urls);
        List<PropertySet> resources = new ArrayList<PropertySet>();
        model.put("resources", resources);
        
        Iterator<PropertySet> iterator = rs.iterator();
        while (iterator.hasNext()) {
            PropertySet next = iterator.next();
            urls.add(next.getURI());
            resources.add(next);
        } 
        
        return new ModelAndView(this.viewName, model);
    }

    @Required public void setSearcher(Searcher searcher) {
        this.searcher = searcher;
    }

    @Required public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    @Required public void setPropDef(PropertyTypeDefinition propDef) {
        this.propDef = propDef;
    }

    @Required public void setRepository(Repository repository) {
        this.repository = repository;
    }

}
