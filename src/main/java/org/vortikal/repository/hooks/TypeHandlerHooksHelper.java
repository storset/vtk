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

package org.vortikal.repository.hooks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.vortikal.repository.RepositoryImpl;
import org.vortikal.repository.ResourceImpl;

/**
 * Assists repository with management, lookup and registration of 
 * {@link TypeHandlerHooks} beans.
 */
public class TypeHandlerHooksHelper implements ApplicationContextAware, InitializingBean {
    
    private List<TypeHandlerHooks> typeHandlerHooks = Collections.emptyList();
    private ApplicationContext context;
    
    private final Log logger = LogFactory.getLog(RepositoryImpl.class.getName() 
            + ".TypeHandlerHooks");
    
    /**
     * Get registered type handler for resource.
     * 
     * @param r the resource
     * @return a registered type handler or <code>null</code> if no such handler
     * exists.
     */
    public TypeHandlerHooks getTypeHandlerHooks(ResourceImpl r) {
        for (TypeHandlerHooks hooks : this.typeHandlerHooks) {
            if (r.getResourceType().equals(hooks.getApplicableResourceType())) {
                return hooks;
            }
        }

        return null;
    }

    /**
     * Get registered type handler for a content type.
     * 
     * @param contentType the fully qualified content type (media type) specification.
     * @return a registered type handler or <code>null</code> if no such handler
     * exists.
     */
    public TypeHandlerHooks getTypeHandlerHooks(String contentType) {
        for (TypeHandlerHooks hooks : this.typeHandlerHooks) {
            String applicableContent = hooks.getApplicableContent();
            if (contentType.startsWith(applicableContent) 
                    || contentType.equals(applicableContent))
                return hooks;
        }

        return null;
    }

    @Override
    public void afterPropertiesSet() {
        Map<String, TypeHandlerHooks> typeHandlerBeans
                = this.context.getBeansOfType(TypeHandlerHooks.class);

        List<TypeHandlerHooks> hooks = new ArrayList<TypeHandlerHooks>(typeHandlerBeans.values());
        validateTypeHandlerHooks(hooks);
        for (TypeHandlerHooks hook : hooks) {
            logger.info("Registered TypeHandlerHooks extension: " + hook
                    + ", content type pattern = " + hook.getApplicableContent()
                    + ", resource type = " + hook.getApplicableResourceType());
        }

        this.typeHandlerHooks = hooks;
    }

    private void validateTypeHandlerHooks(List<TypeHandlerHooks> hooks) {
        final Set<String> contentSpecs = new HashSet<String>();
        final Set<String> typeSpecs = new HashSet<String>();
    
        // Validate type handler configuration 
        for (TypeHandlerHooks h: hooks) {
            String applicableContent = h.getApplicableContent();
            if (applicableContent == null 
                    || applicableContent.isEmpty()
                    || applicableContent.startsWith("/") 
                    || applicableContent.equals("/")
                    || applicableContent.matches("/{2,}")) {
                throw new IllegalArgumentException(
                        "Invalid content type specification from type handler: " + h);
            }
            
            String contentGroup;
            String contentSubType = null;
            if (applicableContent.contains("/") && !applicableContent.endsWith("/")) {
                contentGroup = applicableContent.substring(0, applicableContent.indexOf("/"));
                contentSubType = applicableContent.substring(applicableContent.indexOf("/")+1, 
                        applicableContent.length());
            } else {
                if (applicableContent.endsWith("/")) {
                    contentGroup = applicableContent.substring(0, applicableContent.length()-1);
                } else {
                    contentGroup = applicableContent;
                }
            }
            
            if (contentSubType != null) {
                if (!contentSpecs.add(contentGroup + "/" + contentSubType)
                        || contentSpecs.contains(contentGroup)) {
                    throw new IllegalArgumentException("Overlapping type applicability in configured type handlers");
                }
            } else {
                if (!contentSpecs.add(contentGroup)) {
                    throw new IllegalArgumentException("Overlapping type applicability in configured type handlers");
                }
            }
            
            String resourceType = h.getApplicableResourceType();
            if (resourceType == null
                    || resourceType.isEmpty()
                    || "resource".equals(resourceType)
                    || "file".equals(resourceType)
                    || "collection".equals(resourceType)) {
                throw new IllegalArgumentException("Invalid or illegal resource type specification in type handler: " + h);
            }
            
            if (!typeSpecs.add(resourceType)) {
                throw new IllegalArgumentException("Overlapping type applicability in configured type handlers");
            }
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext ac) throws BeansException {
        this.context = ac;
    }
    
}
