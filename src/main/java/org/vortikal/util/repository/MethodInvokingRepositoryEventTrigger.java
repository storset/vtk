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
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
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

    private LinkedHashMap<Object, String> multipleInvocations;
    private LinkedHashMap<Object, Method> methodInvocations;
    
    @Required
    public void setRepository(Repository repository)  {
        this.repository = repository;
    }
    
    public void setTargetObject(Object targetObject) {
        this.targetObject = targetObject;
    }
    
    /**
     * Only methods which take no arguments are supported for resolving ! 
     * @param method
     */
    public void setMethod(String method) {
        this.method = method;
    }

    public void setMultipleInvocations(LinkedHashMap<Object, String> multipleInvocations) {
        this.multipleInvocations = multipleInvocations;
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
        if (this.multipleInvocations != null && this.targetObject != null) {
            throw new BeanInitializationException("Specify only one of properties 'targetMethod' or 'multipleInvocations'");
        }
        if (this.multipleInvocations == null && (this.targetObject == null || this.method == null)) {
            throw new BeanInitializationException("Specify one of properties 'targetMethod' or 'multipleInvocations'");
        }
        this.methodInvocations = new LinkedHashMap<Object, Method>();
        if (this.multipleInvocations == null) {
            initMethodInvocation(this.targetObject, this.method);
        } else {
            for (Object target : this.multipleInvocations.keySet()) {
                String methodName = this.multipleInvocations.get(target);
                initMethodInvocation(target, methodName);
            }
        }
    }
    
    private void initMethodInvocation(Object target, String methodName) {
        // Resolving only methods that take no arguments.
        Method method = 
            BeanUtils.findMethod(target.getClass(), 
                    methodName, new Class[0]);
        
        if (method == null) {
            throw new BeanInitializationException("Unable to resolve method with name '"
                    + methodName + "' for class " + target.getClass() 
                    + ". Only methods that take no arguments are supported.");
        }
        this.methodInvocations.put(target, method);
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
        for (Object target : this.methodInvocations.keySet()) {
            Method method = this.methodInvocations.get(target);
            try {
                if (logger.isDebugEnabled()) {
                    logger.debug("Invoking method " + method + " on object "
                            + this.targetObject);
                }
                method.invoke(target, new Object[0]);
            } catch (Throwable t) {
                logger.warn("Error occurred while invoking method '" + method +
                        "' on object '" + target+ "'", t);
            }
        }
    }
    

}

