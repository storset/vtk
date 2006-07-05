/* Copyright (c) 2004, University of Oslo, Norway
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
package org.vortikal.web.referencedata.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.BeanInitializationException;
import org.vortikal.repository.Repository;
import org.vortikal.repository.RepositoryException;
import org.vortikal.repository.Resource;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.repository.ResourceSorter;
import org.vortikal.web.RequestContext;
import org.vortikal.web.referencedata.ReferenceDataProvider;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.ServiceUnlinkableException;

/**
 * Directory listing model builder. Creates a model map 'collectionListing' with
 * a list of children and associated data for the
 * requested collection.
 * <p>
 * Configurable properties:
 * <ul>
 *  <li><code>repository</code> - the repository is required
 *   <li><code>retrieveForProcessing</code> - boolean indicating
 *   whether to retrieve resources using the
 *   <code>forProcessing</code> flag set to <code>false</code> or
 *   false. The default is <code>true</code>.
 *  <li><code>linkedServices</code> - map of services providing the
 *  possible operations for each child, e.g. delete,
 *  <li><code>browsingService</code> - the service used for linking to
 *  the children and the parent collection
 *  <li><code>contentTypeFilter</code> - an optional {@link Set} of
 *  content types that specify the resource types that are included in
 *  the listing. The special content type for collections is
 *  <code>application/x-vortex-collection</code>. Default is
 *  <code>null</code> (all resources are included).
 *  <li><code>contentTypeRegexpFilter</code> - an optional regular
 *  expression denoting content types that specify the resource types
 *  that are included in the listing. Default is <code>null</code>
 *  (all resources are included). This setting is incompatible with
 *  the <code>contentTypeFilter</code> setting.
 *  <li><code>childInfoItems</code> - list of info items to be
 *      displayed for the children. Valid items are <code>name</code>,
 *      <code>size</code>, <code>locked</code>,
 *      <code>content-type</code>, <code>owner</code> and
 *      <code>last-modified</code>. Default is <code>name</code>,
 *      <code>content-length</code>, <code>last-modified</code>.
 * </ul>
 * 
 * Possible input:
 * <ul> 
 *  <li><code>sort-by = (name | size | locked | content-type | owner | *  last-modified)</code>
 *      - default <code>name</code>
 *  <li><code>invert = (true | false)</code> - default <code>false</code>
 * </ul>
 * 
 * Model data provided:
 * <ul>
 *   <li><code>children</code> - list of this resource' child
 *   resources
 *   <li><code>linkedServiceNames</code> - the key set for the
 *   childLinks (linkedServices)
 *   <li><code>childLinks</code> (Map[]) - for every child, a map of
 *      links to all linkedServices specified on this ModelBuilder
 *      (with service name as key)
 *   <li><code>sortByLinks</code> (Map) - links to collectionlisting,
 *      sorted by the different configured childInfoItems
 *   <li><code>childInfoItems</code> - the configured child
 *      information to support sorting for
 *   <li><code>sortedBy</code> - the name of the info item the child
 *   list is sorted by
 *   <li><code>invertedSort</code> - is the child list inverted?
 *   <li><code>browsingLinks</code> - array of the links to the
 *   browsing service for each child
 *   <li><code>resourceURIs</code> - array of the resource URI for 
 *   each child (used in naming the checkboxes for copy/move 
 *   <li><code>parentURL</code> - link to the parent collection,
 *       generated using the <code>browsingService</code>. If the
 *       parent collection is the root, or the user is not allowed to
 *       navigate to the parent, the URL is set to <code>null</code>
 * </ul>
 */
public class CollectionListingProvider implements ReferenceDataProvider {

    public static final String DEFAULT_SORT_BY_PARAMETER = "name";

    
    private static final Set supportedResourceColumns = 
        new HashSet(Arrays.asList(new String[] {
                                      DEFAULT_SORT_BY_PARAMETER, 
                                      "content-length", 
                                      "last-modified",
                                      "locked", 
                                      "content-type", 
                                      "owner" }));
    
    private Repository repository;
    private Map linkedServices = new HashMap();
    private Service browsingService;
    private boolean retrieveForProcessing = false;
    private Set contentTypeFilter;
    private Pattern contentTypeRegexpFilter;
    

    public void setBrowsingService(Service browsingService) {
        this.browsingService = browsingService;
    }
    
    private String[] childInfoItems = 
        new String[] {DEFAULT_SORT_BY_PARAMETER, "content-length", "last-modified"};
    

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setLinkedServices(Map linkedServices)  {
        this.linkedServices = linkedServices;
    }

    public void setChildInfoItems(String[] childInfoItems)  {
        this.childInfoItems = childInfoItems;
    }

    public void setRetrieveForProcessing(boolean retrieveForProcessing) {
        this.retrieveForProcessing = retrieveForProcessing;
    }


    public void setContentTypeFilter(Set contentTypeFilter) {
        this.contentTypeFilter = contentTypeFilter;
    }
    
    public void setContentTypeRegexpFilter(String contentTypeRegexpFilter) {
        if (contentTypeRegexpFilter != null) {
            this.contentTypeRegexpFilter = Pattern.compile(contentTypeRegexpFilter);
        }
    }
    
