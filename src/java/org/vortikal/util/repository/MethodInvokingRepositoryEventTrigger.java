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
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.vortikal.repository.Repository;
import org.vortikal.repository.event.RepositoryEvent;



/**
 * 
 */
public class MethodInvokingRepositoryEventTrigger 
  implements ApplicationListener, InitializingBean {

    private Log logger = LogFactory.getLog(this.getClass());

    private Repository repository;
    private String uri;
    private Pattern uriPattern;
    private Object targetObject;
    private String method;
    private Method targetMethod;

    public void setRepository(Repository repository)  {
        this.repository = repository;
    }
    
    public void setTargetObject(Object targetObject) {
        this.targetObject = targetObject;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
    
    public void setUriPattern(String uriPattern) {
        this.uriPattern = Pattern.compile(uriPattern);
    }
    

    public void afterPropertiesSet() {
        if (this.repository == null) {
            throw new BeanInitializationException(
                "JavaBean property 'repository' not set.");
        }
        if (this.method == null) {
            throw new BeanInitializationException(
                "JavaBean property 'method' not set.");
        }
        if (this.targetObject == null) {
            throw new BeanInitializationException(
                "JavaBean property 'targetObject' not set.");
        }
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
        
        String resourceURI = ((RepositoryEvent) event).getURI();
        if (this.uri != null) {
            if (!this.uri.equals(resourceURI)) {
                return;
            }
        }

        if (this.uriPattern != null) {
            Matcher matcher = this.uriPattern.matcher(resourceURI);
            if (!matcher.matches()) {
                return;
            }
        }

        try {
            this.logger.info("Invoking method on ooobject:");

            this.targetMethod.invoke(this.targetObject, new Object[0]);
        } catch (Throwable t) {
            this.logger.warn("Error occurred while invoking method '" + this.method +
                        "' on object '" + this.targetObject + "'", t);
        }

    }

}

