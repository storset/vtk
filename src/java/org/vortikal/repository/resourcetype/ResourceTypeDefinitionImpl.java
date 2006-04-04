/* Copyright (c) 2006, University of Oslo, Norway
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
package org.vortikal.repository.resourcetype;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.Namespace;
import org.vortikal.web.service.RepositoryAssertion;

public class ResourceTypeDefinitionImpl implements ResourceTypeDefinition, 
                                        InitializingBean {

    private String name;
    private Namespace namespace;
    private ResourceTypeDefinition parentTypeDefinition;
    private ResourceTypeDefinition[] mixinTypeDefinitions;
    private PropertyTypeDefinition[] propertyTypeDefinitions = new PropertyTypeDefinitionImpl[0];
    private RepositoryAssertion[] assertions;
    
    public void afterPropertiesSet() throws BeanInitializationException {
        if (name == null) {
            throw new BeanInitializationException("Property 'name' not set.");
        } else if (namespace == null) {
            throw new BeanInitializationException("Property 'namespace' not set.");
        }
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Namespace getNamespace() {
        return this.namespace;
    }

    public void setNamespace(Namespace namespace) {
        this.namespace = namespace;
    }

    public ResourceTypeDefinition getParentTypeDefinition() {
        return this.parentTypeDefinition;
    }

    public void setParentTypeDefinition(ResourceTypeDefinition parentTypeDefinition) {
        this.parentTypeDefinition = parentTypeDefinition;
    }

    public ResourceTypeDefinition[] getMixinTypeDefinitions() {
        return this.mixinTypeDefinitions;
    }

    public void setMixinTypeDefinitions(ResourceTypeDefinition[] mixinTypeDefinitions) {
        this.mixinTypeDefinitions = mixinTypeDefinitions;
    }

    public PropertyTypeDefinition[] getPropertyTypeDefinitions() {
        return this.propertyTypeDefinitions;
    }

    public void setPropertyTypeDefinitions(PropertyTypeDefinition[] propertyTypeDefinitions) {
        this.propertyTypeDefinitions = propertyTypeDefinitions;
    }

    public RepositoryAssertion[] getAssertions() {
        return this.assertions;
    }

    public void setAssertions(RepositoryAssertion[] assertions) {
        this.assertions = assertions;
    }

}
