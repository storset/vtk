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

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.ContentStream;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.util.io.StreamUtil;
import org.vortikal.web.RequestContext;
import org.vortikal.web.referencedata.ReferenceDataProvider;

/**
 * Provide direct URLs to streaming server for current resource, if current
 * resource is of type 'videoref' and streaming is available. Otherwise
 * nothing is provided and submodel with key <code>modelName</code> will not
 * be inserted in main model.
 * 
 * <p>Configurable properties:
 * <ul>
 *   <li><code>modelName</code> - key to use for submodel containing streaming URLs (string).
 * Default value is "directStreamingUrls".
 * </ul>
 * 
 * <p>Provided data in submodel <code>modelName</code>:
 * <ul>
 *   <li><code>hdsStreamUrl<code> - <code>java.net.URL</code> pointing directly
 * to streaming server for hds stream.
 *   <li><code>hlsStreamUrl<code> - <code>java.net.URL</code> pointing directly
 * to streaming server for hls stream.
 * </ul>
 * 
 * Note that the provided URLs will only be valid for a limited amount of time.
 */
public class StreamingDirectUrlProvider implements ReferenceDataProvider {

    private String modelName = "directStreamingUrls";
    private VideoappClient videoappClient;
    private PropertyTypeDefinition videoIdPropDef;
    private PropertyTypeDefinition videoStatusPropDef;
    
    private final Log logger = LogFactory.getLog(StreamingDirectUrlProvider.class.getName());
    
    @Override
    public void referenceData(Map<String, Object> model, HttpServletRequest request) {
        try {
            RequestContext rc = RequestContext.getRequestContext();
            Repository repo = rc.getRepository();
            Resource r = repo.retrieve(rc.getSecurityToken(), rc.getResourceURI(), true);
            if (!"videoref".equals(r.getResourceType())) {
                return;
            }
            
            Property statusProp = r.getProperty(videoStatusPropDef);
            if (statusProp == null || !"completed".equals(statusProp.getStringValue())) {
                return;
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
            
            StreamingRef s = videoappClient.requestStreaming(videoId);
            Map<String,URL> submodel = new HashMap<String,URL>();
            model.put(modelName, submodel);

            submodel.put("hdsStreamUrl", s.hdsStream().toURL());
            submodel.put("hlsStreamUrl", s.hlsStream().toURL());
        } catch (Exception e) {
            logger.warn("Failed to provide direct streaming URLs", e);
        }
    }

    /**
     * @param modelName the modelName to set
     */
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    /**
     * @param videoappClient the videoappClient to set
     */
    @Required
    public void setVideoappClient(VideoappClient videoappClient) {
        this.videoappClient = videoappClient;
    }

    /**
     * @param videoIdPropDef the videoIdPropDef to set
     */
    @Required
    public void setVideoIdPropDef(PropertyTypeDefinition videoIdPropDef) {
        this.videoIdPropDef = videoIdPropDef;
    }

    /**
     * @param videoStatusPropDef the videoStatusPropDef to set
     */
    @Required
    public void setVideoStatusPropDef(PropertyTypeDefinition videoStatusPropDef) {
        this.videoStatusPropDef = videoStatusPropDef;
    }
}
