/* Copyright (c) 2007, University of Oslo, Norway
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
package org.vortikal.repository.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.ChangeLogEntry;
import org.vortikal.repository.Path;
import org.vortikal.repository.ChangeLogEntry.Operation;
import org.vortikal.repository.index.observation.ResourceChangeNotifier;
import org.vortikal.repository.index.observation.ResourceChangeObserver;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.store.ContextDAO;

public class ContextManagerImpl implements ContextManager, InitializingBean {

    private static Log logger = LogFactory.getLog(ContextManagerImpl.class);
    
    private ResourceChangeNotifier notifier;
    
    private ContextDAO contextDAO;
    private List<PropertyTypeDefinition> contextPropertyDefinitions;
    private PropertyTypeDefinition enabledProp;
    private PropertyTypeDefinition definesContextProp;

    private Map<PropertyTypeDefinition, Map<String, String>> contextMaps;
    private Map<String, String> enabledMap;
    private Map<String, String> definesMap;
    
    public Map<PropertyTypeDefinition, String> getContext(Path uri) {

        Map<PropertyTypeDefinition, String> context = new HashMap<PropertyTypeDefinition, String>();

        // Context not enabled or the resource itself already defines it
        if (!enabledContext(uri) || definesContext(uri)) {
            return null;
        }
        
        List<Path> uris = uri.getPaths();
        
        int i = uris.size() - 2 ;
        while (i > -1) {
            Path currentUri = uris.get(i);
            if (enabledContext(currentUri) && definesContext(currentUri)) {
                return extractContext(currentUri);
            }
            i--;
        }
        return context;
    }

    private Map<PropertyTypeDefinition, String> extractContext(Path uri) {
        Map<PropertyTypeDefinition, String> context = new HashMap<PropertyTypeDefinition, String>();

        for (PropertyTypeDefinition propDef : this.contextPropertyDefinitions) {
            Map<String, String> contextMap = this.contextMaps.get(propDef);

            String value = contextMap.get(uri.toString());
            if (value != null) {
                context.put(propDef, value);
            }
        }
        return context;
    }

    private boolean definesContext(Path uri) {
        return "true".equals(this.definesMap.get(uri.toString()));
    }
    
    private boolean enabledContext(Path uri) {
        return "true".equals(this.enabledMap.get(uri.toString()));
    }

    @Required
    public void setEnabledProp(PropertyTypeDefinition enabledProp) {
        this.enabledProp = enabledProp;
    }

    @Required
    public void setDefinesContextProp(PropertyTypeDefinition definesContextProp) {
        this.definesContextProp = definesContextProp;
    }
    
    @Required
    public void setContextDAO(ContextDAO contextDAO) {
        this.contextDAO = contextDAO;
    }

    @Required
    public void setContextPropertyDefinitions(
            List<PropertyTypeDefinition> contextPropertyDefinitions) {
        this.contextPropertyDefinitions = contextPropertyDefinitions;
    }
    
    private synchronized void populate() {
        long start = System.currentTimeMillis();
        Map<PropertyTypeDefinition, Map<String, String>> contextMaps = 
            new HashMap<PropertyTypeDefinition, Map<String, String>>();
        
        for (PropertyTypeDefinition propDef : this.contextPropertyDefinitions) {
            contextMaps.put(propDef, this.contextDAO.getContextMap(propDef));
        }
        this.contextMaps = contextMaps;

        this.definesMap = this.contextDAO.getContextMap(this.definesContextProp);
        this.enabledMap = this.contextDAO.getContextMap(this.enabledProp);

        logger.debug("Loading context took " + (System.currentTimeMillis() - start) + " ms.");
    }

    public void afterPropertiesSet() throws Exception {
        populate();
        if (this.notifier != null)
            this.notifier.registerObserver(new ChangeObserver());
    }

    public void setNotifier(ResourceChangeNotifier notifier) {
        this.notifier = notifier;
    }

    private class ChangeObserver implements ResourceChangeObserver { 
        private boolean enabled = true;

        public void disable() {
            enabled = false;

        }

        public void enable() {
            enabled = true;

        }

        public String getObserverId() {
            return this.getClass().getName();
        }

        public boolean isEnabled() {
            return this.enabled;
        }

        public void notifyResourceChanges(List<ChangeLogEntry> resourceChanges) {
            if (resourceChanges == null || resourceChanges.isEmpty()) {
                return;
            }

            boolean refresh = false;
            for (ChangeLogEntry entry : resourceChanges) {
                Operation o = entry.getOperation();
                if (o.equals(Operation.MODIFIED_PROPS) || 
                        o.equals(Operation.MODIFIED_CONTENT) ||
                        o.equals(Operation.DELETED) || 
                        o.equals(Operation.CREATED)) {
                    refresh = true;
                    break;
                }
            }
            

            if (refresh) {
                populate();
            }
        }
    }
    
}
