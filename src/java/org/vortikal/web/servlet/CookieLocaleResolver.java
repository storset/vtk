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
package org.vortikal.web.servlet;

import java.util.Locale;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.util.WebUtils;


/**
 * Extension of the Spring {@link
 * org.springframework.web.servlet.i18n.CookieLocaleResolver
 * CookieLocaleResolver}. Resolves the request locale based on a
 * cookie, with a fallback to a default preconfigured locale.
 *
 * <p>Configurable properties (in addition to those defined by the {@link
 *  org.springframework.web.servlet.i18n.CookieLocaleResolver superclass}):
 * <ul>
 *   <li><code>defaultLocale</code> - a {@link Locale} that is
 *   resolved when a cookie is not present in the request.
 * </ul>
 */
public class CookieLocaleResolver
  extends org.springframework.web.servlet.i18n.CookieLocaleResolver {

    private Locale defaultLocale;

    /**
     * Set the default locale that this resolver will return if
     * the request does not contain a cookie. If the default
     * locale is not set, the accept header locale of the client
     * is returned.
     */
    public void setDefaultLocale(Locale defaultLocale) {
        this.defaultLocale = defaultLocale;
    }


    /**
     * Return the default locale. 
     */
    public Locale getDefaultLocale() {
        return defaultLocale;
    }


    public Locale resolveLocale(HttpServletRequest request) {
        Locale locale = (Locale) request.getAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME);
        if (locale != null) {
            return locale;
        }
        
        Cookie cookie = WebUtils.getCookie(request, getCookieName());
        if (cookie == null) {
            // use default locale if set
            if (this.defaultLocale != null) {
                return this.defaultLocale;
            }
        }
        return super.resolveLocale(request);
    }
    

    public void setLocale(HttpServletRequest request,
                          HttpServletResponse response, Locale locale) {
        super.setLocale(request, response, locale);
        if (locale == null && this.defaultLocale != null) {
            request.setAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME, this.defaultLocale);            
        }
    }
}
