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
import org.vortikal.security.SecurityContext;
import org.vortikal.web.service.Service;

public abstract class AtomFeedController implements Controller {

    private final Log logger = LogFactory.getLog(AtomFeedController.class);
    private static final String TAG_PREFIX = "tag:";

    protected Repository repository;
    protected Service viewService;
    protected Abdera abdera;
    protected ResourceTypeTree resourceTypeTree;
    protected PropertyTypeDefinition publishDatePropDef;
    protected int entryCountLimit = 200;

    private PropertyTypeDefinition titlePropDef;
    private PropertyTypeDefinition lastModifiedPropDef;
    private String authorPropDefPointer;
    private String introductionPropDefPointer;
    private String picturePropDefPointer;
    private String mediaPropDefPointer;

    protected abstract Feed createFeed(HttpServletRequest request, HttpServletResponse response, String token)
            throws Exception;

    protected abstract Property getPublishDate(PropertySet resource);

    // To be overridden where necessary
    protected Date getLastModified(Resource collection) {
        return collection.getLastModified();
    }

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String token = SecurityContext.getSecurityContext().getToken();
        Feed feed = createFeed(request, response, token);
        if (feed != null) {
            response.setContentType("application/atom+xml;charset=utf-8");
            feed.writeTo("prettyxml", response.getWriter());
        }
        return null;
    }

    protected String getTitle(Resource collection) {
        String feedTitle = collection.getTitle();
        if (Path.ROOT.equals(collection.getURI())) {
            feedTitle = this.repository.getId();
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
        Property published = collection.getProperty(this.publishDatePropDef);
        feed.setId(getId(collection.getURI(), published, getFeedPrefix()));
        feed.addLink(viewService.constructLink(collection.getURI()), "alternate");

        feed.setTitle(feedTitle);

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
                String val = picture.getFormattedValue("thumbnail", Locale.getDefault());
                feed.setLogo(val);
            }
        }

        Date lastModified = this.getLastModified(collection);
        if (lastModified != null) {
            feed.setUpdated(lastModified);
        }
        return feed;
    }

    protected void addEntry(Feed feed, String token, PropertySet result) {
        try {

            Entry entry = Abdera.getInstance().newEntry();

            String id = getId(result.getURI(), result.getProperty(this.publishDatePropDef), null);
            entry.setId(id);
            entry.addCategory(result.getResourceType());

            Property title = result.getProperty(this.titlePropDef);
            if (title != null) {
                entry.setTitle(title.getFormattedValue());
            }
            String type = result.getResourceType();
            // Add introduction and/or pic as xhtml if resource is event or
            // article...
            if (type != null
                    && (type.equals("event") || type.equals("article") || type.equals("structured-article")
                            || type.equals("structured-event") || type.equals("structured-project"))) {
                String summary = prepareSummary(result);
                entry.setSummaryAsXhtml(summary);
                // ...add description as plain text else
            } else {
                String description = getDescription(result);
                if (description != null) {
                    entry.setSummary(description);
                }
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
            } else {
                entry.addAuthor("");
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
                    logger.error("An error occured while setting media link for feed entry, " + result.getURI() + ": "
                            + t.getMessage());
                }
            }

            feed.addEntry(entry);

        } catch (Throwable t) {
            // Don't break the entire feed if the entry breaks
            logger.error("An error occured while creating feed entry for " + result.getURI() + ": " + t.getMessage());
        }
    }

    protected String prepareSummary(PropertySet resource) {

        StringBuilder sb = new StringBuilder();
        String summary = getIntroduction(resource);

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
            String imgPath = picture.getFormattedValue("thumbnail", Locale.getDefault());
            String imgAlt = getImageAlt(imgPath);
            sb.append("<img src=\"" + imgPath + "\" alt=\"" + imgAlt + "\"/>");
        }

        if (summary != null) {
            sb.append(summary);
        }
        return sb.toString();
    }

    private Property getMediaRef(PropertySet resource) {
        PropertyTypeDefinition mediaPropDef = this.resourceTypeTree
                .getPropertyDefinitionByPointer(this.mediaPropDefPointer);
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

    private Property getAuthor(PropertySet resource) {
        PropertyTypeDefinition authorPropDef = this.resourceTypeTree
                .getPropertyDefinitionByPointer(this.authorPropDefPointer);
        if (authorPropDef == null) {
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
        return resource.getURI().extend(val);
    }

    protected String getId(Path resourceUri, Property published, String prefix) throws URIException,
            UnsupportedEncodingException {
        String host = viewService.constructURL(resourceUri).getHost();
        StringBuilder sb = new StringBuilder(TAG_PREFIX);
        sb.append(host + ",");
        sb.append(published.getFormattedValue("iso-8601-short", null) + ":");
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

    public void setPublishDatePropDef(PropertyTypeDefinition publishDatePropDef) {
        this.publishDatePropDef = publishDatePropDef;
    }

}
