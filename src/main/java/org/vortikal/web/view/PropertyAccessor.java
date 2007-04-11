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
package org.vortikal.web.view;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.ValueFormatter;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.repository.URIUtil;
import org.vortikal.web.RequestContext;

public class PropertyAccessor implements InitializingBean {

    private Repository repository;
    private ValueFormatter formatter;
    private ResourceTypeTree resourceTypeTree;

    public String propertyValue(String uri, String prefix, String name, String format) {
        
            String currentUri = RequestContext.getRequestContext().getResourceURI();
            
            if (uri == null || uri.equals("")) {
                uri = currentUri;
            } else {
                uri = URIUtil.getAbsolutePath(uri, currentUri);
            }
            
            if (prefix != null && prefix.equals("")) {
                prefix = null;
            }

            PropertyTypeDefinition def =
                resourceTypeTree.getPropertyDefinitionByPrefix(prefix, name);

            if (def == null) {
                return "";
            }
            
            if (format != null && format.equals("")) {
                format = null;
            }

            String token = SecurityContext.getSecurityContext().getToken();
            HttpServletRequest request = RequestContext.getRequestContext().getServletRequest();
            Locale locale = 
                new org.springframework.web.servlet.support.RequestContext(request).getLocale();

            try {
                Resource resource = this.repository.retrieve(token, uri, true);
                Property prop = resource.getProperty(def);
                if (prop != null)
                    return formatter.valueToString(prop.getValue(), format, locale);
            } catch (Exception e) {
            }

            return "";
    }

    public void afterPropertiesSet() throws Exception {
        if (this.repository == null) throw new BeanInitializationException(
        "Property 'repository' must be set");
        if (this.formatter == null) throw new BeanInitializationException(
        "Property 'formatter' must be set");
        if (this.resourceTypeTree == null) throw new BeanInitializationException(
        "Property 'resourceTypeTree' must be set");
    }

    public void setFormatter(ValueFormatter formatter) {
        this.formatter = formatter;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
    }
}
