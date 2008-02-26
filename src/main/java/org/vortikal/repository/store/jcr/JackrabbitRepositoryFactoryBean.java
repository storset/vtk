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
package org.vortikal.repository.store.jcr;

import java.io.File;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.io.Resource;

public class JackrabbitRepositoryFactoryBean implements FactoryBean {

    private Resource config;
    private String home;
    private Repository repository;
    private static Log logger = LogFactory.getLog(JackrabbitRepositoryFactoryBean.class);
    
    public synchronized Object getObject() throws Exception {
        if (this.repository != null) {
            return this.repository;
        }
        
        File homeDir = new File(this.home);
        if (!homeDir.exists()) {
            logger.info("Repository home directory does not exist, creating:" + homeDir.getAbsolutePath());
            homeDir.mkdirs();
        }
        // Load the configuration and create the repository
        logger.info("Reading repository config from " + this.config);
        RepositoryConfig rc = RepositoryConfig.create(this.config.getInputStream(),
                home);
        this.repository = new InternalRepo(rc);
        return this.repository;
    }

    public Class<InternalRepo> getObjectType() {
        return InternalRepo.class;
    }

    public boolean isSingleton() {
        return true;
    }

    @Required public void setConfig(Resource config) {
        this.config = config;
    }

    @Required public void setHome(String home) {
        this.home = home;
    }

    private class InternalRepo extends RepositoryImpl {
        public InternalRepo(RepositoryConfig rc) throws RepositoryException {
            super(rc);
            logger.info("Configs: " + rc.getWorkspaceConfigs());
        }
    }

}
