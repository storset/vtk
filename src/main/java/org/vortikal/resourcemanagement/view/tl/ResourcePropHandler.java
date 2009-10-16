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

import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.resourcemanagement.view.StructuredResourceDisplayController;
import org.vortikal.security.SecurityContext;
import org.vortikal.text.tl.Argument;
import org.vortikal.text.tl.Context;
import org.vortikal.text.tl.DefineNodeFactory;
import org.vortikal.text.tl.Symbol;

public class ResourcePropHandler implements DefineNodeFactory.ValueProvider {

    private Repository repository;
    
    public ResourcePropHandler(Repository repository) {
        this.repository = repository;
    }

    /**
     * Supported constructions:
     * <code>
     * "/foo/bar/" propname
     * "." propname
     * var propname
     * </code>
     */
    public Object create(List<Argument> tokens, Context ctx) throws Exception {

        if (tokens.size() != 2) {
            throw new RuntimeException("Wrong number of arguments");
        }
        final Argument arg1 = tokens.get(0);
        final Argument arg2 = tokens.get(1);

        PropertySet resource = null;
        String ref = null;
        if (arg1 instanceof Symbol) {
            Object o = arg1.getValue(ctx);
            if (o instanceof PropertySet) {
                resource = (PropertySet) o;
            } else {
                ref = o.toString();
            }
        } else {
            ref = arg1.getValue(ctx).toString();
        }

        if (resource == null) {
            if (ref.equals(".")) {
                Object o = ctx.get(StructuredResourceDisplayController.MVC_MODEL_KEY);
                if (o == null) {
                    throw new Exception("Unable to access MVC model: " 
                            + StructuredResourceDisplayController.MVC_MODEL_KEY);
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> model = (Map<String, Object>) o;
                resource = (Resource) model.get("resource");
            } else {
                Path uri = Path.fromString(ref);
                String token = SecurityContext.getSecurityContext().getToken();
                resource = repository.retrieve(token, uri, true);
            }
        }
        String propName;
        if (arg2 instanceof Symbol) {
            Object o = arg2.getValue(ctx);
            if (o == null) {
                throw new Exception("Unable to resolve: " + arg2.getRawValue());
            }
            propName = o.toString();
        } else {
            propName = arg2.getValue(ctx).toString();
        }
        Property property = resource.getProperty(Namespace.STRUCTURED_RESOURCE_NAMESPACE, propName);
        if (property == null) {
            property = resource.getProperty(Namespace.DEFAULT_NAMESPACE, propName);
        }
        if (property == null) {
            for (Property prop : resource.getProperties()) {
                if (propName.equals(prop.getDefinition().getName())) {
                    property = prop;
                }
            }
        }
        if (property == null) {
            return null;
        }
        if (property.getDefinition().isMultiple()) {
            return property.getValues();
        } else {
            return property.getValue();
        }
    }
}
