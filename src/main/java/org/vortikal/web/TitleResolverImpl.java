/* Copyright (c) 2013, University of Oslo, Norway
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
package org.vortikal.web;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.TypeInfo;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.util.repository.LocaleHelper;
import org.vortikal.util.text.PathMappingConfig;
import org.vortikal.util.text.PathMappingConfig.ConfigEntry;
import org.vortikal.util.text.PathMappingConfig.Qualifier;
import org.vortikal.util.text.SimpleTemplate;
import org.vortikal.util.text.TextUtils;

/**
 * Resolve resource web titles based on configuration.
 * 
 * <p>{@literal General form for a config <rule>:}<br>
 * 
 * {@literal <path> <predicates> = <template value> "\n" }
 * <br>
 * where:<br>
 * {@literal <predicates> is on the form "[P1,P2,P3..]" }
 * <br>and {@literal <path>} is some resource path, optionally ending with slash for exact match.
 *
 * <p>One line per &lt;rule&gt;. One config file to rule them all. For more info on
 * syntax, see {@link PathMappingConfig}.
 * 
 * <p>Matching:
 * <ol>
 *   <li>First, the path specified must be either equal to or ancestor of resource
 * to match. Deeper paths in config override rules in ancestor paths.
 *   <li>If a path match is found, then predicates of all rules applying are matched
 *   against resource. If multiple rules match, the last rule is chosen, and its template
 *   value used to generate title.
 * </ol>
 * 
 * <p>Example configuration:
 * <pre>
 * # Some config
 * / = ${title} - ${hostname}
 * 
 * /foo/bar = ${title} - Foobar area - ${hostname}
 * /foo/bar[lang:en] = ${title} - Foobar area in English - ${hostname}
 * /foo/bar[type:structured-event] = ${title} - Happenings under Foobar area - ${hostname}
 * /foo/bar[type:structured-event,lang:en] = ${title} - English happenings under Foobar area - ${hostname}
 * 
 * # Require that property "prop" in namespace "some" exists (has some value) for resource:
 * /a/b[some\:prop:*] = ${title} - a resource that has prop some:prop - ${hostname}
 * 
 * # A rule that matches ONLY for "/exactly", but <em>not</em> any descendants:
 * /exactly/ = The Exactly resource at /exactly, created at ${creationTime}
 * 
 * # A rule that will match <em>only</em> root resource:
 * // = Welcome to ${hostname}
 * 
 * # This will match any resource that inherits some inheritable property from type "some-context-type":
 * /[context:some-context-type] = ${title} - Some Context ${some-ctx: - ${hostname}
 * </pre>
 * 
 * <p>Available matching predicates:
 * <ul>
 *  <li><b>lang</b> - match on locale of resource.<br>
 *   Example: "/[lang:en] = ..."
 *  <li><b>type</b> - match on <em>exact</em> resource type<br>
 *   Example: "/[type:collection] = ...".
 *  <li><b>context</b> - match on resources that inherit properties from the given type<br>
 *   Example: "/[context:foo-context] = ..."
 *  <li><b>NAMESPACE\:NAME</b> - match on value of property NAME in namespace NAMESPACE<br>
 *   Example: "/[visual-profile\:disabled:true] = ...".
 *   Note the required escaping of ":" between namespace and name, since the colon is also used to separate property from
 *   required value.
 * </ul>
 * 
 * <p>Available template variables:
 * <ul>
 *   <li><b>${NAMESPACE:NAME}</b> - Value of resource property <b>NAME</b>, in namespace <b>NAMESPACE</b>.
 *   <li><b>${NAME}</b> - value of resource property <b>NAME</b> in the default namespace.
 *   <li><b>${hostname}</b> - the name of the host
 * </ul>
 * 
 */
public class TitleResolverImpl implements ApplicationListener<ContextRefreshedEvent>, TitleResolver {

    private final Log logger = LogFactory.getLog(TitleResolverImpl.class);
    private Repository repository;
    private ResourceTypeTree resourceTypeTree;
    private Path configPath;
    private volatile PathMappingConfig config;
    private Map<String, SimpleTemplate> templateCache = new ConcurrentHashMap<String, SimpleTemplate>();
    private SimpleTemplate fallbackTemplate = SimpleTemplate.compile("${title} - ${hostname}");
    
    @Override
     public String resolve(final Resource resource) {
        if (config == null) {
            // Be graceful if config is not loaded
            logger.warn("Configuration not loaded or failed to load, using fallback title for " + resource.getURI());
            return renderTemplate(fallbackTemplate, resource);
        }
        
        ConfigEntry entry = matchConfigForResource(resource);
        if (entry == null) return renderTemplate(fallbackTemplate, resource);
        
        String rawTemplate = entry.getValue();
        SimpleTemplate compiledTemplate = templateCache.get(rawTemplate);
        if (compiledTemplate == null) {
            compiledTemplate = SimpleTemplate.compile(rawTemplate);
            templateCache.put(rawTemplate, compiledTemplate);
        }
        
        return renderTemplate(compiledTemplate, resource);
        
    }
     
