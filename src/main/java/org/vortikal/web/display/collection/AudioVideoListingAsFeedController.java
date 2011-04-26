package org.vortikal.web.display.collection;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Link;
import org.apache.commons.httpclient.URIException;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFormatter;
import org.vortikal.web.RequestContext;

public class AudioVideoListingAsFeedController extends CollectionListingAsAtomFeed {


    @Override
    protected Feed populateFeed(Resource collection, String feedTitle, boolean showIntroduction) throws IOException,
            URIException, UnsupportedEncodingException {

        Feed feed = super.populateFeed(collection, feedTitle, showIntroduction);
        return feed;
    }

    @Override
    protected void addEntry(Feed feed, RequestContext requestContext, PropertySet result) {

        Repository repository = requestContext.getRepository();
        String token = requestContext.getSecurityToken();

        try {

            Entry entry = Abdera.getInstance().newEntry();

            Property publishedDateProp = getPublishDate(result);
            publishedDateProp = publishedDateProp == null ? result.getProperty(getCreationTimePropDef())
                    : publishedDateProp;
            String id = getId(result.getURI(), publishedDateProp, null);
            entry.setId(id);
            entry.addCategory(result.getResourceType());

            Property title = result.getProperty(getTitlePropDef());
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

            Property updated = result.getProperty(getLastModifiedPropDef());
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
