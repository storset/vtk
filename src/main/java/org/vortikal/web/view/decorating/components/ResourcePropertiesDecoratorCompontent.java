/* Copyright (c) 2007, University of Oslo, Norway
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
package org.vortikal.web.view.decorating.components;

import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.BeanInitializationException;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFormatter;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.view.decorating.DecoratorRequest;
import org.vortikal.web.view.decorating.DecoratorResponse;

public class ResourcePropertiesDecoratorCompontent extends AbstractDecoratorComponent {

    private static final String PARAMETER_ID = "id";
    private static final String PARAMETER_ID_DESC = 
        "Identifies the property to report. One of 'uri', 'name', 'type' or '<prefix>:<name>' identifying a property.";

    private static final String DESCRIPTION = "Report a property on the current resource, as a formatted and localized string";
    
    private static final String URL_IDENTIFIER = "url";
    private static final String NAME_IDENTIFIER = "name";
    private static final String TYPE_IDENTIFIER = "type";
    private static final String URI_IDENTIFIER = "uri";

    private Repository repository;

    private boolean forProcessing = true;

    private ValueFormatter valueFormatter;

    private ResourceTypeTree resourceTypeTree;

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void render(DecoratorRequest request, DecoratorResponse response)
            throws Exception {
        String token = SecurityContext.getSecurityContext().getToken();
        String uri = RequestContext.getRequestContext().getResourceURI();

        Resource resource = repository.retrieve(token, uri, this.forProcessing);

        String id = request.getStringParameter(PARAMETER_ID);
        String result = null;

        if (id == null || id.trim().equals("")) {
            return;
        }

        if (URI_IDENTIFIER.equals(id)) {
            result = uri;
        } else if (NAME_IDENTIFIER.equals(id)) {
            result = resource.getName();
        } else if (TYPE_IDENTIFIER.equals(id)) {
            result = resource.getResourceType();
        } else if (URL_IDENTIFIER.equals(id)) {
            return;
        } else {
            String prefix = null;
            String name = null;

            int i = id.indexOf(":");
            if (i < 0) {
                name = id;
            } else if (i == 0 || i == id.length() - 1) {
                // XXX: throw something
                return;
            } else {
                prefix = id.substring(0, i);
                name = id.substring(i + 1);
            }
            PropertyTypeDefinition def = this.resourceTypeTree
                    .getPropertyDefinitionByPrefix(prefix, name);

            Property prop = resource.getProperty(def);

            if (prop == null) {
                return;
            }

            if (prop.getDefinition().isMultiple()) {
                result = "";
                Value[] values = prop.getValues();
                for (int j = 0; j < values.length; j++) {
                    Value value = values[j];
                    result += this.valueFormatter.valueToString(value, null,
                            request.getLocale());
                    if (j != values.length - 1) {
                        result += ", ";
                    }
                }

            } else {

                Value value = prop.getValue();
                result = this.valueFormatter.valueToString(value, null, request
                        .getLocale());
            }
        }

        Writer writer = response.getWriter();
        try {
            writer.write(result);
        } finally {
            writer.close();
        }
    }

    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();

        if (this.repository == null) {
            throw new BeanInitializationException(
                    "JavaBean property 'repository' not set");
        }
        if (this.resourceTypeTree == null) {
            throw new BeanInitializationException(
                    "JavaBean property 'resourceTypeTree' not set");
        }
        if (this.valueFormatter == null) {
            throw new BeanInitializationException(
                    "JavaBean property 'valueFormatter' not set");
        }
    }

    public void setForProcessing(boolean forProcessing) {
        this.forProcessing = forProcessing;
    }

    public void setValueFormatter(ValueFormatter valueFormatter) {
        this.valueFormatter = valueFormatter;
    }

    public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
    }

    protected String getDescriptionInternal() {
        return DESCRIPTION;
    }

    protected Map getParameterDescriptionsInternal() {
        Map map = new HashMap();
        map.put(PARAMETER_ID, PARAMETER_ID_DESC);
        return map;
    }

}
