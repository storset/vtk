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

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Link;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.openxri.IRIUtils;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.HtmlValueFormatter;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFormatter;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.controller.search.SearchComponent;
import org.vortikal.web.service.Service;

public class CollectionListingAsAtomFeed implements Controller {

    private static Namespace NS = Namespace.DEFAULT_NAMESPACE;

    private Repository repository;
    private Service viewService;
    private Abdera abdera;
    private SearchComponent searchComponent;

    public ModelAndView handleRequest(HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        Feed feed = abdera.newFeed();

        String token = SecurityContext.getSecurityContext().getToken();
        String uri = RequestContext.getRequestContext().getResourceURI();

        Resource collection = this.repository.retrieve(token, uri, true);

        Property published = collection.getProperty(NS, PropertyType.CREATIONTIME_PROP_NAME);
        feed.setId(getId(uri, published));
        feed.setTitle(collection.getTitle());
        
        String subTitle = getIntroduction(collection);
        if (subTitle != null) {
            feed.setSubtitleAsXhtml(subTitle);
        } else {
            subTitle = getDescription(collection);
            if (subTitle != null) {
                feed.setSubtitle(subTitle);
            }
        }
        
        Property picture = collection.getProperty(NS, PropertyType.PICTURE_PROP_NAME);
        if (picture != null) {
            feed.setLogo(viewService.constructLink(picture.getStringValue()));
        }
        
        feed.setUpdated(collection.getLastModified());
        //feed.addAuthor(resource.getOwner().getDescription());
        feed.addLink(viewService.constructLink(uri), "alternate");
        
        Map<String, Object> searchResult = searchComponent.execute(request, collection);
        @SuppressWarnings("unchecked")
        List<PropertySet> files = (List<PropertySet>) searchResult.get("files");
        for (PropertySet child : files) {
            Entry entry = feed.addEntry();
            
            entry.setId(getId(child.getURI(), child.getProperty(NS, PropertyType.CREATIONTIME_PROP_NAME)));
            entry.addCategory(child.getResourceType());
            
            Property prop = child.getProperty(NS, PropertyType.TITLE_PROP_NAME);
            entry.setTitle(prop.getFormattedValue());

            String summary = getIntroduction(child);
            if (summary != null) {
                entry.setSummaryAsXhtml(summary);
            } else {
                summary = getDescription(child);
                if (subTitle != null) {
                    entry.setSummary(summary);
                }
            }

            if (searchComponent.getPublishedDatePropDef() != null) {
                prop = child.getProperty(searchComponent.getPublishedDatePropDef());
                entry.setPublished(prop.getDateValue());
            }

            prop = child.getProperty(NS, PropertyType.LASTMODIFIED_PROP_NAME);
            entry.setUpdated(prop.getDateValue());

            if (searchComponent.getAuthorPropDef() != null) {
                prop = child.getProperty(searchComponent.getAuthorPropDef());
                if (prop != null) {
                    ValueFormatter vf = prop.getDefinition().getValueFormatter();
                    if (prop.getDefinition().isMultiple()) {
                        for (Value v: prop.getValues()) {
                            entry.addAuthor(vf.valueToString(v, "name", null));
                        }
                    } else {
                        entry.addAuthor(prop.getFormattedValue("name", null));
                    }
                }
            }

            Link link = abdera.getFactory().newLink();
            prop = child.getProperty(NS, PropertyType.MEDIA_PROP_NAME);
            if (prop != null) {
                try {
                    Resource mediaResource = repository.retrieve(token, prop.getStringValue(), true);
                    link.setHref(viewService.constructLink(prop.getStringValue()));
                    link.setRel("enclosure");
                    link.setMimeType(mediaResource.getContentType());
                    entry.addLink(link);
                } catch (Throwable t) { }
            }

            try {
                link = abdera.getFactory().newLink();
                link.setHref(viewService.constructLink(child.getURI()));
                link.setRel("alternate");
                entry.addLink(link);
            } catch (Throwable t) { }
        }

        response.setContentType("application/atom+xml;charset=utf-8");
        feed.writeTo("prettyxml", response.getWriter());
        return null;
    }

    private String getIntroduction(PropertySet resource) {
        Property prop = resource.getProperty(NS, PropertyType.INTRODUCTION_PROP_NAME);
        return prop != null ? prop.getFormattedValue() : null;
    }
    
    private String getDescription(PropertySet resource) {
        Namespace NS_CONTENT = Namespace.getNamespace("http://www.uio.no/content");
        Property prop = resource.getProperty(NS_CONTENT, PropertyType.DESCRIPTION_PROP_NAME);
        return prop != null ? prop.getFormattedValue(HtmlValueFormatter.FLATTENED_FORMAT, null) : null;
    }

    /**
     * @param resourceUri The uri of the resource
     * @param published The published date of the resource
     * @return The id to set for an atomfeed or entry
     * @throws URIException If an error occurs while parsing/encoding the string 
     *         representing the URI
     * @throws UnsupportedEncodingException If the default (UTF-8) encoding used for 
     *         transformation from an URI to IRI is not supported
     */
    private String getId(String resourceUri, Property published) throws URIException, UnsupportedEncodingException {
        String host = viewService.constructURL(resourceUri).getHost();
        StringBuilder sb = new StringBuilder("tag:");
        sb.append(host + ",");
        sb.append(published.getFormattedValue("iso-8601-short", null) + ":");
        resourceUri = resourceUri.replaceAll("[#% ]", "").replace("[", "").replace("]", "");
        resourceUri = URIUtil.encode(resourceUri, null);
        String iriString = IRIUtils.URItoIRI(resourceUri);
        sb.append(iriString);
        return sb.toString();
    }
    
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @Required
    public void setViewService(Service viewService) {
        this.viewService = viewService;
    }

    @Required
    public void setAbdera(Abdera abdera) {
        this.abdera = abdera;
    }

    @Required
    public void setSearchComponent(SearchComponent searchComponent) {
        this.searchComponent = searchComponent;
    }

}
