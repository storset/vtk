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

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.resourcemanagement.StructuredResource;
import org.vortikal.resourcemanagement.view.StructuredResourceDisplayController;
import org.vortikal.text.tl.Context;
import org.vortikal.text.tl.DirectiveNodeFactory;
import org.vortikal.text.tl.DirectiveParseContext;
import org.vortikal.text.tl.Node;
import org.vortikal.text.tl.Token;
import org.vortikal.web.RequestContext;

public class LocalizationNodeFactory implements DirectiveNodeFactory {

    private String resourceModelKey;
    
    public LocalizationNodeFactory(String resourceModelKey) {
        this.resourceModelKey = resourceModelKey;
    }

    public Node create(DirectiveParseContext ctx) throws Exception {
        List<Token> args = ctx.getArguments();
        if (args.size() == 0) {
            throw new RuntimeException("Missing arguments: " + ctx.getNodeText());
        }
        final Token code = args.remove(0);
        final List<Token> rest = new ArrayList<Token>(args);

        return new Node() {
            public boolean render(Context ctx, Writer out) throws Exception {
                String key = code.getValue(ctx).toString();
                RequestContext requestContext = RequestContext.getRequestContext();
                HttpServletRequest request = requestContext.getServletRequest();
                Object o = request.getAttribute(StructuredResourceDisplayController.MVC_MODEL_REQ_ATTR);
                if (o == null) {
                    throw new RuntimeException("Unable to locate resource: no model: " 
                            + StructuredResourceDisplayController.MVC_MODEL_REQ_ATTR);
                }
                Object[] localizationArgs = new Object[rest.size()];
                for (int i = 0; i < rest.size(); i++) {
                    Token a = rest.get(i);
                    localizationArgs[i] = a.getValue(ctx);
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> model = (Map<String, Object>) o;
                StructuredResource resource = (StructuredResource) model.get(resourceModelKey);
                if (resource == null) {
                    throw new RuntimeException("Unable to localize string: " + key + ": no resource found in model");
                }
                String localizedMsg = resource.getType().getLocalizedMsg(key, ctx.getLocale(), localizationArgs);
                out.write(ctx.htmlEscape(localizedMsg));
                return true;
            }
        };
    }
}
