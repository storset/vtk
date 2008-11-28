/* Copyright (c) 2005, 2008 University of Oslo, Norway
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

package org.vortikal.web.controller.emailafriend;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import freemarker.template.Configuration;


public class MailTemplateProvider {

    private Configuration configuration;
    private String freemarkerTemplate;

    public String generateMailBody(String title, String articleURI, String mailFrom, String comment,
            String serverHostname, String serverHostnameShort, int serverPort, String language) throws Exception {

        String articleFullUri = "";

        if (serverPort != 80) {
            articleFullUri = "http://" + serverHostname + ":" + serverPort + articleURI + " \n\n";
        } else {
            articleFullUri = "http://" + serverHostname + articleURI + " \n\n";
        }

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("title", title);
        model.put("mailFrom", mailFrom);
        model.put("comment", comment);
        model.put("serverHostname", serverHostname);
        model.put("serverHostnameShort", serverHostnameShort);
        model.put("articleFullUri", articleFullUri);
        model.put("language", language);

        // Mail-template from freemarker file.
        // TODO: Localization in freemarker file with vrtx.msg() from
        // messages.properties instead(?) Importing vortikal.ftl gives error..
        // TODO: Put ${choosenTemplate} in vortikal.properties to let sites use
        // different templates.
        String mailMessage = "";

        try {
            mailMessage = FreeMarkerTemplateUtils.processTemplateIntoString(configuration
                    .getTemplate(freemarkerTemplate), model);
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return mailMessage;
    }

    @Required
    public void setFreemarkerTemplate(String freemarkerTemplate) {
        this.freemarkerTemplate = freemarkerTemplate;
    }

    @Required
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
}
