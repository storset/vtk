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

import java.util.Map;
import java.util.Properties;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;

import org.vortikal.util.web.URLUtil;
import org.vortikal.web.RequestContext;


public class PropertyConfigurableTemplateResolver
  implements TemplateResolver, InitializingBean {

    private static Log logger = LogFactory.getLog(PropertyConfigurableTemplateResolver.class);

    private TemplateManager templateManager;
    private Properties templateConfiguration;
    

    public void setTemplateManager(TemplateManager templateManager) {
        this.templateManager = templateManager;
    }
    
    
    public void setTemplateConfiguration(Properties templateConfiguration) {
        this.templateConfiguration = templateConfiguration;
    }


    public void afterPropertiesSet() {
        if (this.templateManager == null) {
            throw new BeanInitializationException(
                "JavaBean property 'templateManager' not set");
        }

        if (this.templateConfiguration == null) {
            throw new BeanInitializationException(
                "JavaBean property 'templateConfiguration' not set");
        }
    }
    
    public Template resolveTemplate(Map model, HttpServletRequest request,
                                    HttpServletResponse response) throws Exception {

        RequestContext requestContext = RequestContext.getRequestContext();
        if (requestContext == null) {
            return null;
        }
        Template template = null;
        String uri = requestContext.getResourceURI();
        String[] path = URLUtil.splitUriIncrementally(uri);
        for (int i = path.length - 1; i >= 0; i--) {
            String prefix = path[i];
            String mapping = this.templateConfiguration.getProperty(prefix);
            if ("NONE".equals(mapping)) {
                return null;
            }
            if (mapping != null) {
                template = this.templateManager.getTemplate(mapping);
                break;
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Resolved request '" + uri + "' to template " + template);
        }
        return template;
    }
    

}
