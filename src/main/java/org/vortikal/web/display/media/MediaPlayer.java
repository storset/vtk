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

import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.security.AuthenticationException;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

public class MediaPlayer {

    protected Map<String, String> extentionToMimetype;
    protected Service viewService;
    private Service thumbnailService;
    private PropertyTypeDefinition posterImagePropDef;
    private PropertyTypeDefinition generatedPosterImagePropDef;

    public void addMediaPlayer(Map<Object, Object> model, String resourceReferance, String height, String width,
            String autoplay, String contentType, String streamType, String poster) throws AuthorizationException {

        if (URL.isEncoded(resourceReferance)) {
            resourceReferance = URL.decode(resourceReferance);
        }

        Resource mediaResource = null;
        try {
            mediaResource = getLocalResource(resourceReferance);
        } catch (AuthorizationException e) {
            return; // not able to read local resource - abort
        } catch (AuthenticationException e) {
            return; // not able to read local resource - abort
        } catch (Exception e) {
            // ignore
        }

        if ((height != null && !"".equals(height)) && (width != null && !"".equals(width))) {
            model.put("height", height);
            model.put("width", width);
        }

        if (autoplay != null && !"".equals(autoplay))
            model.put("autoplay", autoplay);
        if (streamType != null && !"".equals(streamType))
            model.put("streamType", streamType);
        if (poster != null && !"".equals(poster)) {
            model.put("poster", poster);
        } else {
            addPoster(mediaResource, model);
        }

        String extension = getExtension(resourceReferance);
        if (contentType != null && !"".equals(contentType)) {
            model.put("contentType", contentType);
        } else if (mediaResource != null) {
            model.put("contentType", mediaResource.getContentType());
        } else if (getExtentionToMimetype().containsKey(extension)) {
            model.put("contentType", getExtentionToMimetype().get(extension));
        }
        model.put("extension", extension);
        model.put("nanoTime", System.nanoTime());

        createLocalUrlToMediaFile(resourceReferance, model);
    }

    public void addMediaPlayer(Map<Object, Object> model, String resourceReferance) throws AuthorizationException {

        if (URL.isEncoded(resourceReferance)) {
            resourceReferance = URL.decode(resourceReferance);
        }

        Resource mediaResource = null;
        try {
            mediaResource = getLocalResource(resourceReferance);
        } catch (AuthorizationException e) {
            return; // not able to read local resource - abort
        } catch (AuthenticationException e) {
            return; // not able to read local resource - abort
        } catch (Exception e) {
            // ignore
        }

        addPoster(mediaResource, model);
        model.put("extension", getExtension(resourceReferance));

        if (mediaResource != null) {
            model.put("contentType", mediaResource.getContentType());
        } else {
            model.put("contentType", extentionToMimetype.get(getExtension(resourceReferance)));
        }

        model.put("nanoTime", System.nanoTime());
        createLocalUrlToMediaFile(resourceReferance, model);
    }

    public Resource getLocalResource(String resourceReferance) throws Exception {
        Resource mediaResource = null;
        if (resourceReferance != null && resourceReferance.startsWith("/")) {
            RequestContext requestContext = RequestContext.getRequestContext();
            Repository repository = requestContext.getRepository();
            String token = requestContext.getSecurityToken();
            try {
                mediaResource = repository.retrieve(token, Path.fromString(resourceReferance), true);
            } catch (Exception e) {
                throw e;
            }
        }
        return mediaResource;
    }

    public String getExtension(String url) {
        if (url != null && url.contains(".")) {
            String[] s = url.split("\\.");
            return s[s.length - 1];
        }
        return "";
    }

    public void createLocalUrlToMediaFile(String resourceReferance, Map<Object, Object> model) {
        URL url = createUrl(resourceReferance);
        if (url != null) {
            model.put("media", url);
        }
    }

    public void addPoster(Resource mediaFile, Map<Object, Object> model) {
        if (mediaFile == null)
            return;
        URL poster = null;
        Property posterImageProp = mediaFile.getProperty(posterImagePropDef);
        Property thumbnail = mediaFile.getProperty(generatedPosterImagePropDef);
        if (posterImageProp != null) {
            poster = createUrl(posterImageProp.getStringValue());
        } else if (thumbnail != null) {
          poster = thumbnailService.constructURL(mediaFile.getURI());
        }

        if (poster != null) {
            model.put("poster", poster);
        }
    }

    public URL createUrl(String resourceReferance) {

        if (URL.isEncoded(resourceReferance)) {
            resourceReferance = URL.decode(resourceReferance);
        }

        if (resourceReferance != null && resourceReferance.startsWith("/")) {
            URL localURL = null;
            try {
                Path uri = null;
                uri = Path.fromString(resourceReferance);
                localURL = getViewService().constructURL(uri);
            } catch (Exception e) {
                // ignore
            }
            if (localURL != null) {
                return localURL;
            }

        } else {
            URL externalURL = null;
            try {
                externalURL = URL.parse(resourceReferance);
            } catch (Exception e) {
                // ignore
            }
            if (externalURL != null) {
                return externalURL;
            }
        }
        return null;
    }

    public Map<String, String> getExtentionToMimetype() {
        return extentionToMimetype;
    }

    public void setExtentionToMimetype(Map<String, String> extentionToMimetype) {
        this.extentionToMimetype = extentionToMimetype;
    }

    public void setViewService(Service viewService) {
        this.viewService = viewService;
    }

    public Service getViewService() {
        return viewService;
    }

    public void setPosterImagePropDef(PropertyTypeDefinition posterImagePropDef) {
        this.posterImagePropDef = posterImagePropDef;
    }

    public PropertyTypeDefinition getPosterImagePropDef() {
        return posterImagePropDef;
    }

    public void setGeneratedPosterImagePropDef(PropertyTypeDefinition generatedPosterImagePropDef) {
        this.generatedPosterImagePropDef = generatedPosterImagePropDef;
    }

    public PropertyTypeDefinition getGeneratedPosterImagePropDef() {
        return generatedPosterImagePropDef;
    }

    public void setThumbnailService(Service thumbnailService) {
        this.thumbnailService = thumbnailService;
    }

    public Service getThumbnailService() {
        return thumbnailService;
    }

}
