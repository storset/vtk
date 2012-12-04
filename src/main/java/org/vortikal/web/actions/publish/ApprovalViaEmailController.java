/* Copyright (c) 2012 University of Oslo, Norway
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

package org.vortikal.web.actions.publish;

import java.util.HashMap;
import java.util.Map;

import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.edit.editor.ResourceWrapperManager;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.Revision;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.security.Principal;
import org.vortikal.util.mail.MailExecutor;
import org.vortikal.util.mail.MailHelper;
import org.vortikal.util.mail.MailTemplateProvider;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;

public class ApprovalViaEmailController implements Controller {
    private String viewName;
    private ResourceWrapperManager resourceManager;
    private MailExecutor mailExecutor;
    private MailTemplateProvider mailTemplateProvider;
    private Service manageService;
    private String defaultSender;
    private PropertyTypeDefinition editorialContactsPropDef;

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        String token = requestContext.getSecurityToken();
        Repository repository = requestContext.getRepository();
        Path uri = requestContext.getResourceURI();
        Principal principal = requestContext.getPrincipal();

        Resource resource = repository.retrieve(token, uri, true);
        if (resource == null) {
            return null;
        }

        Map<String, Object> model = new HashMap<String, Object>();
        String method = request.getMethod();
        
        String emailFrom = principal.getQualifiedName();

        Property editorialContactsProp = resource.getProperty(editorialContactsPropDef);
        if (editorialContactsProp != null) {
            Value[] editorialContactsVals = editorialContactsProp.getValues();
            StringBuilder sb = new StringBuilder();
            for (Value editorialContactsVal : editorialContactsVals) {
                sb.append(editorialContactsVal.getStringValue() + ", ");
            }
            String editorialContacts = sb.toString();
            if (editorialContacts.length() > 2) {
                model.put("editorialContacts", editorialContacts.substring(0, editorialContacts.length() - 2));
            }
        }
        
        String title = resource.getTitle();
        String url = manageService.constructURL(uri).toString();
        String[] subjectParams = {getLocalizedMsg(request, "resourcetype.name." + resource.getResourceType(), new Object[0])};
        String subject = getLocalizedMsg(request, "send-to-approval.subject", subjectParams);
        String mailBody = mailTemplateProvider.generateMailBody(title, url, emailFrom, "", "");

        if (method.equals("POST")) {
            String emailTo = request.getParameter("emailTo");
            String yourComment = request.getParameter("yourComment");
            if (StringUtils.isBlank(emailTo)) {
                if (StringUtils.isNotBlank(emailTo)) {
                    model.put("emailSavedTo", emailTo);
                }
                if (StringUtils.isNotBlank(yourComment)) {
                    model.put("yourSavedComment", yourComment);
                }
                model.put(MailHelper.RESPONSE_MODEL, MailHelper.RESPONSE_EMPTY_FIELDS);
            } else {
                try {
                    String comment = "";
                    if (StringUtils.isNotBlank(yourComment)) {
                        comment = yourComment;
                    }
                    boolean validAddresses = true;
                    String[] emailMultipleTo = emailTo.split(",");
                    for (String addr: emailMultipleTo) {
                        if (!MailExecutor.isValidEmail(addr)) {
                            validAddresses = false;
                            break;
                        }
                    }
                    if (!emailFrom.endsWith("@localhost")) {
                        validAddresses = validAddresses && MailExecutor.isValidEmail(emailFrom);
                    } else {
                        emailFrom = defaultSender;
                    }
                    if (validAddresses) {
                        mailBody = mailTemplateProvider.generateMailBody(title, url, emailFrom, comment, "");
                        
                        

                        MimeMessage mimeMessage = mailExecutor.createMimeMessage(
                                mailBody,
                                emailMultipleTo,
                                emailFrom,
                                true,
                                subject
                        );

                        mailExecutor.enqueue(mimeMessage);

                        model.put("emailSentTo", emailTo);
                        model.put(MailHelper.RESPONSE_MODEL, MailHelper.RESPONSE_OK);
                    } else {
                        model.put("emailSavedTo", emailTo);

                        if (!StringUtils.isBlank(yourComment)) {
                            model.put("yourSavedComment", yourComment);
                        }
                        model.put(MailHelper.RESPONSE_MODEL, MailHelper.RESPONSE_INVALID_EMAILS);
                    }
                } catch (Exception mtex) { // Unreachable because of thread
                    model.put(MailHelper.RESPONSE_MODEL, MailHelper.RESPONSE_GENERAL_FAILURE);
                    model.put(MailHelper.RESPONSE_MODEL + "Msg", mtex.getMessage());
                }
            }
        }
        model.put("emailSubject", subject);
        model.put("emailBody", mailBody);
        model.put("resource", resourceManager.createResourceWrapper());
        return new ModelAndView(viewName, model);
    }
    
    private String getLocalizedMsg(HttpServletRequest request, String key, Object[] params) {
        org.springframework.web.servlet.support.RequestContext springRequestContext = new org.springframework.web.servlet.support.RequestContext(
                request);
        if (params != null) {
            return springRequestContext.getMessage(key, params);
        }
        return springRequestContext.getMessage(key);
    }

    @Required
    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    @Required
    public void setResourceManager(ResourceWrapperManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    @Required
    public void setMailExecutor(MailExecutor mailExecutor) {
        this.mailExecutor = mailExecutor;
    }

    @Required
    public void setMailTemplateProvider(MailTemplateProvider mailTemplateProvider) {
        this.mailTemplateProvider = mailTemplateProvider;
    }

    @Required
    public void setManageService(Service manageService) {
        this.manageService = manageService;
    }
    
    @Required
    public void setDefaultSender(String defaultSender) {
        this.defaultSender = defaultSender;
    }

    @Required
    public void setEditorialContactsPropDef(PropertyTypeDefinition editorialContactsPropDef) {
        this.editorialContactsPropDef = editorialContactsPropDef;
    }

}
