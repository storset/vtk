/* Copyright (c) 2009, University of Oslo, Norway
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
package org.vortikal.resourcemanagement.view.tl;

import java.util.List;
import java.util.Map;

import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceNotFoundException;
import org.vortikal.resourcemanagement.view.StructuredResourceDisplayController;
import org.vortikal.security.SecurityContext;
import org.vortikal.text.tl.Argument;
import org.vortikal.text.tl.Context;
import org.vortikal.text.tl.DefineNodeFactory;

public class RetrieveHandler implements DefineNodeFactory.ValueProvider {

    private Repository repository;
    
    public RetrieveHandler(Repository repository) {
        this.repository = repository;
    }

    /**
     * resource "/foo/bar"
     * resource "."
     * resource <var>
     */
    public Object create(List<Argument> tokens, Context ctx) throws Exception {

        if (tokens.size() != 1) {
            throw new RuntimeException("Wrong number of arguments");
        }
        final Argument arg = tokens.get(0);

        Resource resource;

        String ref = arg.getValue(ctx).toString();

        if (ref.equals(".")) {
            Object o = ctx.get(StructuredResourceDisplayController.MVC_MODEL_KEY);
            if (o == null) {
                return null;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> model = (Map<String, Object>) o;
            resource = (Resource) model.get("resource");
        } else if (!ref.startsWith("/")) {
            Object o = ctx.get(ref);
            if (o == null) {
                return null;
            }
            String s = o.toString();
            Path uri = Path.fromString(s);
            String token = SecurityContext.getSecurityContext().getToken();
            resource = repository.retrieve(token, uri, true);
        } else {
            Path uri = Path.fromString(ref);
            String token = SecurityContext.getSecurityContext().getToken();
            try {
                resource = repository.retrieve(token, uri, true);
            } catch (ResourceNotFoundException rnfe) {
                return null;
            }
        }
        return resource;
    }
}
