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

import org.springframework.web.servlet.LocaleResolver;
import org.vortikal.repository.Path;
import org.vortikal.repository.Resource;
import org.vortikal.util.repository.LocaleHelper;
import org.vortikal.web.RequestContext;
import org.vortikal.web.RequestContext.RepositoryTraversal;
import org.vortikal.web.RequestContext.TraversalCallback;

/**
 * Resolves locale for the current resource.
 * 
 * <p>The locale is set to the first match:
 * <ul>
 *   <li>The resource {@link Resource#getContentLanguage() contentLanguage}
 *   <li>The nearest parent with {@link Resource#getContentLanguage() contentLanguage} set
 *   <li>defaultLocale
 *   
 */
public class ResourceAwareLocaleResolver implements LocaleResolver {
    
    protected static final String LOCALE_CACHE_REQUEST_ATTRIBUTE_NAME =
        ResourceAwareLocaleResolver.class.getName() + ".RequestAttribute";
    
    private Locale defaultLocale;
    private String trustedToken = null;

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        RequestContext requestContext = RequestContext.getRequestContext();
        Path uri = requestContext.getResourceURI();
        // Try getting request from RequestContext if it's not provided
        if (request == null) {
            request = requestContext.getServletRequest();
        }
        return resolveResourceLocale(uri);
    }


    public Locale resolveResourceLocale(Path uri) {
        RequestContext requestContext = RequestContext.getRequestContext();
        Locale locale = this.defaultLocale;
        try {
            RepositoryTraversal traversal = requestContext.rootTraversal(this.trustedToken, uri);
            final StringBuilder lang = new StringBuilder();

            traversal.traverse(new TraversalCallback() {
                @Override
                public boolean callback(Resource resource) {
                    String s = resource.getContentLanguage();
                    if (s == null) {
                        return true;
                    }
                    lang.insert(0, s);
                    return false;
                }

                @Override
                public boolean error(Path uri, Throwable error) {
                    return false;
                }});
            if (lang.length() == 0) {
                return this.defaultLocale;
            }
            Locale l = LocaleHelper.getLocale(lang.toString());
            if (l != null) {
                locale = l;
            }
        } catch (Exception e) { }
        return locale;
    }
    
    /*
    @SuppressWarnings("unchecked")
    public Locale resolveResourceLocale(HttpServletRequest request, Path uri) {

        Map<Path, Locale> localeCache = null;
        if (request != null) {
            localeCache = (Map<Path,Locale>) request.getAttribute(LOCALE_CACHE_REQUEST_ATTRIBUTE_NAME);
            if (localeCache != null) {
                Locale locale = localeCache.get(uri);
                if (locale != null) {
                    return locale;
                }
            }
        }
        
        String token = this.trustedToken;
        if (token == null) {
            token = SecurityContext.getSecurityContext().getToken();
        }
        
        Locale locale = null;
        NearestAncestorLocale nearestAncestorLocale = null; // Used by caching mechanism
        try {
            Resource resource = this.repository.retrieve(token, uri, true);
            locale = LocaleHelper.getLocale(resource.getContentLanguage());

            if (locale == null && localeCache != null) {
                // Check for parent locale in cache (we might get lucky)
                Path parentUri = uri.getParent();
                if (parentUri != null) {
                    locale = localeCache.get(parentUri);
                }
            }

            if (locale == null) {
                // Check for the nearest ancestor that has a locale set and use it (repository lookup)
                nearestAncestorLocale = getNearestAncestorLocale(token, resource);
                if (nearestAncestorLocale != null){
                    locale = nearestAncestorLocale.locale;
                }
            }
            
            if (locale == null) {
                // If no ancestor has a locale set, use the default of the host
            	locale = this.defaultLocale;
                nearestAncestorLocale = new NearestAncestorLocale(locale, null); // Used by caching mechanism
            }
        } catch (Throwable t) { 
            // Don't cache value if something goes terribly wrong. Just return default.
            return this.defaultLocale;
        }

        // locale should be != null by this point

        // Cache locale for given path, and trail up to locale-defining ancestor (if any)
        if (request != null) {
            if (localeCache == null) {
                localeCache = new HashMap<Path,Locale>();
                request.setAttribute(LOCALE_CACHE_REQUEST_ATTRIBUTE_NAME, localeCache);
            }

            if (nearestAncestorLocale != null) {
                // Locale found in ancestor or host default, cache the info.
                Path localeAncestor = nearestAncestorLocale.uri;
                if (localeAncestor == null) {
                    // Host default locale was selected for the current URI, cache that fact
                    for (Path path: uri.getAncestors()) {
                        localeCache.put(path, locale);
                    }
                } else {
                    // Some ancestor had the locale set, cache up to ancestor
                    Path ancestor = uri.getParent();
                    int localeAncestorDepth = localeAncestor.getDepth();
                    while (ancestor != null && ancestor.getDepth() >= localeAncestorDepth) {
                        localeCache.put(ancestor, locale);
                        ancestor = ancestor.getParent();
                    }
                }
            }

            localeCache.put(uri, locale);            
        }
        
        return locale;
    }
    

    private NearestAncestorLocale getNearestAncestorLocale(String token,
                                            Resource resource) throws Exception {
        Path parentUri = resource.getURI().getParent();

        Locale locale = null;
        while (parentUri != null) {
            Resource parent = this.repository.retrieve(token, parentUri, true);
            if (StringUtils.isNotBlank(parent.getContentLanguage())) {
                locale = LocaleHelper.getLocale(parent.getContentLanguage());
                break;
            }
            parentUri = parentUri.getParent();
        }

        if (locale != null) {
            return new NearestAncestorLocale(locale, parentUri);
        }

        return null; // No luck
    }

    private static class NearestAncestorLocale {
        Locale locale;
        Path uri;

        public NearestAncestorLocale(Locale locale, Path uri) {
            this.locale = locale;
            this.uri = uri;
        }
    }

    */
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        throw new UnsupportedOperationException(
                "This locale resolver does not support explicitly setting the request locale");
    }

    
    /**
     * Set the default locale that this resolver will return if
     * the request does not contain a cookie. If the default
     * locale is not set, the accept header locale of the client
     * is returned.
     */
    public void setDefaultLocale(Locale defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public void setTrustedToken(String trustedToken) {
        this.trustedToken = trustedToken;
    }

}
