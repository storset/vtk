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
package org.vortikal.resourcemanagement;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.text.html.HtmlPage;
import org.vortikal.text.html.HtmlPageParser;
import org.vortikal.util.io.StreamUtil;
import org.vortikal.web.RequestContext;
import org.vortikal.web.decorating.HtmlPageContent;
import org.vortikal.web.decorating.ParsedHtmlDecoratorTemplate;
import org.vortikal.web.decorating.Template;
import org.vortikal.web.decorating.TemplateManager;

public class StructuredResourceDisplayController implements Controller, InitializingBean {

    private Repository repository;
    private String viewName;
    private StructuredResourceManager resourceManager;
    private TemplateManager templateManager;
    private HtmlPageParser htmlParser;
    private String resourceModelKey;
    

    public ModelAndView handleRequest(HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        Path uri = RequestContext.getRequestContext().getResourceURI();
        String token = SecurityContext.getSecurityContext().getToken();
        Resource r = this.repository.retrieve(token, uri, true);
        
        InputStream stream = this.repository.getInputStream(token, uri, true);
        byte[] buff = StreamUtil.readInputStream(stream);
        String encoding = r.getCharacterEncoding();
        if (encoding == null) encoding = "utf-8";
        String source = new String(buff, encoding);
        
        StructuredResourceDescription desc = this.resourceManager.get(r.getResourceType());
        StructuredResource res = new StructuredResource(desc);
        res.parse(source);

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("resource", r);
        model.put(this.resourceModelKey, res);
        
        HtmlPageContent content = 
            renderInitialPage(res, model, request);
        model.put("page", content.getHtmlContent());
        return new ModelAndView(this.viewName, model);
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public void setResourceManager(StructuredResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }
    
    @SuppressWarnings("unchecked")
    public HtmlPageContent renderInitialPage(StructuredResource res, Map model, HttpServletRequest request) throws Exception {
        String html = "<html><head><title></title></head><body></body></html>";
        ByteArrayInputStream in = new ByteArrayInputStream(html.getBytes("utf-8"));
        final HtmlPage dummy = this.htmlParser.parse(in, "utf-8");
        
        HtmlPageContent content = new HtmlPageContent() {
            public HtmlPage getHtmlContent() {
                return dummy;
            }
            public String getContent() {
                return dummy.getStringRepresentation();
            }
            public String getOriginalCharacterEncoding() {
                return dummy.getCharacterEncoding();
            }
        };

        String templateRef = res.getType().getName();
        Template t = this.templateManager.getTemplate(templateRef);
        if (!(t instanceof ParsedHtmlDecoratorTemplate)) {
            throw new IllegalStateException("Template must be of class " 
                    + ParsedHtmlDecoratorTemplate.class.getName());
        }
        ParsedHtmlDecoratorTemplate template = (ParsedHtmlDecoratorTemplate) t;
        ParsedHtmlDecoratorTemplate.Execution execution = 
            (ParsedHtmlDecoratorTemplate.Execution) template.newTemplateExecution(content, request, model);
        content = (HtmlPageContent) execution.render();
        return content;
    }

    public void afterPropertiesSet() {
    }

    @Required public void setTemplateManager(TemplateManager templateManager) {
        this.templateManager = templateManager;
    }

    @Required public void setHtmlParser(HtmlPageParser htmlParser) {
        this.htmlParser = htmlParser;
    }

    @Required public void setResourceModelKey(String resourceModelKey) {
        this.resourceModelKey = resourceModelKey;
    }
}