    public void afterPropertiesSet() {
        if (this.repository == null) {
            throw new BeanInitializationException(
                "JavaBean Property 'repository' must be set");
        }

        if (this.browsingService == null) {
            throw new BeanInitializationException(
                    "JavaBean Property 'browsingService' must be set");    
        }

        if (this.contentTypeRegexpFilter != null && this.contentTypeFilter != null) {
            throw new BeanInitializationException(
                "JavaBean properties 'contentTypeRegexpFilter' and "
                + "'contentTypeFilter' cannot both be specified");
        }

        for (int i = 0; i < this.childInfoItems.length; i++) {
            String column = this.childInfoItems[i];
            if (! supportedResourceColumns.contains(column))
                throw new BeanInitializationException(
                    "JavaBean Property 'childInfoColumns' " +
                    "can only contain supported resource properties. Expected one of " 
                    + supportedResourceColumns + ", not '" + column + "'");
        }
    }

    
    public void referenceData(Map model, HttpServletRequest request)
        throws Exception {

        Map collectionListingModel = new HashMap();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        RequestContext requestContext = RequestContext.getRequestContext();
        String uri = requestContext.getResourceURI();
        String token = securityContext.getToken();
        collectionListingModel.put("childInfoItems", this.childInfoItems);
        Resource[] children = null;

        Resource resource = this.repository.retrieve(token, uri,
                                                this.retrieveForProcessing);
        if (!resource.isCollection()) {
            // Can't do anything unless resource is a collection
            return;
        }

        children = this.repository.listChildren(token, uri, true);

        children = filterChildren(children);

        // Sort children according to input parameters 
        String sortBy = request.getParameter("sort-by");
        boolean invertedSort = "true".equals(request.getParameter("invert"));
        boolean validSortByParameter = false;
        for (int i = 0; i < this.childInfoItems.length; i++) {
            if (this.childInfoItems[i].equals(sortBy)) validSortByParameter = true;
        }
        if (!validSortByParameter) sortBy = DEFAULT_SORT_BY_PARAMETER;
        sortChildren(children, sortBy, invertedSort);
        collectionListingModel.put("sortedBy", sortBy);
        collectionListingModel.put("invertedSort", new Boolean(invertedSort));
        collectionListingModel.put("children", children);
        
        List linkedServiceNames = new ArrayList();
        
        for (Iterator iter = this.linkedServices.keySet().iterator(); iter.hasNext();) {
            String linkName = (String) iter.next();
            linkedServiceNames.add(linkName);
        }
       
        collectionListingModel.put("linkedServiceNames", linkedServiceNames);
        
        Map[] childLinks = new HashMap[children.length];
        String[] browsingLinks = new String[children.length];
        
        for (int i = 0;  i < children.length; i++) {
            Map linkMap = new HashMap();
            
            for (Iterator iter = this.linkedServices.keySet().iterator(); iter.hasNext();) {
                String linkName = (String) iter.next();
                Service service = (Service) this.linkedServices.get(linkName);
                try {
                    String url = service.constructLink(children[i], 
                            securityContext.getPrincipal());
                    linkMap.put(linkName, url);
                } catch (ServiceUnlinkableException e) {
                    // do nothing
                }
            }
            childLinks[i] = linkMap; 
            
            try {
                browsingLinks[i] = this.browsingService.constructLink(
                    children[i], securityContext.getPrincipal());
            } catch (ServiceUnlinkableException e) {
                // do nothing
            }
        }
        collectionListingModel.put("childLinks", childLinks);
        collectionListingModel.put("browsingLinks", browsingLinks);

        Map sortByLinks = new HashMap(); 
        Principal principal = SecurityContext.getSecurityContext().getPrincipal();
        for (int i = 0; i < this.childInfoItems.length; i++) {
            String column = this.childInfoItems[i];
            Map parameters = new HashMap();
            parameters.put("sort-by", column);
            if (sortBy.equals(column) && !invertedSort) {
                parameters.put("invert", "true");
            }
            String url = this.browsingService.constructLink(resource, principal, parameters);
            sortByLinks.put(column, url);
        }
        collectionListingModel.put("sortByLinks", sortByLinks);

        String parentURL = null;
        if (resource.getParent() != null) {
            try {
                Resource parent = this.repository.retrieve(token, resource.getParent(), true);
                parentURL = this.browsingService.constructLink(parent, principal);
            } catch (RepositoryException e) {
                // Ignore
            } catch (AuthenticationException e) {
                // Ignore
            } catch (ServiceUnlinkableException e) {
                // Ignore
            }
        }
        collectionListingModel.put("parentURL", parentURL);
        model.put("collectionListing", collectionListingModel);
    }

    private Resource[] filterChildren(Resource[] children) {

        if (this.contentTypeFilter == null && this.contentTypeRegexpFilter == null) {
            return children;
        }
        
        List filteredChildren = new ArrayList();
        for (int i = 0; i < children.length; i++) {
                
            if (this.contentTypeFilter != null) {
                if (this.contentTypeFilter.contains(children[i].getContentType())) {
                    filteredChildren.add(children[i]);
                }
            } else {
                Matcher m = this.contentTypeRegexpFilter.matcher(
                    children[i].getContentType());
                if (m.matches()) {
                    filteredChildren.add(children[i]);
                }
            }
        }
        return (Resource[]) filteredChildren.toArray(
            new Resource[filteredChildren.size()]);
    }
    

    private void sortChildren(Resource[] children, String sortBy, boolean invert) {
        int order = ResourceSorter.ORDER_BY_NAME;

        if ("content-length".equals(sortBy)) {
            order = ResourceSorter.ORDER_BY_FILESIZE;
        }
        if ("last-modified".equals(sortBy)) {
            order = ResourceSorter.ORDER_BY_DATE;
        }
        if ("locked".equals(sortBy)) {
            order = ResourceSorter.ORDER_BY_LOCKS;
        }
        if ("content-type".equals(sortBy)) {
            order = ResourceSorter.ORDER_BY_CONTENT_TYPE;
        }
        if ("owner".equals(sortBy)) {
            order = ResourceSorter.ORDER_BY_OWNER;
        }
        
        ResourceSorter.sort(children, order, invert);
    }


}
