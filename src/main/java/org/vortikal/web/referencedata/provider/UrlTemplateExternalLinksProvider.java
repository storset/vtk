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
package org.vortikal.web.referencedata.provider;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.security.Principal;
import org.vortikal.util.text.SimpleTemplate;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;

/**
 * Provides external links/URLs by use of template/pattern and dynamic values.
 * 
 * Supported fields: "%{url}" (service URL) and "%{foo:bar}" (property values of current resource).
 * 
 * Example: http://www.foo.com/share?url=%{url}
 */
public class UrlTemplateExternalLinksProvider {

    private static final String URL_ENCODING_CHARSET = "utf-8";

    private int truncateLimit = 250;
    private String truncation = "...";
    private Map<String, SimpleTemplate> urlTemplates;
    private Map<String, SimpleTemplate> altUrlTemplates;
    private Service viewService;

    public List<ExternalLink> getTemplates(List<String> altUrls) throws Exception {

        RequestContext requestContext = RequestContext.getRequestContext();
        String token = requestContext.getSecurityToken();
        Repository repository = requestContext.getRepository();

        final RenderContext ctx = new RenderContext();
        ctx.resource = repository.retrieve(token, requestContext.getResourceURI(), true);
        ctx.principal = requestContext.getPrincipal();
        ctx.service = requestContext.getService();

        List<ExternalLink> links = new ArrayList<ExternalLink>();
        for (String externalLinkName : this.urlTemplates.keySet()) {
            SimpleTemplate template;
            if (altUrls.contains(externalLinkName)) {
                template = this.altUrlTemplates.get(externalLinkName);
            } else {
                template = this.urlTemplates.get(externalLinkName);
            }
            if (template != null) {
                final StringBuilder url = new StringBuilder();
                template.apply(new SimpleTemplate.Handler() {
                    @Override
                    public void write(String text) {
                        url.append(text);
                    }
                    @Override
                    public String resolve(String variable) {
                        if ("url".equals(variable)) {
                            String s = ctx.service.constructLink(ctx.resource, ctx.principal);
                            try {
                                return URLEncoder.encode(s, URL_ENCODING_CHARSET);
                            } catch (UnsupportedEncodingException e) {
                                return "";
                            }
                        } else {
                            // Assume it's a reference to a resource property
                            String prefix = null;
                            String name = null;
                            int idx = variable.indexOf(":");
                            if (idx != -1) {
                                prefix = variable.substring(0, idx);
                                name = variable.substring(idx + 1);
                            } else {
                                name = variable;
                            }
                            String retVal = propValue(ctx.resource, prefix, name);
                            if (retVal.length() > truncateLimit) {
                                retVal = retVal.substring(0, truncateLimit);
                                retVal = retVal + truncation;
                            }
                            try {
                                return URLEncoder.encode(retVal, URL_ENCODING_CHARSET);
                            } catch (UnsupportedEncodingException e) {
                                return "";
                            }
                        }
                    }
                });
                ExternalLink link = new ExternalLink();
                link.setName(externalLinkName);
                link.setUrl(url.toString());
                links.add(link);
            }
        }
        return links;
    }

    
    private static class RenderContext {
        Resource resource;
        Principal principal;
        Service service;
    }

    private String propValue(Resource resource, String prefix, String name) {
        String retVal = "";
        Property prop = resource.getPropertyByPrefix(prefix, name);
        if (prop == null) {
            prop = resource.getPropertyByPrefix("resource", name); // Try structured namespace
        }
        if (prop != null) {
            PropertyTypeDefinition def = prop.getDefinition();
            if (def != null) {
                if (def.getType() == PropertyType.Type.HTML) {
                    retVal = prop.getFormattedValue("flattened", null);
                } else {
                    retVal = prop.getFormattedValue();
                    if (!retVal.isEmpty() && def.getType() == PropertyType.Type.IMAGE_REF) { // Construct absolute URLs
                        try {
                            if (!retVal.startsWith("/")) {
                                Path currentCollection = RequestContext.getRequestContext().getCurrentCollection();
                                retVal = currentCollection.expand(retVal).toString();
                            }
                            retVal = viewService.constructLink(Path.fromString(retVal));
                        } catch (Exception iae) {
                            retVal = "";
                        }
                    }
                }
            }
        }
        return retVal;
        
    }
    
    @Required
    public void setUrlTemplates(Map<String, String> urlTemplates) {
        this.urlTemplates = new LinkedHashMap<String, SimpleTemplate>(urlTemplates.size());
        for (String name : urlTemplates.keySet()) {
            String templateValue = urlTemplates.get(name);
            if (templateValue != null) {
                this.urlTemplates.put(name, SimpleTemplate.compile(templateValue, "%{", "}"));
            }
        }
    }

    public void setAltUrlTemplates(Map<String, String> urlTemplates) {
        this.altUrlTemplates = new LinkedHashMap<String, SimpleTemplate>(urlTemplates.size());
        for (String name : urlTemplates.keySet()) {
            String templateValue = urlTemplates.get(name);
            if (templateValue != null) {
                this.altUrlTemplates.put(name, SimpleTemplate.compile(templateValue, "%{", "}"));
            }
        }
    }

    public void setTruncateLimit(int truncateLimit) {
        this.truncateLimit = truncateLimit;
    }

    public void setTruncation(String truncation) {
        this.truncation = truncation;
    }

    public void setViewService(Service viewService) {
        this.viewService = viewService;
    }
}
