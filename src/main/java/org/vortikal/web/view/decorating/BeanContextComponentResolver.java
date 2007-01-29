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
package org.vortikal.web.view.decorating;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;


public class BeanContextComponentResolver
  implements ComponentResolver, ApplicationContextAware, InitializingBean {

    private static Log logger = LogFactory.getLog(BeanContextComponentResolver.class);
    
    private boolean initialized;
    private ApplicationContext applicationContext;
    private Map components = new HashMap();
    
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    
    public void afterPropertiesSet() {
        if (this.applicationContext == null) {
            throw new BeanInitializationException(
                "JavaBean property 'applicationContext' not specified");
        }
    }
    
    public DecoratorComponent resolveComponent(String namespace, String name) {
        if (!this.initialized) {
            init();
        }

        DecoratorComponent component = (DecoratorComponent)
            this.components.get(namespace + ":" + name);
        if (logger.isDebugEnabled()) {
            logger.debug("Resolved namespace: '" + namespace + "', name: '" + name
                         + "' to component:  " + component);
        }
        return component;
    }
    
    
    private synchronized void init() {
        Collection beans = 
            BeanFactoryUtils.beansOfTypeIncludingAncestors(
                this.applicationContext, 
                DecoratorComponent.class, false, false).values();        
        
        for (Iterator i = beans.iterator(); i.hasNext();) {
            DecoratorComponent component = (DecoratorComponent) i.next();
            String ns = component.getNamespace();
            String name = component.getName();
            if (ns == null) {
                throw new IllegalStateException("Component " + component
                                                + " has invalid namespace (NULL)");
            }
            if (name == null) {
                throw new IllegalStateException("Component " + component
                                                + " has invalid name (NULL)");
            }
            String key = ns + ":" + name;
            logger.info("Registering decorator component " + component);
            this.components.put(key, component);
        }
        this.initialized = true;
    }
}

