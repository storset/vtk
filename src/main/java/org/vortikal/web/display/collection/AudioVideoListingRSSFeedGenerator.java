/* Copyright (c) 2011, University of Oslo, Norway
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
package org.vortikal.web.display.collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.MultiHostSearcher;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFormatter;
import org.vortikal.web.RequestContext;
import org.vortikal.web.display.feed.RSSFeedGenerator;
import org.vortikal.web.search.Listing;
import org.vortikal.web.search.SearchComponent;
import org.vortikal.web.service.URL;

import com.sun.syndication.feed.synd.SyndCategoryImpl;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;

public class AudioVideoListingRSSFeedGenerator extends RSSFeedGenerator {

    private SearchComponent searchComponent;

    @Override
    protected List<SyndEntry> getFeedEntries(Resource feedScope) throws Exception {

        Listing entryElements = searchComponent.execute(RequestContext.getRequestContext().getServletRequest(),
                feedScope, 1, 500, 0);

        List<SyndEntry> entries = new ArrayList<SyndEntry>();
        for (PropertySet ps : entryElements.getFiles()) {

            SyndEntry entry = new SyndEntryImpl();
            entry.setTitle(ps.getProperty(Namespace.DEFAULT_NAMESPACE, "title").getStringValue());

            String urlString = viewService.constructLink(ps.getURI());
            Property urlProp = ps.getProperty(Namespace.DEFAULT_NAMESPACE, MultiHostSearcher.URL_PROP_NAME);
            if (urlProp != null) {
                urlString = URL.parse(urlProp.getStringValue()).toString();
            }
            entry.setLink(urlString);

            Property author = ps.getProperty(Namespace.DEFAULT_NAMESPACE, "author");
            if (author == null) {
                author = ps.getProperty(Namespace.STRUCTURED_RESOURCE_NAMESPACE, "author");
            }
            if (author != null) {
                ValueFormatter vf = author.getDefinition().getValueFormatter();
                if (author.getDefinition().isMultiple()) {
                    List<String> authors = new ArrayList<String>();
                    for (Value v : author.getValues()) {
                        authors.add(vf.valueToString(v, "name", null));
                    }
                    entry.setAuthors(authors);
                } else {
                    entry.setAuthor(author.getFormattedValue("name", null));
                }
            }

            SyndContentImpl description = null;
            Property introductionProp = ps.getProperty(Namespace.DEFAULT_NAMESPACE, "video-description");
            if (introductionProp == null) {
                introductionProp = ps.getProperty(Namespace.DEFAULT_NAMESPACE, "audio-description");
            }
            if (introductionProp != null) {
                description = new SyndContentImpl();
                description.setType("text/html");
                description.setValue(introductionProp.getFormattedValue());
            }

            if (description != null) {
                entry.setDescription(description);
            }

            SyndCategoryImpl category = new SyndCategoryImpl();
            category.setName(ps.getResourceType());
            entry.setCategories(Arrays.asList(category));

            entry.setUpdatedDate(ps.getProperty(lastModifiedPropDef).getDateValue());
            entry.setPublishedDate(ps.getProperty(publishDatePropDef).getDateValue());
            entries.add(entry);

        }

        return entries;
    }

    @Required
    public void setSearchComponent(SearchComponent searchComponent) {
        this.searchComponent = searchComponent;
    }

}
