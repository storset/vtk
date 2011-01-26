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
package org.vortikal.web.actions.report.subresource;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

public class SubresourceProvider {

    private Searcher searcher;
    private Repository repository;
    private ResourceTypeDefinition documentTypeDefinition;
    private ResourceTypeDefinition collectionTypeDefinition;
    
    private static Log logger = LogFactory.getLog(SubresourceProvider.class);
    
    @SuppressWarnings("unchecked")
    public List<SubresourcePermissions> buildSearchAndPopulateSubresources(String uri) {
        List<SubresourcePermissions> subresources = new ArrayList();
        
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
           subresources.add(new SubresourcePermissions(resourceURI, resourceName, resourceTitle, resourceisCollection, 
                                            resourceIsReadRestricted, resourceIsInheritedAcl));
        }
        return subresources;
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
    
}
