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

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Link;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFormatter;
import org.vortikal.web.RequestContext;

public class AudioVideoListingAsFeedController extends CollectionListingAsAtomFeed {

    private final Log logger = LogFactory.getLog(AudioVideoListingAsFeedController.class);

    @Override
    protected void addEntry(Feed feed, RequestContext requestContext, PropertySet result) {

        Repository repository = requestContext.getRepository();
        String token = requestContext.getSecurityToken();

        try {

            Entry entry = Abdera.getInstance().newEntry();

            Property publishedDateProp = getPublishDate(result);
            publishedDateProp = publishedDateProp == null ? result.getProperty(this.creationTimePropDef)
                    : publishedDateProp;
            String id = getId(result.getURI(), publishedDateProp, null);
            entry.setId(id);
            entry.addCategory(result.getResourceType());

            Property title = result.getProperty(this.titlePropDef);
            if (title != null) {
                entry.setTitle(title.getFormattedValue());
            }

            Link mediaLink = abdera.getFactory().newLink();
            mediaLink.setHref(viewService.constructLink(result.getURI()));
            mediaLink.setRel("enclosure");
            Resource mediaResource1 = repository.retrieve(token, result.getURI(), true);
            mediaLink.setMimeType(mediaResource1.getContentType());
            entry.addLink(mediaLink);

            String description = getDescription(result);
            if (description != null) {
                entry.setSummary(description);

            }

            Property publishDate = getPublishDate(result);
            if (publishDate != null) {
                entry.setPublished(publishDate.getDateValue());
            }

            Property updated = result.getProperty(this.lastModifiedPropDef);
            if (updated != null) {
                entry.setUpdated(updated.getDateValue());
            }

            Property author = getAuthor(result);
            if (author != null) {
                ValueFormatter vf = author.getDefinition().getValueFormatter();
                if (author.getDefinition().isMultiple()) {
                    for (Value v : author.getValues()) {
                        entry.addAuthor(vf.valueToString(v, "name", null));
                    }
                } else {
                    entry.addAuthor(author.getFormattedValue("name", null));
                }
            }

            Link link = abdera.getFactory().newLink();
            link.setHref(viewService.constructLink(result.getURI()));
            link.setRel("alternate");
            entry.addLink(link);

            feed.addEntry(entry);

        } catch (Throwable t) {
            // Don't break the entire feed if the entry breaks
            logger.warn("An error occured while creating feed entry for " + result.getURI(), t);
        }

    }
}
