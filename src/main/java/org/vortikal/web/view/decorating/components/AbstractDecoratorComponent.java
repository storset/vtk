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
package org.vortikal.web.view.decorating.components;

import java.util.Map;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.web.view.decorating.DecoratorComponent;

/**
 * 
 * <p>
 * Configurable JavaBean properties
 * 
 * <ul>
 * <li><code>description</code> - by default delegates to sub class,
 * optionally overridden by user. Generic sub classes may not set this, in which case a
 * description is required by the user.</li>
 * </ul>
 */
public abstract class AbstractDecoratorComponent implements DecoratorComponent,
        InitializingBean {

    private String namespace;

    private String name;

    private String description;

    private Map<String, String> parameterDescriptions;

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getNamespace() {
        return this.namespace;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public final String getDescription() {
        return this.description;
    }

    protected abstract String getDescriptionInternal();

    public final Map<String, String> getParameterDescriptions() {
        return this.parameterDescriptions;
    }

    protected abstract Map<String, String> getParameterDescriptionsInternal();

    public void afterPropertiesSet() throws Exception {
        if (this.description == null)
            this.description = getDescriptionInternal();

        if (this.parameterDescriptions == null)
            this.parameterDescriptions = getParameterDescriptionsInternal();

        if (this.description == null) {
            throw new BeanInitializationException(
                    "JavaBean property 'description' not set");
        }

        if (this.namespace == null) {
            throw new BeanInitializationException(
                    "JavaBean property 'namespace' not set");
        }
        if (this.name == null) {
            throw new BeanInitializationException(
                    "JavaBean property 'name' not set");
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(this.getClass().getName());
        sb.append(": ").append(this.namespace).append(":").append(this.name);
        return sb.toString();
    }

}
