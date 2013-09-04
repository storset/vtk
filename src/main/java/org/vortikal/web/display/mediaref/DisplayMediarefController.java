/* Copyright (c) 2012, University of Oslo, Norway
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

package org.vortikal.web.display.mediaref;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.json.JSONObject;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.util.io.StreamUtil;
import org.vortikal.video.rest.VideoRef;
import org.vortikal.web.RequestContext;

/**
 * Simple serving of mediaref resources (only non-converted, original variant).
 */
public class DisplayMediarefController implements Controller {

    private View view;
    
    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        RequestContext requestContext = RequestContext.getRequestContext();
        Repository repository = requestContext.getRepository();
        String token = requestContext.getSecurityToken();
        Path uri = requestContext.getResourceURI();
        
        Resource resource = repository.retrieve(token, uri, true);
        Map<String,Object> model = new HashMap<String, Object>();
        model.put("resource", resource);

        if ("videoref".equals(resource.getResourceType())) {
            JSONObject refJson = JSONObject.fromObject(StreamUtil.streamToString(
                                    repository.getInputStream(token, uri, true), "utf-8"));
            
            VideoRef ref = VideoRef.newBuilder().fromJson(refJson).build();
            String contentType = ref.getSourceVideoFileRef().getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            long contentLength = ref.getSourceVideoFileRef().getSize();
            InputStream stream = repository.getInputStream(token, uri, true, contentType);
            
            model.put("resourceStream", stream);
            model.put("contentType", contentType);
            model.put("contentLength", contentLength);

            return new ModelAndView(this.view, model);
        }
        
        throw new UnsupportedOperationException("Unsupported resource type: " + resource.getResourceType());
    }
    
    public void setView(View view) {
        this.view = view;
    }

}
