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
package org.vortikal.web.actions.subresource;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.Path;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceNotFoundException;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import org.vortikal.repository.search.ResultSet;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.Searcher;
import org.vortikal.repository.search.WildcardPropertySelect;
import org.vortikal.repository.search.query.AndQuery;
import org.vortikal.repository.search.query.OrQuery;
import org.vortikal.repository.search.query.TermOperator;
import org.vortikal.repository.search.query.TypeTermQuery;
import org.vortikal.repository.search.query.UriDepthQuery;
import org.vortikal.repository.search.query.UriPrefixQuery;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.URL;

public class SubresourceController implements Controller, InitializingBean {
    
    private Searcher searcher;
    private Repository repository;
    private ResourceTypeDefinition documentTypeDefinition;
    private ResourceTypeDefinition collectionTypeDefinition;
    private String userAgent = "Subresource retriever";
    
    private static Log logger = LogFactory.getLog(SubresourceController.class);
    
    @SuppressWarnings("unchecked")
    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String uri = null;
        
        try {
          uri = (String) request.getParameter("uri");  
        } catch (Exception e) {
            badRequest(e, response);
            return null;
        }
        
        if(uri == null) {
          return null;
        }

        Path path = RequestContext.getRequestContext().getCurrentCollection();
        URL base = URL.create(request);
        base.clearParameters();
        base.setPath(path);
        
        List<Subresource> subresources = buildSearchAndPopulateSubresources(uri);
        
        writeResults(subresources, response);
        return null;
    }

    private List<Subresource> buildSearchAndPopulateSubresources(String uri) {
        List<Subresource> subresources = new ArrayList();
        
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        String token = securityContext.getToken();
      
        // MainQuery (depth + 1 from uri)
        Path url = Path.fromString(uri);
        int depth = url.getDepth() + 1;
        AndQuery mainQuery = new AndQuery();
        mainQuery.add(new UriPrefixQuery(url.toString()));
        mainQuery.add(new UriDepthQuery(depth));
        OrQuery resourceQuery = new OrQuery();
        
        // Document OR collection
        resourceQuery.add(new TypeTermQuery(documentTypeDefinition.getName(), TermOperator.IN));
        resourceQuery.add(new TypeTermQuery(collectionTypeDefinition.getName(), TermOperator.IN));
        mainQuery.add(resourceQuery);
        
        Search search = new Search();
        search.setQuery(mainQuery);
        search.setLimit(500);
        search.setPropertySelect(new WildcardPropertySelect());
        ResultSet rs = searcher.execute(token, search);
        
        List<PropertySet> results = rs.getAllResults();
        Resource res = null;

        // Retrieve resource and populate sub-resources
        for(PropertySet result : results) {
          String resourceURI = result.getURI().toString();
          String resourceName = result.getName();
          String resourceTitle = "";
          boolean resourceisCollection = false;
          boolean resourceIsReadRestricted = false;
          boolean resourceIsInheritedAcl = false;
          try {
            res = this.repository.retrieve(token, result.getURI(), true);
            if (res != null) {
              resourceTitle = res.getTitle();
              resourceisCollection = res.isCollection();
              if(res.isReadRestricted()) {
                  resourceIsInheritedAcl = true;
               }
               if(res.isInheritedAcl()) {
                  resourceIsInheritedAcl = true;
               }
             }
           } catch (ResourceNotFoundException e) {
             logger.error("ResourceNotFoundException " + e.getMessage());
           } catch (AuthorizationException e) {
             logger.error("AuthorizationException " + e.getMessage());
           } catch (AuthenticationException e) {
             logger.error("AuthenticationException " + e.getMessage());
           } catch (Exception e) {
             logger.error("Exception " + e.getMessage());
           }
           subresources.add(new Subresource(resourceURI, resourceName, resourceTitle, resourceisCollection, 
                                            resourceIsReadRestricted, resourceIsInheritedAcl));
        }
        return subresources;
    }

    private void badRequest(Throwable e, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        PrintWriter writer = response.getWriter();
        try {
            writer.write(e.getMessage());
        } finally {
            writer.close();
        }
    }
    
    private void writeResults(List<Subresource> subresources, HttpServletResponse response) throws Exception {
        JSONArray list = new JSONArray();
        for (Subresource sr: subresources) {
            JSONObject o = new JSONObject();
            o.put("uri", sr.getUri());
            o.put("name", sr.getName());
            o.put("title", sr.getTitle());
            o.put("collection", sr.isCollection());
            o.put("readrestricted", sr.permissions.isReadRestricted());
            o.put("inherited", sr.permissions.isInheritedAcl());
            list.add(o);
        }
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/plain;charset=utf-8");
        PrintWriter writer = response.getWriter();
        try {
            writer.print(list.toString(1));
        } finally {
            writer.close();
        }
    }
    
    private static class Subresource {
        private String uri;
        private String name;
        private String title;
        private boolean collection;
        private SubresourcePermissions permissions;

        public Subresource(String uri, String name, String title,
                           boolean collection, boolean isReadProtected, boolean isInherited) { /* , String read, String write, String admin) { */
            this.uri = uri;
            this.name = name;
            this.title = title;
            this.collection = collection;
            this.permissions = new SubresourcePermissions(isReadProtected, isInherited); /* , read, write, admin); */
        }
        public String getUri() {
            return this.uri;
        }
        public String getName() {
            return this.name;
        }
        public String getTitle() {
            return this.title;
        }
        public boolean isCollection() {
            return this.collection;
        }
    }
    
    private static class SubresourcePermissions {
        private boolean readRestricted = false;
        private boolean inheritedAcl = true;
        /* private String read;
        private String write;
        private String admin; */
        public SubresourcePermissions(boolean readRestricted, boolean inheritedAcl) { /*, String read, String write, String admin) { */
            this.readRestricted = readRestricted;
            this.inheritedAcl = inheritedAcl;
           /* this.read = read;
            this.write = write;
            this.admin = admin; */
        }
        public boolean isReadRestricted() {
            return this.readRestricted;
        }
        public boolean isInheritedAcl() {
            return this.inheritedAcl;
        }
        /*
        public String getRead() {
            return this.read;
        }
        public String getWrite() {
            return this.write;
        }
        public String getAdmin() {
            return this.admin;
        }
        */
    }
    
    public void setSearcher(Searcher searcher) {
        this.searcher = searcher;
    }
    
    public void setRepository(Repository repository) {
        this.repository = repository;
    }
    
    public void setDocumentTypeDefinition(ResourceTypeDefinition documentTypeDefinition) {
        this.documentTypeDefinition = documentTypeDefinition;
    }

    public void setCollectionTypeDefinition(ResourceTypeDefinition collectionTypeDefinition) {
        this.collectionTypeDefinition = collectionTypeDefinition;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
    }
}