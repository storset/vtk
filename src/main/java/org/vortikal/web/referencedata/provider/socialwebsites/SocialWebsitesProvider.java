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
package org.vortikal.web.referencedata.provider.socialwebsites;

import java.net.URLEncoder;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.text.html.HtmlUtil;
import org.vortikal.web.RequestContext;
import org.vortikal.web.referencedata.ReferenceDataProvider;

public class SocialWebsitesProvider implements ReferenceDataProvider {

    private Repository repository = null;
    private List<SocialWebsite> socialWebsites = null;
    private HtmlUtil htmlUtil = null;


    public void setRepository(Repository repository) {
        this.repository = repository;
    }


    @SuppressWarnings("unchecked")
    public void referenceData(Map model, HttpServletRequest request) throws Exception {

        SecurityContext securityContext = SecurityContext.getSecurityContext();
        RequestContext requestContext = RequestContext.getRequestContext();

        Resource resource = this.repository.retrieve(securityContext.getToken(), requestContext.getResourceURI(), true);

        ListIterator it = socialWebsites.listIterator();

        while (it.hasNext()) {

            SocialWebsite sw = (SocialWebsite) it.next();

            Class swClass = sw.getClass();
            String name = swClass.getSimpleName();

            String introduction = "";
            Property propIntroduction = resource.getProperty(Namespace.STRUCTURED_RESOURCE_NAMESPACE, "introduction");

            if (propIntroduction != null) {
                introduction = (propIntroduction.getStringValue() != "") ? URLEncoder.encode(htmlUtil
                        .flatten(propIntroduction.getStringValue()), "utf-8"): "";
                if (introduction.length() > 250) {
                    introduction = introduction.substring(0, 250) + "...";
                }
            }

            String title = URLEncoder.encode(resource.getTitle(), "utf-8");

            sw.generateLink(request.getRequestURL().toString(), title, introduction);
            sw.setName(name);

        }

        model.put("socialWebsites", this.socialWebsites);

    }


    public List<SocialWebsite> getSocialWebsites() {
        return socialWebsites;
    }


    public void setSocialWebsites(List<SocialWebsite> socialWebsites) {
        this.socialWebsites = socialWebsites;
    }


    public Repository getRepository() {
        return repository;
    }


    public HtmlUtil getHtmlUtil() {
        return htmlUtil;
    }


    public void setHtmlUtil(HtmlUtil htmlUtil) {
        this.htmlUtil = htmlUtil;
    }
}