    private String renderTemplate(SimpleTemplate template, final Resource resource) {
        final StringBuilder result = new StringBuilder();
        template.apply(new SimpleTemplate.Handler() {
            @Override
            public void write(String text) {
                result.append(text);
            }

            @Override
            public String resolve(String var) {
                if ("hostname".equals(var)) {
                    return repository.getId();
                }
                
                Property prop = getPropertyByReference(resource, var);
                if (prop == null) {
                    return "";
                }
                return prop.getFormattedValue();
            }
        });
        return result.toString();
    }

    // XXX We're doing exactly the same thing in many places for
    // handling "prefix:name" prop references.. perhaps utility candidate
    private Property getPropertyByReference(Resource resource, String ref) {
        int separatorIdx = ref.indexOf(':');
        if (separatorIdx > 0 && separatorIdx < ref.length()-1) {
            String prefix = ref.substring(0, separatorIdx);
            String suffix = ref.substring(separatorIdx+1);

            Property prop = resource.getPropertyByPrefix(prefix, suffix);
            return prop;
        }
        Property prop = resource.getProperty(Namespace.DEFAULT_NAMESPACE, ref);
        if (prop == null) {
            prop = resource.getProperty(Namespace.STRUCTURED_RESOURCE_NAMESPACE, ref);
        }
        return prop;
    }
    
    private ConfigEntry matchConfigForResource(Resource resource) {
        
        List<ConfigEntry> entries = this.config.getMatchAncestor(resource.getURI());
        if (entries != null && !entries.isEmpty()) {
            List<ConfigEntry> candidates = new ArrayList<ConfigEntry>();
            for (ConfigEntry entry: entries) {
                boolean match = true;
                for (Qualifier q: entry.getQualifiers()) {
                    if (!matchQualifier(q, resource)) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    candidates.add(entry);
                }
            }
            if (!candidates.isEmpty()) {
                return candidates.get(candidates.size()-1);
            }
        }
        
        return null;
    }
    
    private boolean matchQualifier(Qualifier q, Resource r) {
        if ("type".equals(q.getName())) {
            return r.getResourceType().equals(q.getValue());
        }
        if ("context".equals(q.getName())) {
            return matchContext(q.getValue(), r);
        }
        if ("lang".equals(q.getName())) {
            return matchLanguage(q.getValue(), r);
        }
        
        return matchPropertyValue(q.getName(),q.getValue(), r);
    }
    
    private boolean matchContext(String contextType, Resource r) {
        ResourceTypeDefinition typeDef = null;

        try {
            typeDef = resourceTypeTree.getResourceTypeDefinitionByName(contextType);
        } catch (IllegalArgumentException ie) {}

        if (typeDef == null) return false;
        
        for (PropertyTypeDefinition def: typeDef.getPropertyTypeDefinitions()) {
            if (def.isInheritable()) {
                if (r.getProperty(def) != null) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private boolean matchLanguage(String lang, Resource r) {
        Locale contentLocale = r.getContentLocale();
        if (contentLocale == null) {
            return false;
        }
        
        String resourceLang = LocaleHelper.getPreferredLang(contentLocale);
        return resourceLang.equalsIgnoreCase(lang);
    }
    
    private boolean matchPropertyValue(String propRef, String value, Resource r) {
        Property prop = getPropertyByReference(r, propRef);
        // Asterisk as value means "exists" testing on property
        if ("*".equals(value)) {
            return prop != null;
        }
        if (prop == null) {
            return false;
        }
        
        PropertyTypeDefinition def = prop.getDefinition();
        Value[] values;
        if (def.isMultiple()) {
           values = prop.getValues();
        } else {
            values = new Value[] {prop.getValue()};
        }

        for (Value v: values) {
            if (value.equals(v.getNativeStringRepresentation())) {
                return true;
            }
        }

        return false;
    }
    
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        loadConfig();
    }
    
    /**
     * (Re)load configuration data from resource at config path. Must be called
     * at least once before using instance.
     * 
     * @see #setConfigPath(java.lang.String) 
     */
    public void loadConfig() {
        try {
            InputStream inputStream = this.repository.getInputStream(null, this.configPath, true);
            this.config = new PathMappingConfig(inputStream);
            this.templateCache.clear();
        } catch(Throwable t) {
            logger.warn("Unable to load title configuration file: " 
                    + this.configPath + ": " + t.getMessage(), t);
        }
    }

    /**
     * Set fallback template to use when no configuration rules match a resource.
     * Default template value is "${title} - ${hostname}".
     * 
     * @param template The template to use as fallback, as a string.
     */
    public void setFallbackTemplate(String template) {
        this.fallbackTemplate = SimpleTemplate.compile(template);
    }
    
    /**
     * Set repository path to configuration resource.
     * @param configPath the path to the configuration resource.
     */
    @Required
    public void setConfigPath(String configPath) {
        this.configPath = Path.fromString(configPath);
    }

    @Required
    public void setResourceTypeTree(ResourceTypeTree rt) {
        this.resourceTypeTree = rt;
    }
    
    @Required
    public void setRepository(Repository repository) {
        this.repository = repository;
    }
}
