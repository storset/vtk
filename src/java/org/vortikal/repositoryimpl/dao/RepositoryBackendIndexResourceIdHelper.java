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

package org.vortikal.repositoryimpl.dao;

import java.io.IOException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repositoryimpl.ResourceImpl;
import org.vortikal.repositoryimpl.index.util.IndexResourceIdHelper;
import org.vortikal.util.repository.URIUtil;


/**
 * Derive resource IDs from Vortex repository backend.
 * This will typically be the Cache wrapper implementation around a
 * DataAccessor.
 *
 * Note: This class essentially voids the need for the ID generator helpers that
 *       access the backend database directly. It should also be more effective
 *       since the Cache dao can be used.
 *
 * @see org.vortikal.repositoryimpl.dao.DataAccessor
 * @see org.vortikal.repositoryimpl.dao.Cache
 *
 * @author oyviste
 */
public class RepositoryBackendIndexResourceIdHelper implements IndexResourceIdHelper, 
                                                               InitializingBean {
    
    Log logger = LogFactory.getLog(this.getClass());    
    private DataAccessor dao;
    
    public void afterPropertiesSet() {
        if (dao == null) {
            throw new BeanInitializationException("DataAccessor not set.");
        }
    }

    /**
     * Generate a string with IDs of all parent collections to resource
     * given by URI.
     *
     * @param uri The URI of the resource.
     * @return A string of IDs separated by whitespace.
     * @return null if unable to generate the string of IDs.
     */
    public String getResourceParentCollectionIds(String uri) {
        
        // Strip slash at end of URI, if any.
        if (uri.endsWith("/") && uri.length() > 1) {
            uri = uri.substring(0, uri.length()-1);
        }
        
        // Generate String with parent IDs
        StringBuffer idString = new StringBuffer();
        try {
            ResourceImpl r = dao.load(uri);
            
            if (r == null) return null;
            
            String parent;
            while ((parent = URIUtil.getParentURI(r.getURI())) != null) {
                r = dao.load(parent);
                idString.append(r.getID());
                idString.append(" ");
            }
        } catch (IOException io) {
            logger.warn("IOException while fetching parent collection IDs for resource " + uri, io);
            return null;
        }

        return idString.toString();
    }
    
    /** 
     * Returns a String with ID for resource given by URI.
     *
     * @param uri The URI of the resource.
     * @return An ID String for the resource.
     * @return null if unable to load resource.
     */
    public String getResourceId(String uri) {
        // Strip slash at end of URI, if any.
        if (uri.endsWith("/") && uri.length() > 1) {
            uri = uri.substring(0, uri.length()-1);
        }

        try {
            ResourceImpl r = dao.load(uri);
            return Integer.toString(r.getID());
        } catch (IOException io) {
            logger.warn("IOException while fetching ID for resource " + uri, io);
            return null;
        }
    }
    
    /**
     * Set DataAccessor property.
     * @see org.vortikal.repositoryimpl.dao.DataAccessor;
     */
    public void setDao(DataAccessor dao) {
        this.dao = dao;
    }
}
