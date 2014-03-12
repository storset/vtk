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
package org.vortikal.web.display.media;

import java.util.Map;
import org.springframework.beans.factory.annotation.Required;

import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.security.AuthenticationException;
import org.vortikal.util.repository.MimeHelper;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

/**
 * XXX This class needs complete documentation on model attributes for the two
 * main "provider" methods.
 * 
 * <p>Class adds model data required for displaying media files
 * in web pages using an embedded player/placeholder and download link. Used
 * both by component for insertion at arbitrary locations in HTML documents or
 * the web page view of Vortex media resources.
 */
public class MediaPlayer {

    private Service viewService;
    private Service thumbnailService;
    private PropertyTypeDefinition posterImagePropDef;
    private PropertyTypeDefinition thumbnailPropDef;

    /**
     * Provided model data:
     * <ul>
     *   <li><code>mediaResource</code> - if media reference points to a local
     * resource, then the {@link Resource} will be provided under this key if it
     * could be retrieved from the repository.</li>
     *   <li><code>media</code> - a {@link URL} instance pointing to the media.
     *   <li>TODO complete me</li>
     * </ul>
     * 
     * @param model model to add data to
     * @param mediaRef the media resource URI as a string
     * @param height height of inserted video
     * @param width width of inserted video
     * @param autoplay whether to setup player to start automatically or not.
     * @param contentType 
     * @param streamType
     * @param poster
     * @param showDL
     * @throws AuthorizationException 
     */
    public void addMediaPlayer(Map<String, Object> model, String mediaRef, String height, String width,
            String autoplay, String contentType, String streamType, String poster, String showDL)
            throws AuthorizationException {

        if (URL.isEncoded(mediaRef)) {
            mediaRef = URL.decode(mediaRef);
        }

        Resource mediaResource = null;
        try {
            mediaResource = getLocalResource(mediaRef);
        } catch (AuthorizationException e) {
            return; // not able to read local resource - abort
        } catch (AuthenticationException e) {
            return; // not able to read local resource - abort
        } catch (Exception e) {
            // ignore
        }
        
        if (mediaResource != null) {
            model.put("mediaResource", mediaResource);
        }

        if ((height != null && !"".equals(height)) && (width != null && !"".equals(width))) {
            model.put("height", height);
            model.put("width", width);
        }

        if (autoplay != null && !autoplay.isEmpty())
            model.put("autoplay", autoplay);

        if (streamType != null && !streamType.isEmpty())
            model.put("streamType", streamType);

        model.put("showDL", showDL != null && showDL.equalsIgnoreCase("true") ? "true" : "false");

        if (poster != null && !poster.isEmpty())
            model.put("poster", poster);
        else
            addPosterUrl(mediaResource, model);

        if (contentType != null && !contentType.isEmpty()) {
            model.put("contentType", contentType);
        } else if (mediaResource != null) {
            model.put("contentType", mediaResource.getContentType());
        } else {
            model.put("contentType", MimeHelper.map(mediaRef));
        }
        model.put("extension", MimeHelper.findExtension(mediaRef));
        model.put("nanoTime", System.nanoTime());

        addMediaUrl(mediaRef, model);
    }

    /**
     * Provided model data:
     * <ul>
     *   <li><code>mediaResource</code> - if media reference points to a local
     * resource, then the {@link Resource} will be provided under this key if it
     * could successfully be retrieved from the repository.</li>
     *   <li><code>media</code> - a {@link URL} instance pointing to the media.
     *   <li>TODO complete me</li>
     * </ul>
     * 
     **/ 
    public void addMediaPlayer(Map<String, Object> model, String mediaRef) throws AuthorizationException {

        if (URL.isEncoded(mediaRef)) {
            mediaRef = URL.decode(mediaRef);
        }

        Resource mediaResource = null;
        try {
            mediaResource = getLocalResource(mediaRef);
        } catch (AuthorizationException e) {
            return; // not able to read local resource - abort
        } catch (AuthenticationException e) {
            return; // not able to read local resource - abort
        } catch (Exception e) {
            // ignore
        }

        if (mediaResource != null) {
            model.put("mediaResource", mediaResource);
        }

        addPosterUrl(mediaResource, model);
        model.put("extension", MimeHelper.findExtension(mediaRef));

        if (mediaResource != null) {
            model.put("contentType", mediaResource.getContentType());
        } else {
            model.put("contentType", MimeHelper.map(mediaRef));
        }

        model.put("nanoTime", System.nanoTime());
        
        addMediaUrl(mediaRef, model);
    }

    private Resource getLocalResource(String resourceRef) throws Exception {
        if (resourceRef != null && resourceRef.startsWith("/")) {
            RequestContext requestContext = RequestContext.getRequestContext();
            Repository repository = requestContext.getRepository();
            String token = requestContext.getSecurityToken();
            return repository.retrieve(token, Path.fromString(resourceRef), true);
        }
        
        return null;
    }

    // Adds media URL to model. Local resources are resolved to absolute
    // URLs and external URLs are parsed for validity. In case of invalid
    // URL, nothing is added to model.
    private void addMediaUrl(String resourceReferance, Map<String, Object> model) {
        URL url = createUrl(resourceReferance);
        if (RequestContext.getRequestContext().isPreviewUnpublished()) {
            url.setParameter("vrtxPreviewUnpublished", "true");
        }
        if (url != null) {
            model.put("media", url);
        }
    }

    private void addPosterUrl(Resource mediaFile, Map<String, Object> model) {
        if (mediaFile == null)
            return;
        URL poster = null;
        Property posterImageProp = mediaFile.getProperty(posterImagePropDef);
        Property thumbnail = mediaFile.getProperty(thumbnailPropDef);
        if (posterImageProp != null) {
            poster = createUrl(posterImageProp.getStringValue());
        } else if (thumbnail != null) {
            poster = thumbnailService.constructURL(mediaFile.getURI());
        }

        if (poster != null) {
            model.put("poster", poster);
        }
    }

    private URL createUrl(String mediaRef) {

        if (URL.isEncoded(mediaRef)) {
            mediaRef = URL.decode(mediaRef);
        }

        if (mediaRef != null && mediaRef.startsWith("/")) {
            URL localURL = null;
            try {
                Path uri = Path.fromString(mediaRef);
                localURL = this.viewService.constructURL(uri);
            } catch (Exception e) {
                // ignore
            }
            if (localURL != null) {
                return localURL;
            }

        } else {
            URL externalURL = null;
            try {
                externalURL = URL.parse(mediaRef);
            } catch (Exception e) {
                // ignore
            }
            if (externalURL != null) {
                return externalURL;
            }
        }
        return null;
    }

    @Required
    public void setViewService(Service viewService) {
        this.viewService = viewService;
    }

    @Required
    public void setPosterImagePropDef(PropertyTypeDefinition posterImagePropDef) {
        this.posterImagePropDef = posterImagePropDef;
    }

    @Required
    public void setThumbnailService(Service thumbnailService) {
        this.thumbnailService = thumbnailService;
    }

    /**
     * @param thumbnailPropDef the thumbnailPropDef to set
     */
    @Required
    public void setThumbnailPropDef(PropertyTypeDefinition thumbnailPropDef) {
        this.thumbnailPropDef = thumbnailPropDef;
    }

}
