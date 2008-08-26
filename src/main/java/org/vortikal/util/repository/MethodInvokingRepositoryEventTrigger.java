/* Copyright (c) 2005, University of Oslo, Norway
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
package org.vortikal.util.repository;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.event.RepositoryEvent;
import org.vortikal.repository.event.ResourceCreationEvent;
import org.vortikal.repository.event.ResourceDeletionEvent;

/**
 * 
 */
public class MethodInvokingRepositoryEventTrigger 
  implements ApplicationListener, InitializingBean {

    private static Log logger = LogFactory.getLog(MethodInvokingRepositoryEventTrigger.class);

    private Repository repository;
    private Path uri;
    private Pattern uriPattern;
    private Object targetObject;
    private String method;
    private Method targetMethod;

    @Required
    public void setRepository(Repository repository)  {
        this.repository = repository;
    }
    
    @Required
    public void setTargetObject(Object targetObject) {
        this.targetObject = targetObject;
    }

    @Required
    public void setMethod(String method) {
        this.method = method;
    }

    public void setUri(String uri) {
        this.uri = Path.fromString(uri);
    }
    
    public void setUriPattern(String uriPattern) {
        this.uriPattern = Pattern.compile(uriPattern);
    }
    

    public void afterPropertiesSet() {
        if (this.uri == null && this.uriPattern == null) {
            throw new BeanInitializationException(
                "One of JavaBean properties 'uri' or 'uriPattern' must be specified.");
        }

        Method[] methods = this.targetObject.getClass().getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            if (this.method.equals(methods[i].getName())) {
                this.targetMethod = methods[i];
            }
        }
    }

    
    public void onApplicationEvent(ApplicationEvent event) {
        if (! (event instanceof RepositoryEvent)) {
            return;
        }
        Repository rep = ((RepositoryEvent) event).getRepository();
        if (! rep.getId().equals(this.repository.getId())) {
            return;
        }
        
        Path resourceURI = ((RepositoryEvent) event).getURI();

        if (this.uri != null) {
            if (((event instanceof ResourceDeletionEvent)
                 || (event instanceof ResourceCreationEvent))
                && (resourceURI.isAncestorOf(this.uri)
                    || this.uri.isAncestorOf(resourceURI))) {
                invoke();
            } else if (this.uri.equals(resourceURI)) {
                invoke();
            }
        } else if (this.uriPattern != null) {
            Matcher matcher = this.uriPattern.matcher(resourceURI.toString());
            if (matcher.find()) {
                invoke();
            }
        }
    }


    private void invoke() {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Invoking method " + this.targetMethod + " on object "
                             + this.targetObject);
            }
            this.targetMethod.invoke(this.targetObject, new Object[0]);
        } catch (Throwable t) {
            logger.warn("Error occurred while invoking method '" + this.method +
                             "' on object '" + this.targetObject + "'", t);
        }
    }
    

}

