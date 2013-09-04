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
package org.vortikal.web.decorating.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.web.decorating.DecoratorComponent;

/**
 * 
 * <p>
 * Configurable JavaBean properties
 * 
 * <ul>
 * <li><code>description</code> - by default delegates to sub class, optionally
 * overridden by user. Generic sub classes may not set this, in which case a
 * description is required by the user.</li>
 * </ul>
 */
public abstract class AbstractDecoratorComponent implements DecoratorComponent, InitializingBean {

    private String namespace;

    private String name;

    private String description;

    private Collection<UsageExample> usageExamples;

    private Map<String, String> parameterDescriptions;

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public String getNamespace() {
        return this.namespace;
    }

    @Required
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public final String getDescription() {
        if (this.description == null) {
            return getDescriptionInternal();
        }

        return this.description;
    }

    public void setParameterDescriptions(Map<String, String> parameterDescriptions) {
        this.parameterDescriptions = parameterDescriptions;
    }

    @Override
    public final Map<String, String> getParameterDescriptions() {
        return this.parameterDescriptions;
    }

    public void setExamples(Map<String, String> examples) {
        if (examples == null) {
            return;
        }
        List<UsageExample> result = new ArrayList<UsageExample>();
        for (String key : examples.keySet()) {
            String value = examples.get(key);
            result.add(new UsageExample(value, key));
        }
        this.usageExamples = Collections.unmodifiableCollection(result);
    }

    @Override
    public Collection<UsageExample> getUsageExamples() {
        return this.usageExamples;
    }

    protected abstract String getDescriptionInternal();

    protected abstract Map<String, String> getParameterDescriptionsInternal();

    @Override
    public void afterPropertiesSet() throws Exception {
        if (this.description == null)
            this.description = getDescriptionInternal();

        if (this.parameterDescriptions == null)
            this.parameterDescriptions = getParameterDescriptionsInternal();

        if (this.parameterDescriptions != null)
            this.parameterDescriptions = Collections.unmodifiableSortedMap(new TreeMap<String, String>(
                    this.parameterDescriptions));

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(this.getClass().getName());
        sb.append(": ").append(this.namespace).append(":").append(this.name);
        return sb.toString();
    }

}
