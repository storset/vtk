/* Copyright (c) 2013, University of Oslo, Norway
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

package org.vortikal.videoref;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.ContentStream;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.util.io.StreamUtil;
import org.vortikal.web.RequestContext;

/**
 * Request streaming of resource from videoapp and redirect client to
 * stream location.
 */
public class StreamingRedirect implements Controller {

    private VideoappClient videoappClient;
    private StreamType streamType = StreamType.ADOBE_HDS;
    private PropertyTypeDefinition videoIdPropDef;
    
    private final Log logger = LogFactory.getLog(StreamingRedirect.class.getName());

    public enum StreamType {
        APPLE_HLS,
        ADOBE_HDS
    }

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        RequestContext rc = RequestContext.getRequestContext();
        Repository repo = rc.getRepository();
        Resource r = repo.retrieve(rc.getSecurityToken(), rc.getResourceURI(), true);
        
        if (!"videoref".equals(r.getResourceType())) {
            throw new IllegalStateException("This controller should only be called for videoref resources");
        }
        
        VideoId videoId = null;
        Property videoIdProp = r.getProperty(videoIdPropDef);
        if (videoIdProp != null) {
            videoId = VideoId.fromString(videoIdProp.getStringValue());
        }
        
        if (videoId == null) {
            // Try to obtain via alternative content
            logger.warn("Videoref resource " + rc.getResourceURI() + " missing videoId property");
            ContentStream cs = repo.getAlternativeContentStream(rc.getSecurityToken(),
                                            rc.getResourceURI(), true, "application/json");
            VideoRef ref = VideoRef.fromJsonString(StreamUtil.streamToString(cs.getStream())).build();
            videoId = ref.videoId();
        }

        StreamingRef ref = videoappClient.requestStreaming(r.getURI(), videoId);

        response.setStatus(303);
        if (streamType == StreamType.ADOBE_HDS) {
            response.setHeader("Location", ref.hdsStream().toASCIIString());
            return null;
        }
        if (streamType == StreamType.APPLE_HLS) {
            response.setHeader("Location", ref.hlsStream().toASCIIString());
            return null;
        }
        
        throw new IllegalStateException("Invalid or unknown stream type configured");
    }
    
    /**
     * @param videoappClient the videoappClient to set
     */
    @Required
    public void setVideoappClient(VideoappClient videoappClient) {
        this.videoappClient = videoappClient;
    }

    /**
     * @param streamType the streamType to set
     */
    public void setStreamType(StreamType streamType) {
        this.streamType = streamType;
    }
    
    /**
     * @param videoIdPropDef the videoIdPropDef to set
     */
    @Required
    public void setVideoIdPropDef(PropertyTypeDefinition videoIdPropDef) {
        this.videoIdPropDef = videoIdPropDef;
    }

}
