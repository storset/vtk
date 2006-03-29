/* Copyright (c) 2004, University of Oslo, Norway
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.referencedata.ReferenceDataProvider;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.ServiceUnlinkableException;

/**
 * Model builder that has a list of locales, providing URLs to switch
 * locale to each one (if they are not already the current
 * locale). The URLs are made available in the submodel of a
 * configurable name (<code>locales</code> by default).
 * 
 * Configurable properties:
 * <ul>
 *   <li><code>repository</code> - the content repository
 *   <li><code>modelName</code> - the name to use for the submodel
 *       generated (default <code>locales</code>)
 *   <li><code>locales</code> - a map containing keys that are
 *       interpreted as locale names. The values are required to be
 *       <code>org.vortikal.web.service.Service</code> objects, used
 *       for constructing URLs for changing the locale.
 * </ul>
 * 
 * Model data provided:
 * <ul>
 *   <li><code>localeServiceURLs</code> -
 *       (<code>java.util.Map</code>): for each locale, a URL to the
 *       service that changes the locale to that of the key
 *   <li><code>localeServiceNames</code> - the name of the keys in the
 *   URL map
 *   <li><code>currentLocale</code> - the name of the current locale
 * </ul>
 * 
 */
public class SwitchLocaleProvider implements InitializingBean, ReferenceDataProvider {

    private Repository repository = null;
    private String modelName = "locales";
    private Map locales;


    public void setRepository(Repository repository) {
        this.repository = repository;
    }


    public void setModelName(String modelName) {
        this.modelName = modelName;
    }


    public void setLocales(Map locales) {
        this.locales = locales;
    }


    public void afterPropertiesSet() throws Exception {
        if (this.repository == null) {
            throw new BeanInitializationException(
                "Bean property 'repository' must be set");
        }
        if (this.modelName == null) {
            throw new BeanInitializationException(
                "Bean property 'modelName' must be set");
        }
        
        if (this.locales == null) {
            throw new BeanInitializationException(
                "Bean property 'locales' must be set");
        }
    }


    public void referenceData(Map model, HttpServletRequest request)
        throws Exception {
        Map localeMap = new HashMap();

        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        
        Principal principal = securityContext.getPrincipal();
        Resource resource = repository.retrieve(
            securityContext.getToken(), requestContext.getResourceURI(), false);

        org.springframework.web.servlet.support.RequestContext springContext =
            new org.springframework.web.servlet.support.RequestContext(request);
        String currentLocale = springContext.getLocale().toString();

        Map localeServiceURLs = new HashMap();
        List localeServiceNames = new ArrayList();
        for (Iterator i = locales.keySet().iterator(); i.hasNext();) {
            String key = (String) i.next();
            Service service = (Service) locales.get(key);
            if (!currentLocale.equals(key)) {
                try {
                    localeServiceNames.add(key);
                    String url = service.constructLink(resource, principal);
                    localeServiceURLs.put(key, url);
                } catch (ServiceUnlinkableException e) { }
            }
        }
        
        localeMap.put("currentLocale", currentLocale);
        localeMap.put("localeServiceNames", localeServiceNames);
        localeMap.put("localeServiceURLs", localeServiceURLs);
        model.put(modelName, localeMap);
    }

}
