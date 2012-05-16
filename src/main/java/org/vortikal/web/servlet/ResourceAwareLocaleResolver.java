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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.LocaleResolver;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.web.RequestContext;

/**
 * Resolves locale for resources. If no locale is set on resource, then
 * a configurable default Locale is returned.
 */
public class ResourceAwareLocaleResolver implements LocaleResolver {

//    protected static final String LOCALE_CACHE_REQUEST_ATTRIBUTE_NAME = ResourceAwareLocaleResolver.class.getName()
//            + ".RequestAttribute";

    private Locale defaultLocale;
    private String trustedToken;
    private Repository repository;

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        RequestContext requestContext = RequestContext.getRequestContext();
        Path uri = requestContext.getResourceURI();
        return resolveResourceLocale(uri);
    }

    public Locale resolveResourceLocale(Path uri) {
        Locale locale = null;
        try {
            Resource r = this.repository.retrieve(this.trustedToken, uri, true);
            locale = r.getContentLocale();
        } catch (Exception e) {}

        if (locale == null) {
            return this.defaultLocale;
        } else {
            return locale;
        }
    }
    
//    public Locale resolveResourceLocaleOld(Path uri) {
//        RequestContext requestContext = RequestContext.getRequestContext();
//        Locale locale = this.defaultLocale;
//        try {
//            RepositoryTraversal traversal = requestContext.rootTraversal(this.trustedToken, uri);
//            final StringBuilder lang = new StringBuilder();
//
//            traversal.traverse(new TraversalCallback() {
//                @Override
//                public boolean callback(Resource resource) {
//                    String s = resource.getContentLanguage();
//                    if (s == null) {
//                        return true;
//                    }
//                    lang.insert(0, s);
//                    return false;
//                }
//
//                @Override
//                public boolean error(Path uri, Throwable error) {
//                    return false;
//                }
//            });
//            if (lang.length() == 0) {
//                return this.defaultLocale;
//            }
//            Locale l = LocaleHelper.getLocale(lang.toString());
//            if (l != null) {
//                locale = l;
//            }
//        } catch (Exception e) {
//        }
//        return locale;
//    }

//    public Locale getNearestAncestorLocale(Path uri) {
//        Path parentUri = uri.getParent();
//
//        Locale locale = null;
//        while (parentUri != null) {
//            try {
//                Resource parent = repository.retrieve(this.trustedToken, parentUri, true);
//                if (StringUtils.isNotBlank(parent.getContentLanguage())) {
//                    locale = LocaleHelper.getLocale(parent.getContentLanguage());
//                    break;
//                }
//                parentUri = parentUri.getParent();
//            } catch (Exception e) {
//                return this.defaultLocale;
//            }
//        }
//        if (locale == null) {
//            locale = this.defaultLocale;
//        }
//        return locale;
//    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        throw new UnsupportedOperationException(
                "This locale resolver does not support explicitly setting the request locale");
    }
    
    /**
     * Set the default locale that this resolver will return if the request does
     * not contain a cookie. If the default locale is not set, the accept header
     * locale of the client is returned.
     */
    public void setDefaultLocale(Locale defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public void setTrustedToken(String trustedToken) {
        this.trustedToken = trustedToken;
    }

    @Required
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

}
