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
package org.vortikal.web.commenting;

import java.util.Map;

import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.RepositoryAction;
import org.vortikal.repository.Resource;
import org.vortikal.web.RequestContext;
import org.vortikal.web.RequestContext.RepositoryTraversal;
import org.vortikal.web.RequestContext.TraversalCallback;
import org.vortikal.web.decorating.DecoratorRequest;
import org.vortikal.web.decorating.DecoratorResponse;
import org.vortikal.web.decorating.components.ViewRenderingDecoratorComponent;

public class CommentsFeedUrlProvider extends ViewRenderingDecoratorComponent {
    
    private String trustedToken = null;
    
    public void setTrustedToken(String trustedToken) {
        this.trustedToken = trustedToken;
    }

    protected void processModel(final Map<Object, Object> model, DecoratorRequest request, DecoratorResponse response)
            throws Exception {
        super.processModel(model, request, response);
        RequestContext requestContext = RequestContext.getRequestContext();
        Repository repository = requestContext.getRepository();
        String token = requestContext.getSecurityToken();
        Path uri = RequestContext.getRequestContext().getResourceURI();
        Resource resource = repository.retrieve(token, uri, true);
        boolean commentsAllowed = false;
        if (resource != null) {
            commentsAllowed = repository.isAuthorized(resource, RepositoryAction.ADD_COMMENT, 
                    requestContext.getPrincipal(), false);
        }
        model.put("commentsAllowed", commentsAllowed);
        
        model.put("commentsEnabled", false);
        String traversalToken = this.trustedToken != null ? this.trustedToken : token;
        RepositoryTraversal traversal = requestContext.rootTraversal(traversalToken, uri);
        traversal.traverse(new TraversalCallback() {
            @Override
            public boolean callback(Resource resource) {
                for (Property p: resource.getProperties()) {
                    if (p.getDefinition().getName().equals("commentsEnabled")) {
                        model.put("commentsEnabled", p.getBooleanValue());
                        return false;
                    }
                }
                return true;
            }
            @Override
            public boolean error(Path uri, Throwable error) {
                return false;
            }
        });
    }

}
