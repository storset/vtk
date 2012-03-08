/* Copyright (c) 2012, University of Oslo, Norway
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

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.json.simple.JSONValue;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.ContentStream;
import org.vortikal.repository.Property;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.search.PropertySortField;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.SortFieldDirection;
import org.vortikal.repository.search.SortingImpl;
import org.vortikal.repository.search.query.ACLReadForAllQuery;
import org.vortikal.repository.search.query.AndQuery;
import org.vortikal.repository.search.query.OrQuery;
import org.vortikal.repository.search.query.PropertyTermQuery;
import org.vortikal.repository.search.query.TermOperator;
import org.vortikal.repository.search.query.UriPrefixQuery;

public class BrokenLinksReport extends DocumentReporter {
    
    private PropertyTypeDefinition linkStatusPropDef;
    private PropertyTypeDefinition linkCheckPropDef;
    private PropertyTypeDefinition sortPropDef;
    private PropertyTypeDefinition publishedPropDef;
    private SortFieldDirection sortOrder;

    @Override
    protected Search getSearch(String token, Resource currentResource, HttpServletRequest request) {
        OrQuery linkStatusCriteria = new OrQuery();
        linkStatusCriteria.add(new PropertyTermQuery(this.linkStatusPropDef, "BROKEN_LINKS", TermOperator.EQ))
        .add(new PropertyTermQuery(this.linkStatusPropDef, "AWAITING_LINKCHECK", TermOperator.EQ));

        AndQuery topLevel = new AndQuery();
        
        // Read restriction (all|true|false)
        String readRestriction = request.getParameter("read-restriction");

        if ("true".equals(readRestriction)) {
            ACLReadForAllQuery aclReadForAllQuery = new ACLReadForAllQuery(true);
            topLevel.add(aclReadForAllQuery);
        } else if ("false".equals(readRestriction)) {
            ACLReadForAllQuery aclReadForAllQuery = new ACLReadForAllQuery();
            topLevel.add(aclReadForAllQuery);
        }
          
        topLevel.add(new UriPrefixQuery(currentResource.getURI().toString())).add(linkStatusCriteria);
 
        SortingImpl sorting = new SortingImpl();
        sorting.addSortField(new PropertySortField(this.sortPropDef, this.sortOrder));
        Search search = new Search();
        search.setQuery(topLevel);
        search.setSorting(sorting);
        
        // Published (true|false)
        if("false".equals(request.getParameter("published"))) {
            // ONLY those NOT published
            PropertyTermQuery ptq = new PropertyTermQuery(this.publishedPropDef, "true", TermOperator.NE);
            topLevel.add(ptq);
        } else {
            search.setOnlyPublishedResources(true);
        }

        return search;
    }
    
    @Override
    protected void handleResult(Resource resource, Map<String, Object> model) {
        Property linkCheck = resource.getProperty(this.linkCheckPropDef);
        if (linkCheck == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) model.get("linkCheck");
        if (map == null) {
            map = new HashMap<String, Object>();
            model.put("linkCheck", map);
        } 
        
        ContentStream binaryStream = linkCheck.getBinaryStream();
        Object obj = JSONValue.parse(new InputStreamReader(binaryStream.getStream()));
        map.put(resource.getURI().toString(), obj);
    }

    @Required
    public void setLinkStatusPropDef(PropertyTypeDefinition linkStatusPropDef) {
        this.linkStatusPropDef = linkStatusPropDef;
    }
    
    @Required
    public void setLinkCheckPropDef(PropertyTypeDefinition linkCheckPropDef) {
        this.linkCheckPropDef = linkCheckPropDef;
    }

    @Required
    public void setPublishedPropDef(PropertyTypeDefinition publishedPropDef) {
        this.publishedPropDef = publishedPropDef;
    }

    @Required
    public void setSortPropDef(PropertyTypeDefinition sortPropDef) {
        this.sortPropDef = sortPropDef;
    }

    @Required
    public void setSortOrder(SortFieldDirection sortOrder) {
        this.sortOrder = sortOrder;
    }

}
