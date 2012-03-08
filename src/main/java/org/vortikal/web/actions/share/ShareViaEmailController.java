/* Copyright (c) 2008 University of Oslo, Norway
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

package org.vortikal.web.actions.share;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.edit.editor.ResourceWrapperManager;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.util.mail.MailExecutor;
import org.vortikal.util.mail.MailTemplateProvider;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;

public class ShareViaEmailController implements Controller {
    private String viewName;
    private String siteName;
    private ResourceWrapperManager resourceManager;
    private MailExecutor mailExecutor;
    private MailTemplateProvider mailTemplateProvider;
    private LocaleResolver localeResolver;
    private Service viewService;
    private String displayUpscoping;

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        String token = requestContext.getSecurityToken();
        Repository repository = requestContext.getRepository();
        Path uri = requestContext.getResourceURI();

        Resource resource = repository.retrieve(token, uri, true);
        if (resource == null) {
            return null;
        }

        String language = resource.getContentLanguage();
        if (language == null) {
            Locale locale = localeResolver.resolveLocale(request);
            language = locale.toString();
        }

        Map<String, Object> model = new HashMap<String, Object>();
        String method = request.getMethod();

        if (this.displayUpscoping != null) {
          model.put("displayUpscoping", displayUpscoping);
        }

        if (method.equals("POST")) {
            String emailTo = request.getParameter("emailTo");
            String emailFrom = request.getParameter("emailFrom");
            String yourComment = request.getParameter("yourComment");
            if (StringUtils.isBlank(emailTo) || StringUtils.isBlank(emailFrom)) {
                if (StringUtils.isNotBlank(emailTo)) {
                    model.put("emailSavedTo", emailTo);
                }
                if (StringUtils.isNotBlank(emailFrom)) {
                    model.put("emailSavedFrom", emailFrom);
                }
                if (StringUtils.isNotBlank(yourComment)) {
                    model.put("yourSavedComment", yourComment);
                }
                model.put("tipResponse", "FAILURE-NULL-FORM");
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
                    validAddresses = validAddresses && MailExecutor.isValidEmail(emailFrom);
                    
                    if (validAddresses) {
                        String url = this.viewService.constructURL(uri).toString();

                        MimeMessage mimeMessage = mailExecutor.createMimeMessage(
                                mailTemplateProvider, this.siteName, url, resource.getTitle(), 
                                emailMultipleTo,
                                emailFrom, comment, resource.getTitle());

                        mailExecutor.enqueue(mimeMessage);

                        model.put("emailSentTo", emailTo);
                        model.put("tipResponse", "OK");
                    } else {
                        model.put("emailSavedTo", emailTo);
                        model.put("emailSavedFrom", emailFrom);

                        if (!StringUtils.isBlank(yourComment)) {
                            model.put("yourSavedComment", yourComment);
                        }
                        model.put("tipResponse", "FAILURE-INVALID-EMAIL");
                    }
                } catch (Exception mtex) { // Unreachable because of thread
                    model.put("tipResponse", "FAILURE");
                    model.put("tipResponseMsg", mtex.getMessage());
                }
            }
        }
        model.put("resource", this.resourceManager.createResourceWrapper());
        return new ModelAndView(this.viewName, model);
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
    public void setLocaleResolver(LocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }


    @Required
    public void setViewService(Service viewService) {
        this.viewService = viewService;
    }


    @Required
    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    @Required
    public void setDisplayUpscoping(String displayUpscoping) {
        this.displayUpscoping = displayUpscoping;
    }

}
