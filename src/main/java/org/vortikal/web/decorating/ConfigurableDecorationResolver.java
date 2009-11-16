/* Copyright (c) 2007, 2008, University of Oslo, Norway
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
package org.vortikal.web.decorating;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceNotFoundException;
import org.vortikal.repository.TypeInfo;
import org.vortikal.repository.resourcetype.PrimaryResourceTypeDefinition;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.decorating.PathMappingConfig.ConfigEntry;
import org.vortikal.web.decorating.PathMappingConfig.Predicate;
import org.vortikal.web.service.Service;
import org.vortikal.web.servlet.StatusAwareHttpServletResponse;



public class ConfigurableDecorationResolver implements DecorationResolver, InitializingBean {

    private static Log logger = LogFactory.getLog(
        ConfigurableDecorationResolver.class);

    private TemplateManager templateManager;
    private PathMappingConfig config;
    private Path configPath;
    private Properties decorationConfiguration;
    private PropertyTypeDefinition parseableContentPropDef;
    private Repository repository; 
    private boolean supportMultipleTemplates = false;
    private Map<String, RegexpCacheItem> regexpCache = new HashMap<String, RegexpCacheItem>();

    private class RegexpCacheItem {
        String string;
        Pattern compiled;
    }
    
    public void setTemplateManager(TemplateManager templateManager) {
        this.templateManager = templateManager;
    }
    
    public void setDecorationConfiguration(Properties decorationConfiguration) {
        this.decorationConfiguration = decorationConfiguration;
    }

    public void setSupportMultipleTemplates(boolean supportMultipleTemplates) {
        this.supportMultipleTemplates = supportMultipleTemplates;
    }

    public void setParseableContentPropDef(
            PropertyTypeDefinition parseableContentPropDef) {
        this.parseableContentPropDef = parseableContentPropDef;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }
    
    public void setConfig(String config) {
        this.configPath = Path.fromString(config);
    }
    
    public void afterPropertiesSet() {
        if (this.configPath == null) {
            throw new BeanInitializationException(
                "JavaBean property 'config' not set");
        }
        if (this.templateManager == null) {
            throw new BeanInitializationException(
                "JavaBean property 'templateManager' not set");
        }
        if (this.decorationConfiguration == null) {
            throw new BeanInitializationException(
                "JavaBean property 'decorationConfiguration' not set");
        }
        if (this.parseableContentPropDef != null && this.repository == null) {
            throw new BeanInitializationException(
            "JavaBean property 'repository' must be set when property " +
            "'parseableContentPropDef' is set");
        }
        loadConfig();
    }
    
    public void loadConfig() {
        try {
            InputStream inputStream = this.repository.getInputStream(null, this.configPath, true);
            this.config = new PathMappingConfig(inputStream);
        } catch(Throwable t) {
            logger.warn("Unable to load decoration configuration file: " 
                    + this.configPath + ": " + t.getMessage(), t);
        }
    }

    public DecorationDescriptor resolve(HttpServletRequest request,
                                        HttpServletResponse response) throws Exception {

        
        InternalDescriptor descriptor = new InternalDescriptor();
        RequestContext requestContext = RequestContext.getRequestContext();
        Path uri = requestContext.getResourceURI();
        Resource resource = null;
        String token = SecurityContext.getSecurityContext().getToken();
        try {
            resource = this.repository.retrieve(token, uri, true);
        } catch (ResourceNotFoundException e) {
        } catch (AuthorizationException e) {
        } catch (Throwable t) {
            throw new RuntimeException(
                    "Unrecoverable error when decorating '" + uri + "'", t);
        }
        
        String paramString = null;
        
        boolean errorPage = false;
        if (response instanceof StatusAwareHttpServletResponse) {
            int status = ((StatusAwareHttpServletResponse) response).getStatus();
            if (status >= 400) {
                errorPage = true;
                paramString = checkErrorCodeMatch(status);
            }
        }

        if (paramString == null && !errorPage) {
            paramString = checkRegexpMatch(uri.toString());
        }
        
        if (paramString == null && !errorPage) {
            paramString = checkPathMatch(uri, resource);
        }
        
        if (paramString != null) {
        	Locale locale = 
                new org.springframework.web.servlet.support.RequestContext(request).getLocale();
            populateDescriptor(descriptor, locale, paramString);
        }
        
        // Checking if there is a reason to parse content
        if (this.parseableContentPropDef != null && descriptor.parse) {
            if (resource == null) {
                descriptor.parse = false;
            }
            if (resource != null && resource.getProperty(this.parseableContentPropDef) == null) {
                descriptor.parse = false;
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Resolved request " + request.getRequestURI() 
                    + " to decorating descriptor " + descriptor);
        }
        return descriptor;
    }

    private String checkErrorCodeMatch(int status) {
        String value = this.decorationConfiguration.getProperty("error[" + status + "]");
        if (value == null) {
            value = this.decorationConfiguration.getProperty("error");
        }
        return value;
    }
    
    private String checkRegexpMatch(String uri) {
        Enumeration<?> keys = this.decorationConfiguration.propertyNames();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            if (!key.startsWith("regexp[")) {
                continue;
            }

            RegexpCacheItem cached = this.regexpCache.get(key);
            if (cached == null || !cached.string.equals(key)) {
                cached = new RegexpCacheItem();
                cached.string = key;
                cached.compiled = parseRegexpParam(key);
                synchronized(this.regexpCache) {
                    this.regexpCache.put(key, cached);
                }
            }
            if (cached.compiled == null) {
                continue;
            }
            
            Matcher m = cached.compiled.matcher(uri);
            if (m.find()) {
                return this.decorationConfiguration.getProperty(key);
            } 
        }
        return null;
    }

    private String checkPathMatch(Path uri, Resource resource) throws Exception {
        if (this.config == null) {
            return null;
        }
        List<Path> list = new ArrayList<Path>(uri.getPaths());
        Collections.reverse(list);

        // TODO: Algorithm: sort entries into "score groups".
        // If several entries exist in the top group, 
        // first look for one that has "exact = true". 
        // Otherwise, just pick first one in top group.
        for (Path path: list) {
            List<ConfigEntry> entries = this.config.get(path);
            Map<ConfigEntry, Integer> score = new HashMap<ConfigEntry, Integer>();
            
            if (entries != null) {
                for (ConfigEntry entry: entries) {
                    if (entry.isExact()) {
                        if (!uri.equals(path)) {
                            score.put(entry, -1);
                            break;
                        } 
                        if (score.get(entry) == null) {
                            score.put(entry, 0);
                        }
                        // XXX: boost score for exact entries by 10 (temporarily).
                        // See TODO above.
                        score.put(entry, score.get(entry) + 10);
                    }
                    List<Predicate> predicates = entry.getPredicates();
                    if (score.get(entry) == null) {
                        score.put(entry, 0);
                    }
                    for (Predicate predicate: predicates) {
                        if (!matchPredicate(predicate, resource)) {
                            score.put(entry, -1);
                            break;
                        } else {
                            if (!score.containsKey(entry)) {
                                score.put(entry, 0);
                            }
                            score.put(entry, score.get(entry) + 1);
                        }
                    }
                }
                int highest = 0;
                ConfigEntry topEntry = null;
                for (ConfigEntry entry: score.keySet()) {
                    if (score.get(entry) >= highest) {
                        highest = score.get(entry);
                        topEntry = entry;
                    }
                }
                if (topEntry != null) {
                    return topEntry.getValue();
                }
            }
        }
        return null;
    }
    

    private boolean matchPredicate(Predicate predicate, Resource resource) throws Exception {
        if ("type".equals(predicate.getName())) {
            String token = SecurityContext.getSecurityContext().getToken();
            
            TypeInfo typeInfo = this.repository.getTypeInfo(token, resource.getURI());
            PrimaryResourceTypeDefinition type = typeInfo.getResourceType();
            while (type != null) {
                if (type.getName().equals(predicate.getValue())) {
                    return true;
                }
                type = type.getParentTypeDefinition();
            }
            return false;
        } else if ("service".equals(predicate.getName())) {

            Service currentService = RequestContext.getRequestContext().getService();
            while (currentService != null) {
                Object attr = currentService.getAttribute("decorating.servicePredicateName");
                if (attr != null && attr instanceof String) {
                    String servicePredicate = (String) attr;
                    if (servicePredicate.equals(predicate.getValue())) {
                        return true;
                    }
                    return false;
                }
                currentService = currentService.getParent();
            }
        }
        return false;
    }

    
    // Example: regexp[/foo/bar/.*\.html]
    private Pattern parseRegexpParam(String s) {
        try {
            String regexp = s.substring("regexp[".length(), s.length() - 1);
            return Pattern.compile(regexp, Pattern.DOTALL);
        } catch (Throwable t) {
            return null;
        }
    }
    
    private void populateDescriptor(InternalDescriptor descriptor, Locale locale, 
                                    String paramString) throws Exception {
        String[] params = paramString.split(",");
        for (String param : params) {
            param = param.trim();
            if ("NONE".equals(param)) {
                descriptor.tidy = false;
                descriptor.parse = false;
                descriptor.templates.clear();
                break;
            } else if ("TIDY".equals(param)) {
                descriptor.tidy = true;
            } else if ("NOPARSING".equals(param)) {
                descriptor.parse = false;
            } else {
                Template t = resolveTemplateReferences(locale, param);

                if (t != null) {
                    if (!this.supportMultipleTemplates) {
                        descriptor.templates.clear();
                    }
                    descriptor.templates.add(t);
                }
            }
        }
        
    }
    

    private class InternalDescriptor implements DecorationDescriptor {
        private boolean tidy = false;
        private boolean parse = true;
        private List<Template> templates = new ArrayList<Template>();
        
        public boolean decorate() {
            return !this.templates.isEmpty() || this.tidy || this.parse;
        }
        
        public boolean tidy() {
            return this.tidy;
        }
        public boolean parse() {
            return this.parse;
        }
        public List<Template> getTemplates() {
            return this.templates;
        }
        
        public String toString() {
            StringBuilder sb = new StringBuilder(this.getClass().getName());
            sb.append(" [templates=").append(this.templates.toString());
            sb.append(", parse=").append(this.parse);
            sb.append(", tidy=").append(this.tidy).append("]");
            return sb.toString();
        }
    }


    private Template resolveTemplateReferences(Locale locale, String mapping)
        throws Exception {
        
        String[] localizedRefs = buildLocalizedReferences(mapping, locale);
        for (int j = 0; j < localizedRefs.length; j++) {
            String localizedRef = localizedRefs[j];
            Template t = this.templateManager.getTemplate(localizedRef);
            if (t != null) {
                return t;
            }
        }
        return null;
    }
    

    private String[] buildLocalizedReferences(String ref, Locale locale) {
        String base = ref;
        String extension = "";
        int baseIdx = ref.lastIndexOf(".");
        int slashIdx = ref.lastIndexOf("/");
        if (baseIdx != -1 && slashIdx < baseIdx) {
            base = ref.substring(0, baseIdx);
            extension = ref.substring(baseIdx);
        }

        String language = locale.getLanguage();
        String country = locale.getCountry();
        String variant = locale.getVariant();
        
        List<String> references = new ArrayList<String>();
        if (!"".equals(country) && !"".equals(variant)) {
            references.add(base + "_" + language + "_" + country + "_" + variant + extension);
        }
        if (!"".equals(country)) {
            references.add(base + "_" + language + "_" + country + "_" + extension);
        }
        references.add(base + "_" + language + extension);
        references.add(base + extension);
        if (logger.isDebugEnabled()) {
            logger.debug("Attempting to resolve template ref '" + ref + "' using "
                         + "sequence " + references);
        }
        return references.toArray(new String[references.size()]);
    }
}
