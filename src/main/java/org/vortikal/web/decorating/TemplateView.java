/* Copyright (c) 2008, University of Oslo, Norway
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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.text.html.HtmlPage;

public class TemplateView implements HtmlRenderer, InitializingBean {
    
    private TemplateManager templateManager;
    private String templateRef;
    
    public String getContentType() {
        return null;
    }

    @SuppressWarnings("rawtypes")
    public HtmlPageContent render(Map model, HttpServletRequest request) throws Exception {
        Object o = model.get("page");
        if (o == null || !(o instanceof HtmlPage)) {
            throw new RuntimeException("No page available for rendering");
        }
        final HtmlPage page = (HtmlPage) o;
        HtmlPageContent content = new HtmlPageContent() {
            public HtmlPage getHtmlContent() {
                return page;
            }
            public String getContent() {
                return page.getStringRepresentation();
            }
            public String getOriginalCharacterEncoding() {
                return page.getCharacterEncoding();
            }
        };
        
        ParsedHtmlDecoratorTemplate template = getTemplate();
        @SuppressWarnings("unchecked")
        ParsedHtmlDecoratorTemplate.Execution execution = 
            (ParsedHtmlDecoratorTemplate.Execution) 
            template.newTemplateExecution(content, request, model, new HashMap<String, Object>());
        return execution.render();
    }
    

    @SuppressWarnings("rawtypes")
    public void render(Map model, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        HtmlPageContent content = render(model, request);
        
        String characterEncoding = content.getHtmlContent().getCharacterEncoding();
        byte[] result = content.getContent().getBytes(
                characterEncoding);
        response.setHeader("Content-Type", "text/html;charset=" + characterEncoding);
        response.setContentLength(result.length);
        ServletOutputStream outStream = response.getOutputStream();
        outStream.write(result);
        outStream.flush();
        outStream.close();
    }

    @Required public void setTemplateRef(String templateRef) {
        this.templateRef = templateRef;
    }

    @Required public void setTemplateManager(TemplateManager templateManager) {
        this.templateManager = templateManager;
    }

    public void afterPropertiesSet() {
        if (this.templateRef == null) {
            throw new BeanInitializationException("Unable to get template: no templateRef specified");
        }
        try {
            getTemplate();
        } catch (Throwable t) {
            throw new BeanInitializationException(
                    "Unable to instantiate template '" + this.templateRef + "'", t);
        }
    }

    private ParsedHtmlDecoratorTemplate getTemplate() throws Exception {
        Template t = this.templateManager.getTemplate(this.templateRef);
        if (!(t instanceof ParsedHtmlDecoratorTemplate)) {
            throw new IllegalStateException("Template must be of class " 
                    + ParsedHtmlDecoratorTemplate.class.getName());
        }
        return (ParsedHtmlDecoratorTemplate) t;
    }
}
