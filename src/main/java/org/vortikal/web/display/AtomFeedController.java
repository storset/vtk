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
package org.vortikal.web.display;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Link;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openxri.IRIUtils;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.MultiHostSearcher;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.HtmlValueFormatter;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFormatter;
import org.vortikal.text.html.HtmlFragment;
import org.vortikal.text.html.HtmlUtil;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

public abstract class AtomFeedController implements Controller {

    private final Log logger = LogFactory.getLog(AtomFeedController.class);
    public static final String TAG_PREFIX = "tag:";

    protected Service viewService;
    protected Abdera abdera;
    protected ResourceTypeTree resourceTypeTree;
    protected PropertyTypeDefinition publishDatePropDef;
    protected int entryCountLimit = 200;

    protected PropertyTypeDefinition titlePropDef;
    protected PropertyTypeDefinition lastModifiedPropDef;
    protected PropertyTypeDefinition creationTimePropDef;

    private HtmlUtil htmlUtil;
    private String authorPropDefPointer;
    private String introductionPropDefPointer;
    private String picturePropDefPointer;
    private String mediaPropDefPointer;
    private String structuredAuthorPropDefPointer;
    private String structuredIntroductionPropDefPointer;
    private String structuredPicturePropDefPointer;
    private String structuredMediaPropDefPointer;

    protected abstract Feed createFeed(RequestContext requestContext) throws Exception;

    protected abstract Property getPublishDate(PropertySet resource);

    // To be overridden where necessary
    protected Date getLastModified(PropertySet resource) {
        return resource.getProperty(lastModifiedPropDef).getDateValue();
    }

