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
package org.vortikal.web.actions.copymove;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.web.Message;
import org.vortikal.web.RequestContext;
import org.vortikal.web.actions.convert.CopyAction;

/**
 * A controller that copies (or moves) resources from one folder to another
 * based on a set of resources stored in a session variable
 * <p>
 * Configurable properties:
 * <ul>
 * <li>{@link String viewName} - the view to which to return to
 * <li>{@link CopyAction copyAction} - if specified, invoke this 
 *     {@link CopyAction} instead of {@link Repository#copy} to 
 *     copy resources
 * </ul>
 * </p>
 * <p>
 * Model data published:
 * <ul>
 * <li>{@code createErrorMessage}: error message
 * <li>{@code errorItems}: an array of repository items which the
 *      error message relates to
 * </ul>
 * </p>
 */
public class CopyMoveToSelectedFolderController implements Controller {

    private static Log logger = LogFactory.getLog(CopyMoveToSelectedFolderController.class);
    static final String COPYMOVE_SESSION_ATTRIBUTE = "copymovesession";
    private String viewName = "DEFAULT_VIEW_NAME";
    private CopyHelper copyHelper;

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        RequestContext requestContext = RequestContext.getRequestContext();
        String token = requestContext.getSecurityToken();
        Path destinationUri = requestContext.getCurrentCollection();
        Repository repository = requestContext.getRepository();
        
        CopyMoveSessionBean sessionBean = (CopyMoveSessionBean) request.getSession(true).getAttribute(
                COPYMOVE_SESSION_ATTRIBUTE);
        if (sessionBean == null) {
            return new ModelAndView(this.viewName);
        }
        if (request.getParameter("cancel-action") != null) {
            return new ModelAndView(this.viewName);
        }
        long before = System.currentTimeMillis();

        List<Path> filesFailed = new ArrayList<Path>();
        String action = sessionBean.getAction();

        List<String> items = sessionBean.getFilesToBeCopied();

        boolean authorizationFailed = false;
        boolean moveAction = "move-resources".equals(action);

        for (String s : items) {
            Path uri = Path.fromString(s);
            String name = uri.getName();

            Path newResourceUri = destinationUri.extend(name);

            try {
                if (moveAction) {
                    if (repository.exists(token, newResourceUri)) {
                        throw new RuntimeException("Trying to move to resource with same filename: " + newResourceUri);
                    }
                    repository.move(token, uri, newResourceUri, false);

                } else {
                    Path destUri = newResourceUri;
                    newResourceUri = this.copyHelper.copyResource(uri, destUri, repository, token, null, null, false);    
                }
            } catch (AuthorizationException e) {
                filesFailed.add(uri);
                authorizationFailed = true;

            } catch (Exception e) {
                filesFailed.add(uri);
            }
        }

        if (filesFailed.size() > 0) {
            String msgCode = "";
            if (authorizationFailed) {
                if (moveAction) {
                    msgCode = "manage.create.copyMove.error.authorization.moveFailed";
                } else {
                    msgCode = "manage.create.copyMove.error.authorization.copyFailed";
                }
            } else {
                if (moveAction) {
                    msgCode = "manage.create.copyMove.error.moveFailed";
                } else {
                    msgCode = "manage.create.copyMove.error.copyFailed";
                }
            }

            Message msg = new Message(msgCode);
            for (Path p : filesFailed) {
                msg.addMessage(p.getName());
            }
            requestContext.addErrorMessage(msg);
        }

        // Removing session variable
        request.getSession(true).removeAttribute(COPYMOVE_SESSION_ATTRIBUTE);

        if (logger.isDebugEnabled()) {
            long total = System.currentTimeMillis() - before;
            logger.debug("Milliseconds spent on this copy/move operation: " + total);
        }

        return new ModelAndView(this.viewName);
    }

    public void setCopyHelper(CopyHelper copyHelper) {
        this.copyHelper = copyHelper;
    }

}
