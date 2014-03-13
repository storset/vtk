/* Copyright (c) 2014, University of Oslo, Norway
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
package org.vortikal.web.display.diff;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.mvc.ParameterizableViewController;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Revision;
import org.vortikal.security.PrincipalFactory;
import org.vortikal.security.Principal.Type;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.URL;
import org.vortikal.web.servlet.BufferedResponse;
import org.vortikal.web.servlet.ConfigurableRequestWrapper;
import org.vortikal.web.servlet.VortikalServlet;

/*
 * Visually display the differences between two revisions of a document,
 * by comparing and marking up the underlying HTML.
 * 
 * Parameter: The request argument 'revision' should contain a comma separated pair of revision names.
 * Differences will then be calculated from the first one to the second one.
 * Revisions names may be anything (not including a comma), and is not constrained to integers.
 * If just a single revision name is present, it must be an integer value (n). Differences 
 * will then be calculated for revisions 'n-1,n'.
 * 
 * NB! The current implementation will only return the inner HTMl of the body tag. Outer HTML is discarded.
 * NB! No decorating is performed on the result. 
 */
public class DisplayRevisionsDifferenceController extends ParameterizableViewController implements Controller, InitializingBean {

    private static Log logger = LogFactory.getLog(DisplayRevisionsDifferenceController.class);

    private PrincipalFactory principalFactory;
   
    public void afterPropertiesSet() throws Exception {
    }

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        
        String revisionParam = request.getParameter("revision");
        if (revisionParam == null) {
            return badRequest("'revision' argument must be supplied", response);
        }
        String revisionNameA = null;
        String revisionNameB = null;
        String[] revisions = revisionParam.split(",");
        if (revisions.length == 2) {
            revisionNameA = revisions[0];
            revisionNameB = revisions[1];
        } else {
            try {
                revisionNameB = revisionParam;
                int revisionNumber = Integer.parseInt(revisionParam);
                revisionNameA = "" + (revisionNumber - 1); 
            } catch (NumberFormatException e) {
                return badRequest("'revision' must be either an integer or a comma separated pair", response);
            }
        }
        String content = diffRevisions(revisionNameA, revisionNameB, request);
        
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("revisionA", revisionNameA);
        model.put("revisionB", revisionNameB);
        model.put("content", content);
        putRevisionInfo(model, revisionNameA, revisionNameB, request);
        
        return new ModelAndView(getViewName(), model);
    }
    
    /*
     * For aborting the request with a HTTP Bad Request response.
     */
    private ModelAndView badRequest(String errorMessage, HttpServletResponse response) throws IOException {
        logger.error(errorMessage);
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, errorMessage);
        return null;
    }
    
    /*
     * Retrieve specified revisions and return a comparison result.
     * 
     * NB! No input validation is performed.
     * I.e. it is assumed that the revisions names are valid an existing,
     * and that a null result for a revision is not returned.
     */
    private String diffRevisions(String revisionNameA, String revisionNameB, HttpServletRequest request) throws Exception {
        String contentA = getContentForRevision(revisionNameA, request);
        String contentB = getContentForRevision(revisionNameB, request);
        return getHtmlDifferences(contentA, contentB);
    }
    
    private String getHtmlDifferences(String contentA, String contentB) throws Exception {
        DifferenceEngine differ = new DifferenceEngine();
        return differ.diff(contentA, contentB);
    }

    /*
     * Use internal request to look up a plain version of the given revision of the resource.
     */
    private String getContentForRevision(String revisionName, HttpServletRequest request) throws Exception {
        URL forwardURL = URL.create(request);
        forwardURL.clearParameters();
        if (!"HEAD".equals(revisionName)) {
            forwardURL.addParameter("revision", revisionName);
        }
        forwardURL.addParameter("x-decorating-mode", "plain");

        if (logger.isDebugEnabled()) {
            logger.debug("Dispatch forward request to: " + forwardURL);
        }

        ConfigurableRequestWrapper requestWrapper = new ConfigurableRequestWrapper(request, forwardURL);
        String servletName = (String) request.getAttribute(VortikalServlet.SERVLET_NAME_REQUEST_ATTRIBUTE);
        RequestDispatcher rd = getServletContext().getNamedDispatcher(servletName);

        if (rd == null) {
            throw new RuntimeException("No request dispatcher for name '" + servletName + "' available");
        }

        BufferedResponse bufferedResponse = new BufferedResponse();
        rd.include(requestWrapper, bufferedResponse);
        int status = bufferedResponse.getStatus();
        if (status < 200 || status > 299) {
            throw new Exception("Internal request " + forwardURL + " failed with HTTP status " + status);
        }
        return bufferedResponse.getContentString();
    }

    /*
     * Add meta data for current revision, and list all available revisions for the resource
     */
    private void putRevisionInfo(Map<String, Object> model, String revisionNameA, String revisionNameB, HttpServletRequest request) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        Repository repository = requestContext.getRepository();
        String token = requestContext.getSecurityToken();
        Path uri = requestContext.getResourceURI();
        List<Revision> revisions = repository.getRevisions(token, uri);

        List<Object> allRevisions = new ArrayList<Object>();
        String revisionBNext = null;
        boolean haveRecentlySeenRevisionA = false;
        for (Revision revision: revisions) {
            Map<String, Object> rev = new HashMap<String, Object>();
            rev.put("id", revision.getID());
            rev.put("name", revision.getName());
            rev.put("timestamp", revision.getTimestamp());
            rev.put("principal", this.principalFactory.getPrincipal(revision.getUid(), Type.USER)); 
            rev.put("acl", revision.getAcl());
            rev.put("checksum", revision.getChecksum());
            rev.put("changeAmount", revision.getChangeAmount());
            allRevisions.add(rev);
            if (haveRecentlySeenRevisionA) {
                model.put("revisionAPrevious", revision.getName());
                haveRecentlySeenRevisionA = false;
            }
            if (revisionNameA.equalsIgnoreCase(revision.getName())) {
                model.put("revisionADetails", rev);
                haveRecentlySeenRevisionA = true;
            }
            if (revisionNameB.equalsIgnoreCase(revision.getName())) {
                model.put("revisionBDetails", rev);
                if (revisionBNext != null) {
                    model.put("revisionBNext", revisionBNext);
                }
            }
            revisionBNext = revision.getName();
        }
        model.put("allRevisions", allRevisions);
    }
   
    @Required
    public void setPrincipalFactory(PrincipalFactory principalFactory) {
        this.principalFactory = principalFactory;
    }
}
