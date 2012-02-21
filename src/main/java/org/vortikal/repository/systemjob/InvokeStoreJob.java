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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceNotFoundException;
import org.vortikal.security.SecurityContext;

/**
 * Periodic task that should invoke store on a set of repository resources
 * periodically.
 */
public class InvokeStoreJob extends RepositoryJob {

    private PathSelector pathSelector;
    
    private final Log logger = LogFactory.getLog(getClass());
    
    @Override
    public void executeWithRepository(Repository repository, SystemChangeContext context) throws Exception {
        
        if (repository.isReadOnly()) {
            return;
        }

        String token = null;
        if (SecurityContext.exists()) {
            token = SecurityContext.getSecurityContext().getToken();
        }

        List<Path> selectedPaths = this.pathSelector.selectPaths(repository, context);
        
        logger.info("Running job '" + getId() + "', " + selectedPaths.size() + " resource(s) to be affected");
        
        for (Path path : selectedPaths) {
            try {

                Resource resource = repository.retrieve(token, path, false);
                if (resource.getLock() == null) {
                    repository.store(token, resource, context);
                } else {
                    logger.warn("Resource " + resource + " currently locked, will not invoke store for periodic job.");
                }
            } catch (ResourceNotFoundException rnfe) {
                // Resource is no longer there after search (deleted, moved
                // or renamed)
                logger.warn("A resource ("
                        + path
                        + ") that was to be affected by a systemjob was no longer available: "
                        + rnfe.getMessage());
            }
        }
    }
    
    @Required
    public void setPathSelector(PathSelector pathSelector) {
        this.pathSelector = pathSelector;
    }
}
