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
package org.vortikal.repositoryimpl;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;

import org.vortikal.repositoryimpl.dao.DataAccessor;


public class EvaluatorUtil implements InitializingBean {

    private PropertyManagerImpl propertyManager;
    private DataAccessor dataAccessor;
    private Log logger = LogFactory.getLog(this.getClass());

    public void setPropertyManager(PropertyManagerImpl propertyManager) {
        this.propertyManager = propertyManager;
    }

    public void setDataAccessor(DataAccessor dataAccessor) {
        this.dataAccessor = dataAccessor;
    }
    

    public void afterPropertiesSet() {
        if (this.propertyManager == null) {
            throw new BeanInitializationException(
                "JavaBean property 'propertyManager' not specified");
        }
        if (this.dataAccessor == null) {
            throw new BeanInitializationException(
                "JavaBean property 'dataAccessor' not specified");
        }
    }
    
    public void evaluate(String uri) throws IOException, CloneNotSupportedException {
        ResourceImpl resource = this.dataAccessor.load(uri);
        if (resource == null) {
            throw new IOException("Resource " + uri + " does not exist");
        }
        logger.info("Evaluating: " + resource.getURI());

        resource = 
            this.propertyManager.storeProperties(
                resource, resource.getOwner(), resource);

        if (!resource.isCollection()) {
            resource = this.propertyManager.fileContentModification(
                resource, resource.getOwner());
        } else {
            resource = this.propertyManager.collectionContentModification(
                resource, resource.getOwner());
        }


        this.dataAccessor.store(resource);
        
        if (resource.isCollection()) {
            ResourceImpl[] children = this.dataAccessor.loadChildren(resource);
            for (int i = 0; i < children.length; i++) {
                evaluate(children[i].getURI());
            }
        }
    }
    
}
