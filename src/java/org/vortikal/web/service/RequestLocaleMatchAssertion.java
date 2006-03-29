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
package org.vortikal.web.service;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.LocaleResolver;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;



/**
 * Assertion that matches on the locale of the request. The locale is
 * resolved using Spring's standard locale resolver functionality.
 *
 * <p>Configurable properties:
 * <ul>
 *    <li><code>locale</code> - a {@link Locale} to match
 *    <li><code>localeResolver</code> - a {@link LocaleResolver}
 *        that determines the request locale.
 *    <li><code>invert</code> - a boolean indicating whether to perform an inverted
 *        match. Default is false.
 * </ul>
 */
public class RequestLocaleMatchAssertion
  implements Assertion, InitializingBean {
    
    private Locale locale;
    private LocaleResolver localeResolver;
    private boolean invert = false;
    

    public void setLocale(Locale locale) {
        this.locale = locale;
    }


    public void setLocaleResolver(LocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }


    public void setInvert(boolean invert) {
        this.invert = invert;
    }



    public void afterPropertiesSet() throws Exception {
        if (this.locale == null) {
            throw new BeanInitializationException(
                "Property 'locale' not set");
        }
        if (this.localeResolver == null) {
            throw new BeanInitializationException(
                "Property 'localeResolver' not set");
        }
    }



    public boolean conflicts(Assertion assertion) {
        if (assertion instanceof RequestLocaleMatchAssertion) {

            Locale locale = ((RequestLocaleMatchAssertion) assertion).locale;
            boolean invert = ((RequestLocaleMatchAssertion) assertion).invert;

            if (!match(locale)) {
                return true;
            } else if (this.invert != invert) {
                return true;
            }
        }
        return false;
    }



    private boolean match(Locale locale) {
        
        if (!this.locale.getLanguage().equals(locale.getLanguage())) {
            return false;
        }
        if (!this.locale.getCountry().equals(locale.getCountry())) {
            return false;
        }
        if (!this.locale.getVariant().equals(locale.getVariant())) {
            return false;
        }
        return true;
    }
    

    public String toString() {
        StringBuffer sb = new StringBuffer();
		
        sb.append(super.toString());
        sb.append("; locale = ").append(this.locale);
        sb.append("; invert = ").append(this.invert);
        return sb.toString();
    }


    public boolean processURL(URL url, Resource resource, Principal principal, boolean match) {
        return true;
    }


    public boolean matches(HttpServletRequest request, Resource resource, Principal principal) {
        Locale locale = localeResolver.resolveLocale(request);
        boolean match = match(locale);
        if (invert) {
            return !match;
        }
        return match;
    }
    
}
