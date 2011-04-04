package org.vortikal.web.display.collection;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.xml.namespace.QName;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Link;
import org.apache.commons.httpclient.URIException;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFormatter;
import org.vortikal.web.RequestContext;

public class AudioVideoListingAsFeedController extends CollectionListingAsAtomFeed {

    QName itunesSummary = new QName("http://www.itunes.com/dtds/podcast-1.0.dtd", "summary", "itunes");
    QName itunesImage = new QName("http://www.itunes.com/dtds/podcast-1.0.dtd", "image", "itunes");
    QName itunesKeywords = new QName("http://www.itunes.com/dtds/podcast-1.0.dtd", "keywords", "itunes");
    QName itunesAuthor = new QName("http://www.itunes.com/dtds/podcast-1.0.dtd", "author", "itunes");

    @Override
    protected Feed populateFeed(Resource collection, String feedTitle, boolean showIntroduction) throws IOException,
            URIException, UnsupportedEncodingException {

        Feed feed = super.populateFeed(collection, feedTitle, showIntroduction);

        feed.addExtension(itunesSummary);
        feed.addExtension(itunesImage);
        feed.addExtension(itunesKeywords);
        feed.addExtension(itunesAuthor);

        return feed;
    }

    @Override
    protected void addEntry(Feed feed, RequestContext requestContext, PropertySet result) {

        Repository repository = requestContext.getRepository();
        String token = requestContext.getSecurityToken();

        try {

            Entry entry = Abdera.getInstance().newEntry();

            Element newElement = abdera.getFactory().newElement(itunesSummary);
            entry.addExtension(newElement);

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

            Property picture = this.getPicture(result);
            if (picture != null) {
                String imageRef = picture.getStringValue();
                if (!imageRef.startsWith("/") && !imageRef.startsWith("https://") && !imageRef.startsWith("https://")) {
                    try {
                        imageRef = result.getURI().getParent().expand(imageRef).toString();
                        picture.setValue(new Value(imageRef, PropertyType.Type.STRING));
                    } catch (Throwable t) {
                    }
                }
                Element newElement1 = abdera.getFactory().newElement(itunesImage);
                newElement1.setText(picture.toString());
                entry.addExtension(newElement);
            }

            // TODO: htmlDescription?

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

            Property mediaRef = getMediaRef(result);
            if (mediaRef != null) {
                try {
                    Link mediaLink = abdera.getFactory().newLink();
                    Path propRef = getPropRef(result, mediaRef.getStringValue());
                    mediaLink.setHref(viewService.constructLink(propRef));
                    mediaLink.setRel("enclosure");
                    Resource mediaResource = repository.retrieve(token, propRef, true);
                    mediaLink.setMimeType(mediaResource.getContentType());
                    entry.addLink(mediaLink);
                } catch (Throwable t) {
                    // Don't break the entire entry if media link breaks
                    logger.warn("An error occured while setting media link for feed entry, " + result.getURI(), t);
                }
            }

            feed.addEntry(entry);

        } catch (Throwable t) {
            // Don't break the entire feed if the entry breaks
            logger.warn("An error occured while creating feed entry for " + result.getURI(), t);
        }

    }
}