    // To be overridden where necessary
    protected void setFeedEntrySummary(Entry entry, PropertySet result) throws Exception {
        // Hack for project resource type
        String type = result.getResourceType();
        if (type != null && type.equals("structured-project")) {
            HtmlFragment summary = prepareSummary(result);
            if (summary != null) {
                entry.setSummaryAsXhtml(summary.getStringRepresentation());
            }
            // ...add description as plain text else
        } else {
            String description = getDescription(result);
            if (description != null) {
                entry.setSummary(description);
            }
        }
    }

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        Feed feed = this.createFeed(requestContext);
        if (feed != null) {
            response.setContentType("application/atom+xml;charset=utf-8");
            feed.writeTo("prettyxml", response.getWriter());
        }
        return null;
    }

    protected String getTitle(Resource collection, RequestContext requestContext) {
        String feedTitle = collection.getTitle();
        if (Path.ROOT.equals(collection.getURI())) {
            feedTitle = requestContext.getRepository().getId();
        }
        return feedTitle;
    }

    protected Feed populateFeed(Resource collection, String feedTitle) throws IOException, URIException,
            UnsupportedEncodingException {
        return this.populateFeed(collection, feedTitle, true);
    }

    protected Feed populateFeed(Resource collection, String feedTitle, boolean showIntroduction) throws IOException,
            URIException, UnsupportedEncodingException {

        Feed feed = abdera.newFeed();

        Property publishedDateProp = getPublishDate(collection);
        publishedDateProp = publishedDateProp == null ? collection.getProperty(this.creationTimePropDef)
                : publishedDateProp;
        feed.setId(getId(collection.getURI(), publishedDateProp, getFeedPrefix()));
        feed.addLink(viewService.constructLink(collection.getURI()), "alternate");

        feed.setTitle(feedTitle);
        feed.addAuthor("");

        if (showIntroduction) {
            String subTitle = getIntroduction(collection);
            if (subTitle != null) {
                feed.setSubtitleAsXhtml(subTitle);
            } else {
                subTitle = getDescription(collection);
                if (subTitle != null) {
                    feed.setSubtitle(subTitle);
                }
            }

            Property picture = getPicture(collection);
            if (picture != null) {
                String val = picture.getFormattedValue(PropertyType.THUMBNAIL_PROP_NAME, Locale.getDefault());
                feed.setLogo(val);
            }
        }

        Date lastModified = getLastModified(collection);
        if (lastModified != null) {
            feed.setUpdated(lastModified);
        }
        return feed;
    }

    protected void addEntry(Feed feed, RequestContext requestContext, PropertySet result) {
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

            // Set the summary
            this.setFeedEntrySummary(entry, result);

            Property publishDate = getPublishDate(result);
            if (publishDate != null) {
                entry.setPublished(publishDate.getDateValue());
            }

            Date updated = getLastModified(result);
            if (updated != null) {
                entry.setUpdated(updated);
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
            String urlString = viewService.constructLink(result.getURI());
            Property urlProp = result.getProperty(Namespace.DEFAULT_NAMESPACE, MultiHostSearcher.URL_PROP_NAME);
            if (urlProp != null) {
                urlString = URL.parse(urlProp.getStringValue()).toString();
            }
            link.setHref(urlString);
            link.setRel("alternate");
            entry.addLink(link);

            Property mediaRef = getMediaRef(result);
            if (mediaRef != null) {
                try {
                    Link mediaLink = abdera.getFactory().newLink();
                    Path propRef = getPropRef(result, mediaRef.getStringValue());
                    if (propRef != null) {
                        mediaLink.setHref(viewService.constructLink(propRef));
                        mediaLink.setRel("enclosure");
                        Repository repository = requestContext.getRepository();
                        String token = requestContext.getSecurityToken();
                        Resource mediaResource = repository.retrieve(token, propRef, true);
                        mediaLink.setMimeType(mediaResource.getContentType());
                        entry.addLink(mediaLink);
                    }
                } catch (Throwable t) {
                    // Don't break the entire entry if media link breaks
                    logger.warn("An error occured while setting media link for feed entry, " + result.getURI() + ": "
                            + t.getMessage());
                }
            }

            feed.addEntry(entry);

        } catch (Throwable t) {
            // Don't break the entire feed if the entry breaks
            logger.warn("An error occured while creating feed entry for " + result.getURI(), t);
        }
    }

    protected HtmlFragment prepareSummary(PropertySet resource) throws Exception {
        StringBuilder sb = new StringBuilder();

        Property picture = this.getPicture(resource);
        if (picture != null) {
            String imageRef = picture.getStringValue();
            if (!imageRef.startsWith("/") && !imageRef.startsWith("https://") && !imageRef.startsWith("https://")) {
                try {
                    imageRef = resource.getURI().getParent().expand(imageRef).toString();
                    picture.setValue(new Value(imageRef, PropertyType.Type.STRING));
                } catch (Throwable t) {
                }
            }
            String imgPath = picture.getFormattedValue(PropertyType.THUMBNAIL_PROP_NAME, Locale.getDefault());
            String imgAlt = getImageAlt(imgPath);

            sb.append("<img src=\"" + HtmlUtil.escapeHtmlString(imgPath) + "\" alt=\""
                    + HtmlUtil.escapeHtmlString(imgAlt) + "\"/>");
        }

        String intro = getIntroduction(resource);
        if (intro != null) {
            sb.append(intro);
        }

        URL baseURL = viewService.constructURL(resource.getURI());

        if (sb.length() > 0) {
            HtmlFragment summary = htmlUtil.linkResolveFilter(sb.toString(), baseURL, RequestContext
                    .getRequestContext().getRequestURL());
            return summary;
        } else {
            return null;
        }
    }

    protected Property getMediaRef(PropertySet resource) {
        PropertyTypeDefinition mediaPropDef = this.resourceTypeTree
                .getPropertyDefinitionByPointer(this.mediaPropDefPointer);
        if (mediaPropDef != null) {
            Property mediaProp = resource.getProperty(mediaPropDef);
            if (mediaProp != null)
                return mediaProp;
        }
        mediaPropDef = this.resourceTypeTree.getPropertyDefinitionByPointer(this.structuredMediaPropDefPointer);
        if (mediaPropDef != null) {
            Property mediaProp = resource.getProperty(mediaPropDef);
            return mediaProp;
        }
        return null;
    }

    protected Property getDefaultPublishDate(PropertySet result) {
        if (this.publishDatePropDef != null) {
            return result.getProperty(this.publishDatePropDef);
        }
        return null;
    }

    protected Property getAuthor(PropertySet resource) {
        PropertyTypeDefinition authorPropDef = this.resourceTypeTree
                .getPropertyDefinitionByPointer(this.authorPropDefPointer);
        if (authorPropDef != null) {
            Property author = resource.getProperty(authorPropDef);
            if (author != null)
                return author;
        }

        authorPropDef = this.resourceTypeTree.getPropertyDefinitionByPointer(this.structuredAuthorPropDefPointer);
        if (authorPropDef != null) {
            Property author = resource.getProperty(authorPropDef);
            return author;
        }

        return null;
    }

    protected String getIntroduction(PropertySet resource) {
        PropertyTypeDefinition introductionPropDef = this.resourceTypeTree
                .getPropertyDefinitionByPointer(this.introductionPropDefPointer);
        Property introductionProp = null;
        if (introductionPropDef != null) {
            introductionProp = resource.getProperty(introductionPropDef);
            if (introductionProp != null)
                return introductionProp.getFormattedValue();
        }

        introductionPropDef = this.resourceTypeTree
                .getPropertyDefinitionByPointer(this.structuredIntroductionPropDefPointer);
        if (introductionPropDef != null) {
            introductionProp = resource.getProperty(introductionPropDef);

        }
        return introductionProp != null ? introductionProp.getFormattedValue() : null;
    }

    protected String getDescription(PropertySet resource) {
        Namespace NS_CONTENT = Namespace.getNamespace("http://www.uio.no/content");
        Property prop = resource.getProperty(NS_CONTENT, PropertyType.DESCRIPTION_PROP_NAME);
        return prop != null ? prop.getFormattedValue(HtmlValueFormatter.FLATTENED_FORMAT, null) : null;
    }

    protected Path getPropRef(PropertySet resource, String val) {
        if (val.startsWith("/")) {
            return Path.fromString(val);
        }
        if (val.startsWith("http://") || val.startsWith("https://")) {
            // Only relative references are supported:
            return null;
        }
        Property collectionProp = resource.getProperty(Namespace.DEFAULT_NAMESPACE, PropertyType.COLLECTION_PROP_NAME);
        if (collectionProp != null && collectionProp.getBooleanValue() == true) {
            return resource.getURI().extend(val);
        }
        return resource.getURI().getParent().extend(val);
    }

    protected String getId(Path resourceUri, Property publishedDateProp, String prefix) throws URIException,
            UnsupportedEncodingException {
        String host = viewService.constructURL(resourceUri).getHost();
        StringBuilder sb = new StringBuilder(TAG_PREFIX);
        sb.append(host + ",");
        sb.append(publishedDateProp.getFormattedValue("iso-8601-short", null) + ":");
        if (prefix != null) {
            sb.append(prefix);
        }
        String uriString = resourceUri.toString();
        // Remove any invalid character before decoding
        uriString = removeInvalid(uriString);
        uriString = URIUtil.decode(uriString);
        // Remove any unknown character after decoding
        uriString = removeInvalid(uriString);
        uriString = URIUtil.encode(uriString, null);
        String iriString = IRIUtils.URItoIRI(uriString);
        sb.append(iriString);
        return sb.toString();
    }

    protected String getFeedPrefix() {
        return null;
    }

    protected Property getPicture(PropertySet resource) {
        PropertyTypeDefinition picturePropDef = this.resourceTypeTree
                .getPropertyDefinitionByPointer(this.picturePropDefPointer);
        if (picturePropDef != null) {
            Property pic = resource.getProperty(picturePropDef);
            if (pic != null)
                return pic;
        }
        picturePropDef = this.resourceTypeTree.getPropertyDefinitionByPointer(this.structuredPicturePropDefPointer);
        if (picturePropDef != null) {
            Property pic = resource.getProperty(picturePropDef);
            return pic;
        }
        return null;
    }

    private String removeInvalid(String s) {
        return s.replaceAll("[#%?\\[\\] ]", "");
    }

    private String getImageAlt(String imgPath) {
        try {
            return imgPath.substring(imgPath.lastIndexOf("/") + 1, imgPath.lastIndexOf("."));
        } catch (Throwable t) {
            // Don't do anything special, imgAlt isn't all that important
            return "feed_image";
        }
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
    public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
    }

    @Required
    public void setTitlePropDef(PropertyTypeDefinition titlePropDef) {
        this.titlePropDef = titlePropDef;
    }

    @Required
    public void setLastModifiedPropDef(PropertyTypeDefinition lastModifiedPropDef) {
        this.lastModifiedPropDef = lastModifiedPropDef;
    }

    @Required
    public void setAuthorPropDefPointer(String authorPropDefPointer) {
        this.authorPropDefPointer = authorPropDefPointer;
    }

    @Required
    public void setIntroductionPropDefPointer(String introductionPropDefPointer) {
        this.introductionPropDefPointer = introductionPropDefPointer;
    }

    @Required
    public void setPicturePropDefPointer(String picturePropDefPointer) {
        this.picturePropDefPointer = picturePropDefPointer;
    }

    @Required
    public void setMediaPropDefPointer(String mediaPropDefPointer) {
        this.mediaPropDefPointer = mediaPropDefPointer;
    }

    @Required
    public void setCreationTimePropDef(PropertyTypeDefinition creationTimePropDef) {
        this.creationTimePropDef = creationTimePropDef;
    }

    @Required
    public void setPublishDatePropDef(PropertyTypeDefinition publishDatePropDef) {
        this.publishDatePropDef = publishDatePropDef;
    }

    @Required
    public void setStructuredAuthorPropDefPointer(String structuredAuthorPropDefPointer) {
        this.structuredAuthorPropDefPointer = structuredAuthorPropDefPointer;
    }

    @Required
    public void setStructuredIntroductionPropDefPointer(String structuredIntroductionPropDefPointer) {
        this.structuredIntroductionPropDefPointer = structuredIntroductionPropDefPointer;
    }

    @Required
    public void setStructuredPicturePropDefPointer(String structuredPicturePropDefPointer) {
        this.structuredPicturePropDefPointer = structuredPicturePropDefPointer;
    }

    @Required
    public void setStructuredMediaPropDefPointer(String structuredMediaPropDefPointer) {
        this.structuredMediaPropDefPointer = structuredMediaPropDefPointer;
    }

    @Required
    public void setHtmlUtil(HtmlUtil htmlUtil) {
        this.htmlUtil = htmlUtil;
    }

}
