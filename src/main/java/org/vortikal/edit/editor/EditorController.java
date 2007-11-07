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
package org.vortikal.edit.editor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.io.StreamUtil;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.ServiceUnlinkableException;

public class EditorController extends SimpleFormController {

    private Repository repository;
    private List<Service> tooltipServices;

    private PropertyTypeDefinition titlePropDef;
    
    
    
    public EditorController() {
        super();
        setValidator(new Validator() {

            public boolean supports(Class clazz) {
                return Command.class.equals(clazz);
            }

            public void validate(Object target, Errors errors) {
                Command c = (Command) target;
                String title = c.getTitle();
                if (title == null || title.trim().equals("")) {
                    errors.rejectValue("title", "title.missing", "Title cannot be empty");
                }
            }});
    }


    @Override
    protected void doSubmitAction(Object command) throws Exception {
        String token = SecurityContext.getSecurityContext().getToken();
        String uri = RequestContext.getRequestContext().getResourceURI();
        Resource resource = this.repository.retrieve(token, uri, false);

        Command c = (Command) command;
        System.out.println("XXX: " + c.getTitle() + ", " + resource.getTitle());
        if (!resource.getTitle().equals(c.getTitle())) {
            Property prop = resource.getProperty(titlePropDef);
            if (prop == null) {
                prop = resource.createProperty(titlePropDef.getNamespace(), titlePropDef.getName());
            }
            prop.setStringValue(c.getTitle());
            resource = this.repository.store(token, resource);
        }
        byte[] bytes = c.getContent().getBytes(resource.getCharacterEncoding());
        resource = this.repository.storeContent(token, uri, new ByteArrayInputStream(bytes));
        
    }


    @Override
    protected Object formBackingObject(HttpServletRequest request)
            throws Exception {

        String token = SecurityContext.getSecurityContext().getToken();
        Principal principal = SecurityContext.getSecurityContext().getPrincipal();
        String uri = RequestContext.getRequestContext().getResourceURI();
        
        Resource resource = this.repository.retrieve(token, uri, false);
        InputStream is = this.repository.getInputStream(token, uri, false);
        
        byte[] bytes = StreamUtil.readInputStream(is);
        
        String content = new String(bytes, resource.getCharacterEncoding());

        Command command = new Command();
        command.setTitle(resource.getTitle());
        command.setContent(content);

        command.setTooltips(resolveTooltips(resource, principal));
        
        return command;
    }


    @Required public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setTooltipServices(List<Service> tooltipServices) {
        this.tooltipServices = tooltipServices;
    }

    
    public class Command {
        private String content;
        private String title;
        
        private List<Map<String, String>> tooltips;

        public List<Map<String, String>> getTooltips() {
            return tooltips;
        }

        public void setTooltips(List<Map<String, String>> tooltips) {
            this.tooltips = tooltips;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
         
        
    }

    private List<Map<String, String>> resolveTooltips(Resource resource, Principal principal) {
        if (this.tooltipServices == null) {
            return null;
        }
        List<Map<String, String>> tooltips = new ArrayList<Map<String, String>>();
        for (Service service: this.tooltipServices) {
            String url = null;
            try {
                url = service.constructLink(resource, principal);
                Map<String, String> tooltip = new HashMap<String, String>();
                tooltip.put("url", url);
                tooltip.put("messageKey", "plaintextEdit.tooltip." + service.getName());
                tooltips.add(tooltip);
            } catch (ServiceUnlinkableException e) {
                // Ignore
            }
        }
        return tooltips;
    }


    public void setTitlePropDef(PropertyTypeDefinition titlePropDef) {
        this.titlePropDef = titlePropDef;
    }

}
