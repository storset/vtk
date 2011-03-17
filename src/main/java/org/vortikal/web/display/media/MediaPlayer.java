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

import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

public class MediaPlayer {

    protected Map<String, String> extentionToMimetype;
    protected Service viewService;

    public void addMediaPlayer(Map<Object, Object> model, String token, Repository repository, String URL) {
        Resource mediaResource = null;
        try {
            mediaResource = repository.retrieve(token, Path.fromString(URL), false);
        } catch (Exception e) {
        }

        model.put("extension", getExtension(URL));
        model.put("autoplay", "false");

        if (mediaResource != null) {
            model.put("contentType", mediaResource.getContentType());
        } else {
            model.put("contentType", extentionToMimetype.get(getExtension(URL)));
        }

        createLocalUrlToMediaFile(URL, model);
    }

    public String getExtension(String url) {
        if (url != null && url.contains(".")) {
            String[] s = url.split("\\.");
            return s[s.length - 1];
        }
        return "";
    }

    public void createLocalUrlToMediaFile(String mediaUri, Map<Object, Object> model) {
        mediaUri = URL.decode(mediaUri); // Decode if encoded
        Path uri = null;
        URL localURL = null;
        try {
            uri = Path.fromString(mediaUri);
            localURL = getViewService().constructURL(uri);
        } catch (Exception e) {
        }
        if (localURL != null) {
            model.put("media", localURL);
        } else {
            model.put("media", mediaUri);
        }
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

}
