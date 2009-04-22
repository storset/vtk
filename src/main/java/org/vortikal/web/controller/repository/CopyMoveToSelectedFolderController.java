/* Copyright (c) 2005, 2008 University of Oslo, Norway
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
package org.vortikal.web.controller.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Repository.Depth;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.controller.CopyMoveSessionBean;

/**
 * A controller that copies (or moves) resources from one folder to another based on a set of resources stored in a
 * session variable
 * 
 * <p>
 * Description:
 * 
 * <p>
 * Configurable properties:
 * <ul>
 * <li><code>repository</code> - the content repository
 * <li><code>viewName</code> - the view to which to return to
 * </ul>
 * 
 * <p>
 * Model data published:
 * <ul>
 * <li><code>createErrorMessage</code>: errormessage
 * <li><code>errorItems</code>: an array of repository items which the errormessage relates to
 * </ul>
 */

public class CopyMoveToSelectedFolderController implements Controller {
    
    private static Log logger = LogFactory.getLog(CopyMoveToSelectedFolderController.class);
    private static final String COPYMOVE_SESSION_ATTRIBUTE = "copymovesession";
    private String viewName = "DEFAULT_VIEW_NAME";
    private Repository repository = null;


    public void setViewName(String viewName) {
        this.viewName = viewName;
    }


    public final void setRepository(final Repository newRepository) {
        this.repository = newRepository;
    }


    @SuppressWarnings("unchecked")
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Map model = new HashMap();

        SecurityContext securityContext = SecurityContext.getSecurityContext();
        String token = securityContext.getToken();
        RequestContext requestContext = RequestContext.getRequestContext();
        Path destinationUri = requestContext.getCurrentCollection();

        CopyMoveSessionBean sessionBean = (CopyMoveSessionBean) request.getSession(true).getAttribute(
                COPYMOVE_SESSION_ATTRIBUTE);

        // TODO: Should probably give some feedback when there is no session variable, but ...
        if (sessionBean == null) {
            return new ModelAndView(this.viewName, model);
        }

        // For logging purposes
        long before = System.currentTimeMillis();

        List<Path> filesFailed = new ArrayList<Path>();
        String action = sessionBean.getAction();

        // Getting the selected files from Session
        List<String> filesToBeCopied = sessionBean.getFilesToBeCopied();

        ListIterator<String> i = filesToBeCopied.listIterator();

        while (i.hasNext()) {

            // TODO: Need to construct the uri to the new file in a more elegant way...
            Path resourceUri = Path.fromString(i.next());
            String resourceFilename = resourceUri.getName();

            Path newResourceUri = destinationUri.extend(resourceFilename);

            if (logger.isDebugEnabled()) {
                logger.debug("Trying to copy(or move) resource from: " + resourceUri + " to: " + newResourceUri);
            }

            try {
                if (action.equals("move-resources")) {

                    if (!this.repository.exists(token, newResourceUri)) {
                        this.repository.move(token, resourceUri, newResourceUri, false);
                    } else {

                        if (logger.isDebugEnabled()) {
                            logger.debug("Trying to move to resource with same filename: " + newResourceUri);
                        }

                        throw new Exception();
                    }

                } else {
                    if (!this.repository.exists(token, newResourceUri)) {
                        this.repository.copy(token, resourceUri, newResourceUri, Depth.INF, false, false);
                    } else {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Trying to duplicate resource: " + newResourceUri);
                        }

                        Path newUri = newResourceUri;

                        int number = 1;
                        while (this.repository.exists(token, newUri)) {
                            newUri = appendCopySuffix(newUri, number);
                            number++;
                        }
                        this.repository.copy(token, resourceUri, newUri, Depth.INF, false, false);
                    }
                }

            } catch (Exception e) {
                filesFailed.add(resourceUri);

                if (logger.isDebugEnabled()) {
                    logger.debug("Copy/Move action failed: " + e.getClass().getName() + " " + e.getMessage());
                }

            }
        }

        if (filesFailed.size() > 0) {
            model.put("errorItems", filesFailed);
            model.put("createErrorMessage", "manage.create.copyMove.error.moveFailed");
        }

        // Removing session variable
        request.getSession(true).removeAttribute(COPYMOVE_SESSION_ATTRIBUTE);

        long total = System.currentTimeMillis() - before;

        if (logger.isDebugEnabled()) {
            logger.debug("Milliseconds spent on this copy/move operation: " + total);
        }

        return new ModelAndView(this.viewName, model);
    }

    
    protected Path appendCopySuffix(Path newUri, int number) {
        String extension = "";
        String dot = "";
        String name = newUri.getName();

        if (name.endsWith(".")) {
            name = name.substring(0, name.lastIndexOf("."));

        } else if (name.contains(".")) {
            extension = name.substring(name.lastIndexOf(".") + 1, name.length());
            dot = ".";
            name = name.substring(0, name.lastIndexOf("."));
        }
        
        Pattern pattern = Pattern.compile("\\(\\d\\)$");
        Matcher matcher = pattern.matcher(name);
        if (matcher.find()) {
            String count = matcher.group();
            count = count.substring(1, count.length() - 1);
            try {
                number = Integer.parseInt(count) + 1;
                name = pattern.split(name)[0];
            } catch (Exception e) {
            }
        }
        
        name = name + "(" + number + ")" + dot + extension;
        return newUri.getParent().extend(name);
    }

}
