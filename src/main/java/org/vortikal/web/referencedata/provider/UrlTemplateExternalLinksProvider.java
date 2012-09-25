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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.security.Principal;
import org.vortikal.web.RequestContext;
import org.vortikal.web.referencedata.ReferenceDataProvider;
import org.vortikal.web.service.Service;

/**
 * Provides external links/URLs by use of template/pattern and dynamic values. 
 * 
 * Supported fields: "%{url}" (service URL)
 * and "%{foo:bar}" (property values of current resource).
 * 
 * Example: http://www.foo.com/share?url=%{url}
 */
public class UrlTemplateExternalLinksProvider implements ReferenceDataProvider {

    private static final Pattern FIELD_PATTERN = Pattern.compile("(%\\{[^\\}]+\\})");
    private static final String URL_ENCODING_CHARSET = "utf-8";

    private int fieldValueSizeLimit = 250;
    private String fieldValueTruncationIndicator = "...";
    private Map<String, UrlTemplate> urlTemplates;
    private String modelKey = "externalLinks";


    @Override
    public void referenceData(Map<String, Object> model, HttpServletRequest request) throws Exception {

        RequestContext requestContext = RequestContext.getRequestContext();
        String token = requestContext.getSecurityToken();
        Repository repository = requestContext.getRepository();
        
        RenderContext ctx = new RenderContext();
        ctx.resource = repository.retrieve(token, requestContext.getResourceURI(), true);
        ctx.principal = requestContext.getPrincipal();
        ctx.service = requestContext.getService();
        ctx.request = request;

        List<ExternalLink> links = new ArrayList<ExternalLink>();
        for (String externalLinkName : this.urlTemplates.keySet()) {
            UrlTemplate template = this.urlTemplates.get(externalLinkName);
            String encodedUrl = template.renderEncodedUrl(ctx);
            ExternalLink link = new ExternalLink();
            link.setName(externalLinkName);
            link.setUrl(encodedUrl);
            links.add(link);
        }
        model.put(this.modelKey, links);
    }

    private class UrlTemplate {
        private List<TemplateNode> templateNodes;

        UrlTemplate(String urlTemplate) {
            List<TemplateNode> templateNodes = new ArrayList<TemplateNode>();

            Matcher fieldPatternMatcher = FIELD_PATTERN.matcher(urlTemplate);
            int pos = 0;
            while (fieldPatternMatcher.find()) {
                int fieldStart = fieldPatternMatcher.start(1);
                int fieldEnd = fieldPatternMatcher.end(1);

                if (fieldStart > pos) {
                    templateNodes.add(new StaticEncodedText(urlTemplate.substring(pos, fieldStart)));
                }
                String fieldId = urlTemplate.substring(fieldStart + 2, fieldEnd - 1);
                templateNodes.add(dynamicNode(fieldId));

                pos = fieldEnd;
            }
            if (pos < urlTemplate.length()) {
                templateNodes.add(new StaticEncodedText(urlTemplate.substring(pos, urlTemplate.length())));
            }
            this.templateNodes = templateNodes;
        }


        String renderEncodedUrl(RenderContext ctx) {
            StringBuilder url = new StringBuilder();
            for (TemplateNode node : this.templateNodes) {
                url.append(node.render(ctx));
            }
            return url.toString();
        }
    }

    // Factory method for dynamic node renderers. Might consider making this configurable (only if need arises).
    private TemplateNode dynamicNode(String field) {
        TemplateNode node;

        if ("url".equals(field)) {
            node = new ServiceUrl();
        } else {
            // Assume it's a reference to a resource property
            String prefix = null;
            String name = null;
            int idx = field.indexOf(":");
            if (idx != -1) {
                prefix = field.substring(0, idx);
                name = field.substring(idx + 1);
            } else {
                name = field;
            }

            node = new TruncatingWrapper(new ResourcePropertyValue(prefix, name));
        }
        return new UrlEncodingWrapper(node);
    }

    @SuppressWarnings("unused")
    private class RenderContext {
        Resource resource;
        Principal principal;
        Service service;
        HttpServletRequest request;
    }

    private interface TemplateNode {
        String render(RenderContext ctx);
    }

    private class StaticEncodedText implements TemplateNode {
        private String text;
        StaticEncodedText(String text) {
            this.text = text;
        }
        public String render(RenderContext ctx) {
            return this.text;
        }
    }

    private class ServiceUrl implements TemplateNode {
        public String render(RenderContext ctx) {
            return ctx.service.constructLink(ctx.resource, ctx.principal);
        }
    }

    private class ResourcePropertyValue implements TemplateNode {
        private String prefix;
        private String name;

        ResourcePropertyValue(String prefix, String name) {
            this.prefix = prefix;
            this.name = name;
        }

        public String render(RenderContext ctx) {
            String retVal = "";
            Property prop = ctx.resource.getPropertyByPrefix(this.prefix, this.name);
            if (prop != null) {
                PropertyTypeDefinition def = prop.getDefinition();
                if (def != null) {
                    if (def.getType() == PropertyType.Type.HTML) {
                        retVal = prop.getFormattedValue("flattened", null);
                    } else {
                        retVal = prop.getFormattedValue();
                    }
                }
            }
            return retVal;
        }
    }

    private class UrlEncodingWrapper implements TemplateNode {
        private TemplateNode wrappedNode;
        UrlEncodingWrapper(TemplateNode wrapped) {
            this.wrappedNode = wrapped;
        }

        public String render(RenderContext ctx) {
            try {
                return URLEncoder.encode(this.wrappedNode.render(ctx), URL_ENCODING_CHARSET);
            } catch (UnsupportedEncodingException ue) {
                return "";
            }
        }
    }

    private class TruncatingWrapper implements TemplateNode {
        private TemplateNode wrappedNode;
        TruncatingWrapper(TemplateNode wrapped) {
            this.wrappedNode = wrapped;
        }
        public String render(RenderContext ctx) {
            String retVal = this.wrappedNode.render(ctx);
            if (retVal.length() > UrlTemplateExternalLinksProvider.this.fieldValueSizeLimit) {
                retVal = retVal.substring(0, UrlTemplateExternalLinksProvider.this.fieldValueSizeLimit);
                retVal = retVal + UrlTemplateExternalLinksProvider.this.fieldValueTruncationIndicator;
            }
            return retVal;
        }

    }

    @Required
    public void setUrlTemplates(Map<String, String> urlTemplates) {
        this.urlTemplates = new LinkedHashMap<String, UrlTemplate>(urlTemplates.size());
        for (String name : urlTemplates.keySet()) {
            String templateValue = urlTemplates.get(name);
            if (templateValue != null) {
                this.urlTemplates.put(name, new UrlTemplate(templateValue));
            }
        }
    }

    public void setModelKey(String modelKey) {
        this.modelKey = modelKey;
    }

    public void setFieldValueSizeLimit(int fieldValueSizeLimit) {
        this.fieldValueSizeLimit = fieldValueSizeLimit;
    }

    public void setFieldValueTruncationIndicator(String fieldValueTruncationIndicator) {
        this.fieldValueTruncationIndicator = fieldValueTruncationIndicator;
    }
}
