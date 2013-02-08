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
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.util.text.PathMappingConfig;
import org.vortikal.util.text.PathMappingConfig.ConfigEntry;
import org.vortikal.util.text.PathMappingConfig.Qualifier;
import org.vortikal.util.text.SimpleTemplate;
import org.vortikal.util.text.TextUtils;


public class TitleResolver implements ApplicationListener<ContextRefreshedEvent> {
    private static Log logger = LogFactory.getLog(TitleResolver.class);
    private Repository repository;
    private Path configPath;
    private PathMappingConfig config;
    private Map<String, SimpleTemplate> cache = new ConcurrentHashMap<String, SimpleTemplate>();
    
    public String resolve(final Resource resource) {
        ConfigEntry entry = matchConfigForResource(resource);
        if (entry == null) return resource.getTitle();
        
        String rawTemplate = entry.getValue();
        SimpleTemplate compiledTemplate = cache.get(rawTemplate);
        if (compiledTemplate == null) {
            compiledTemplate = SimpleTemplate.compile(rawTemplate);
            cache.put(rawTemplate, compiledTemplate);
        }
        
        final StringBuilder result = new StringBuilder();
        compiledTemplate.apply(new SimpleTemplate.Handler() {
            @Override
            public void write(String text) {
                result.append(text);
            }

            @Override
            public String resolve(String variable) {
                Property prop = getPropertyByReference(resource, variable);
                if (prop == null) {
                    return resource.getTitle();
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
    
    private ConfigEntry matchConfigForResource(final Resource resource) {
        
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
        
        return matchPropertyQualifier(q, r);
    }
    
    private boolean matchPropertyQualifier(Qualifier q, Resource r) {
        String ref = q.getName();
        Property prop = getPropertyByReference(r, ref);
        // Asterisk as value means "exists" testing on property
        if ("*".equals(q.getValue())) {
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
            if (q.getValue().equals(v.getNativeStringRepresentation())) {
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        loadConfig();
    }
    
    @Required
    public void setRepository(Repository repository) {
        this.repository = repository;
    }
    
    @Required
    public void setConfigPath(String configPath) {
        this.configPath = Path.fromString(configPath);
    }
    
    public void loadConfig() {
        try {
            InputStream inputStream = this.repository.getInputStream(null, this.configPath, true);
            this.config = new PathMappingConfig(inputStream);
        } catch(Throwable t) {
            logger.warn("Unable to load title configuration file: " 
                    + this.configPath + ": " + t.getMessage(), t);
        }
    }

}
