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
package org.vortikal.web.referencedata.provider;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.vortikal.web.referencedata.ReferenceDataProvider;

/**
 * A reference data provider that puts a spring {@link
 * ClassPathResource class path resource} in the model.
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>path</code> - the path to the resource
 *   <li><code>modelName</code> - the name to use for the sub-model in
 *   the main model. 
 * </ul>
 *
 * <p>Model data provided:
 * <ul>
 *   <li>The Spring resource (in a sub-model whose name is
 *   configurable trough the <code>modelName</code> JavaBean property)
 * </ul>
 * 
 */
public class ClassPathResourceProvider implements ReferenceDataProvider, InitializingBean {

    private Log logger = LogFactory.getLog(this.getClass());

    private String path;

    private String modelName;


    public void setPath(String path) {
        this.path = path;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public void afterPropertiesSet() {

        if (this.path == null) {
            throw new BeanInitializationException(
                "JavaBean property 'path' not set");
        }

        if (this.modelName == null) {
            throw new BeanInitializationException(
                "JavaBean property 'modelName' not set");
        }
    }
    


    public void referenceData(Map model, HttpServletRequest request)
            throws Exception {

        ClassPathResource resource = new ClassPathResource(this.path);
        Object result = processResource(resource);
        model.put(this.modelName, result);
    }
    

    protected Object processResource(ClassPathResource resource) throws Exception {
        return resource;
    }
    
    
    public String toString() {
        StringBuffer sb = new StringBuffer(this.getClass().getName());
        sb.append(" [ ");
        sb.append("path = ").append(this.path);
        sb.append(", modelName = ").append(this.modelName);
        sb.append(" ]");
        return sb.toString();
    }

}
