/* Copyright (c) 2008, University of Oslo, Norway
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
package org.vortikal.web.tags;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.support.RequestContext;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceNotFoundException;

public final class TagsHelper {

    public static final String TAG_PARAMETER = "tag";
    public static final String SCOPE_PARAMETER = "scope";
    public static final String RESOURCE_TYPE_PARAMETER = "resource-type";
    public static final String RESOURCE_TYPE_MODEL_KEY = "resourceType";
    public static final String SCOPE_UP_MODEL_KEY = "scopeUp";

    private Repository repository;
    private String repositoryID;
    private boolean includeScopeInTitle;

    public Resource getScope(String token, HttpServletRequest request) throws Exception {
        Path requestedScope = this.getScopePath(request);
        Resource scopedResource = null;
        try {
            scopedResource = this.repository.retrieve(token, requestedScope, true);
        } catch (ResourceNotFoundException e) {
            throw new IllegalArgumentException("Scope resource doesn't exist: " + requestedScope);
        }

        if (!scopedResource.isCollection()) {
            throw new IllegalArgumentException("Scope resource isn't a collection");
        }
        return scopedResource;
    }

    private final Path getScopePath(HttpServletRequest request) {

        String scopeFromRequest = request.getParameter(SCOPE_PARAMETER);

        if (StringUtils.isBlank(scopeFromRequest) || ".".equals(scopeFromRequest)) {
            return org.vortikal.web.RequestContext.getRequestContext().getCurrentCollection();
        } else if (scopeFromRequest.startsWith("/")) {
            return Path.fromString(scopeFromRequest);
        }

        throw new IllegalArgumentException("Scope parameter must be empty, '.' or a valid path");
    }

    public String getTitle(HttpServletRequest request, Resource scope, String tag) {
        return this.getTitle(request, scope, tag, null);
    }

    public String getTitle(HttpServletRequest request, Resource scope, String tag, String scopeTitle) {
        RequestContext rc = new RequestContext(request);
        if (StringUtils.isBlank(tag)) {
            return getTitle(rc, scope, scopeTitle);
        }
        if (scopeTitle == null) {
            scopeTitle = scope.getURI().isRoot() ? this.repositoryID : scope.getTitle();
        }
        String titleKey = this.includeScopeInTitle ? "tags.scopedTitle" : "tags.title";
        String[] resourceParams = request.getParameterValues(RESOURCE_TYPE_PARAMETER);
        if (resourceParams != null && resourceParams.length == 1) {
            String tmpKey = titleKey + "." + resourceParams[0];
            try {
                rc.getMessage(tmpKey);
                titleKey = tmpKey;
            } catch (Exception e) {
                // key doesn't exist, ignore it
            }
        }
        return this.includeScopeInTitle ? rc.getMessage(titleKey, new Object[] { scopeTitle, tag }) : rc.getMessage(
                titleKey, new Object[] { tag });
    }

    private String getTitle(RequestContext rc, Resource scope, String scopeTitle) {
        if (!scope.getURI().isRoot()) {
            String titleParam = StringUtils.isBlank(scopeTitle) ? scope.getTitle() : scopeTitle;
            return rc.getMessage("tags.serviceTitle", new Object[] { titleParam });
        }
        return rc.getMessage("tags.noTagTitle");
    }

    @Required
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @Required
    public void setRepositoryID(String repositoryID) {
        this.repositoryID = repositoryID;
    }

    public void setIncludeScopeInTitle(boolean includeSopeInTitle) {
        this.includeScopeInTitle = includeSopeInTitle;
    }

}
