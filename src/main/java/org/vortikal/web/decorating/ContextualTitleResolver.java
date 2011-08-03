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
package org.vortikal.web.decorating;

import java.util.Map;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.web.RequestContext;
import org.vortikal.web.RequestContext.RepositoryTraversal;
import org.vortikal.web.RequestContext.TraversalCallback;

public class ContextualTitleResolver {
    
    private Repository repository;
    private Map<String, ?> config;
    private String token = null;

    public String resolve(Resource resource) {
        RepositoryTraversal traversal = RequestContext.rootTraversal(this.repository, this.token, resource.getURI());
        final String repoID = this.repository.getId();
        if (!this.config.containsKey(repoID)) {
            return resource.getTitle();
        }
        final StringBuilder mapping = new StringBuilder();
        try {
            traversal.traverse(new TraversalCallback() {
                @Override
                public boolean callback(Resource resource) {
                    Object o = config.get(repoID);
                    if (o instanceof Map) {
                        Map<?, ?> m = (Map<?, ?>) o;
                        Object val = m.get(resource.getURI().toString());
                        if (val != null) {
                            mapping.append(val.toString());
                            return false;
                        }
                    }
                    return true;
                }});
        } catch (Exception e) {
            return resource.getTitle();
        }
        if (mapping.length() == 0) {
            return resource.getTitle();
        }
        String result = mapping.toString().replaceAll("\\$\\{title\\}", resource.getTitle());
        return result;
    }

    @Required
    public void setConfig(Map<String, ?> config) {
        this.config = config;
    }
    
    @Required
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
