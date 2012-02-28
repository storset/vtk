/* Copyright (c) 2012, University of Oslo, Norway
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

package org.vortikal.repository.systemjob;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.context.BaseContext;
import org.vortikal.repository.Repository;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.scheduling.AbstractTask;
import org.vortikal.security.SecurityContext;

/**
 * Sets up thread local security context, system change context and executes
 * repository job.
 */
public abstract class RepositoryJob extends AbstractTask implements InitializingBean {

    private SecurityContext securityContext;
    private Repository repository;

    private PropertyTypeDefinition systemJobStatusPropDef;
    private ResourceTypeTree resourceTypeTree;
    private List<String> affectedPropDefPointers;
    private List<PropertyTypeDefinition> affectedProperties;
    
    private final Log logger = LogFactory.getLog(getClass());
    
    @Override
    public void run() {

        try {
            BaseContext.pushContext();
            SecurityContext.setSecurityContext(this.securityContext);
            
            String time = SystemChangeContext.dateAsTimeString(Calendar.getInstance().getTime());
            SystemChangeContext systemChangeContext =
                    new SystemChangeContext(getId(), time,
                                            this.affectedProperties, this.systemJobStatusPropDef);
            
            executeWithRepository(this.repository, systemChangeContext);
        } catch (Throwable t) {
            logger.error("Error executing repository job", t);
        } finally {
            BaseContext.popContext();
        }
    }
    
    @Override
    public void afterPropertiesSet() throws Exception {
        if (this.affectedPropDefPointers != null) {
            for (String pointer : this.affectedPropDefPointers) {
                PropertyTypeDefinition prop = this.resourceTypeTree.getPropertyDefinitionByPointer(pointer);
                if (this.affectedProperties == null) {
                    this.affectedProperties = new ArrayList<PropertyTypeDefinition>();
                }
                if (prop != null) {
                    this.affectedProperties.add(prop);
                }
            }
        }
    }
    
    public abstract void executeWithRepository(Repository repository, SystemChangeContext context) throws Exception;

    @Required
    public void setRepository(Repository repository) {
        this.repository = repository;
    }
    
    public void setSecurityContext(SecurityContext securityContext) {
        this.securityContext = securityContext;
    }
    
    @Required
    public void setSystemJobStatusPropDef(PropertyTypeDefinition systemJobStatusPropDef) {
        this.systemJobStatusPropDef = systemJobStatusPropDef;
    }

    @Required
    public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
    }

    public void setAffectedPropDefPointers(List<String> affectedPropDefPointers) {
        this.affectedPropDefPointers = affectedPropDefPointers;
    }

}
