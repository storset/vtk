/* Copyright (c) 2004, University of Oslo, Norway
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
package org.vortikal.repositoryimpl.event;

import org.vortikal.repositoryimpl.ACL;
import org.vortikal.repositoryimpl.Collection;
import org.vortikal.repositoryimpl.RepositoryImpl;
import org.vortikal.repositoryimpl.Resource;
import org.vortikal.repositoryimpl.dao.DataAccessor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;


/**
 * ChangeListener implementation that reports modifications in a way
 * suitable for direct replication to other databases.
 */
public class ReplicationEventDumper implements EventListener {
    private final static String CREATED = "created";
    private final static String DELETED = "deleted";
    private final static String MODIFIED_PROPS = "modified_props";
    private final static String MODIFIED_CONTENT = "modified_content";
    private final static String MODIFIED_ACL = "modified_acl";
    private String type = "2";
    private String id;
    private DataAccessor dao;
    private Log logger = LogFactory.getLog(this.getClass());

    public void setID(String id) {
        this.id = id;
    }

    public void setRepository(RepositoryImpl repository) {
        this.dao = repository.getDataAccessor();
    }

    public void created(Resource resource) {
        try {
            dao.addChangeLogEntry(id, type, resource.getURI(), CREATED);

            if (resource.isCollection()) {
                String[] childUris = dao.listSubTree((Collection) resource);

                for (int i = 0; i < childUris.length; i++) {
                    dao.addChangeLogEntry(id, type, childUris[i], CREATED);
                }
            }
        } catch (IOException e) {
            logger.warn("Caught IOException while reporting resource creation " +
                "for uri " + resource.getURI(), e);
        }
    }

    public void deleted(String uri) {
        try {
            dao.addChangeLogEntry(id, type, uri, DELETED);
        } catch (IOException e) {
            logger.warn("Caught IOException while reporting resource deletion " +
                "for uri " + uri, e);
        }
    }

    public void modified(Resource resource, Resource originalResource) {
        try {
            dao.addChangeLogEntry(id, type, resource.getURI(), MODIFIED_PROPS);
        } catch (IOException e) {
            logger.warn(
                "Caught IOException while reporting property modification " +
                "for uri " + resource.getURI(), e);
        }
    }

    public void contentModified(Resource resource) {
        try {
            dao.addChangeLogEntry(id, type, resource.getURI(), MODIFIED_CONTENT);
        } catch (IOException e) {
            logger.warn(
                "Caught IOException while reporting content modification " +
                "for uri " + resource.getURI(), e);
        }
    }

    public void aclModified(Resource resource, Resource originalResource,
        ACL originalACL, ACL newACL, boolean wasInherited) {
        try {
            if (originalACL.equals(newACL) &&
                    (resource.getInheritedACL() == wasInherited)) {
                return;
            }

            dao.addChangeLogEntry(id, type, resource.getURI(), MODIFIED_ACL);
        } catch (IOException e) {
            logger.warn("Caught IOException while reporting ACL modification " +
                "for uri " + resource.getURI(), e);
        }
    }
}
