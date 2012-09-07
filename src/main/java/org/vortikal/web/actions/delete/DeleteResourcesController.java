/* Copyright (c) 2011, University of Oslo, Norway
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
package org.vortikal.web.actions.delete;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.ResourceLockedException;
import org.vortikal.web.Message;
import org.vortikal.web.RequestContext;

public class DeleteResourcesController implements Controller {

    private String viewName;
    
    private static Log logger = LogFactory.getLog(DeleteResourcesController.class);

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) {
        RequestContext requestContext = RequestContext.getRequestContext();
        Repository repository = requestContext.getRepository();
        String token = requestContext.getSecurityToken();

        boolean recoverable = true;
        String permanent = request.getParameter("permanent");
        if ("true".equals(permanent)) {
            recoverable = false;
        }
        
        // Map of files that for some reason failed on delete. Separated by a
        // key (String) that specifies type of failure and identifies list of
        // paths to resources that failed.
        Map<String, List<Path>> failures = new HashMap<String, List<Path>>();
        String msgKey = "manage.delete.error.";

        @SuppressWarnings("rawtypes")
        Enumeration e = request.getParameterNames();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            Path uri = null;
            try {
            	uri = Path.fromString(name);
            	if (repository.exists(token, uri)) {
            		repository.delete(token, uri, recoverable);
            	} else {
            		this.addToFailures(failures, uri, msgKey, "nonExisting");
            	}
            } catch (IllegalArgumentException iae) {
            	// Not a path, ignore it, try next one
            	continue;
            } catch (AuthorizationException ae) {
                this.addToFailures(failures, uri, msgKey, "unAuthorized");
            } catch (ResourceLockedException rle) {
                this.addToFailures(failures, uri, msgKey, "locked");
            } catch (Exception ex) {
                StringBuilder msg = new StringBuilder("Could not perform ");
                msg.append("delete of ").append(uri);
                msg.append(": ").append(ex.getMessage());
                logger.warn(msg);
                this.addToFailures(failures, uri, msgKey, "generic");
            }
        }

        for (Entry<String, List<Path>> entry : failures.entrySet()) {
            String key = entry.getKey();
            List<Path> failedResources = entry.getValue();
            Message msg = new Message(key);
            for (Path p : failedResources) {
                msg.addMessage(p.getName());
            }
            requestContext.addErrorMessage(msg);
        }

        return new ModelAndView(this.viewName);
    }
    
    private void addToFailures(Map<String, List<Path>> failures, Path fileUri, String msgKey, String failureType) {
        String key = msgKey.concat(failureType);
        List<Path> failedPaths = failures.get(key);
        if (failedPaths == null) {
            failedPaths = new ArrayList<Path>();
            failures.put(key, failedPaths);
        }
        failedPaths.add(fileUri);
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public String getViewName() {
        return viewName;
    }
}
