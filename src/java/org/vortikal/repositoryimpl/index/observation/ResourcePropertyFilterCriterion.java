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
package org.vortikal.repositoryimpl.index.observation;

import java.io.IOException;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.RepositoryException;
import org.vortikal.repository.Resource;

/**
 * Filter criterion that uses a property for determing if a resource should be
 * filtered out of index or not.
 * 
 * @author oyviste
 */
public class ResourcePropertyFilterCriterion implements FilterCriterion, 
    InitializingBean {
    
    // XXX: Initializing with "" value!
    private Repository repository;
    private String token = "";
    private Namespace propertyNamespace = Namespace.DEFAULT_NAMESPACE;
    private String propertyName = "";
    private String requiredValue = "";
    
    public void afterPropertiesSet() throws BeanInitializationException {
        if (repository == null) {
            throw new BeanInitializationException("Property 'repository' not set.");
        }
    }
    
    /** Creates a new instance of ResourcePropertyFilterCriteria */
    public ResourcePropertyFilterCriterion() {
    }
    
    public boolean isFiltered(String uri) {
        try {
            Resource resource = repository.retrieve(token, uri, false);
            Property prop = resource.getProperty(this.propertyNamespace, this.propertyName);
            if (prop != null && !prop.getValue().equals(requiredValue)) {
                return true;
            } 
        } catch (IOException io) {}
        catch (RepositoryException repex) {}
     
       return false;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public void setPropertyNamespace(Namespace propertyNamespace) {
        this.propertyNamespace = propertyNamespace;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setRequiredValue(String requiredValue) {
        this.requiredValue = requiredValue;
    }

    public void setToken(String token) {
        this.token = token;
    }
    
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("ResourceProperty["); buffer.append(this.propertyNamespace);
        buffer.append(":"); buffer.append(this.propertyName);
        buffer.append(" = "); buffer.append(this.requiredValue);
        buffer.append("]");

        return buffer.toString();
        
    }
}
