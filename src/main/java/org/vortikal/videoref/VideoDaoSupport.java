/* Copyright (c) 2013, University of Oslo, Norway
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

package org.vortikal.videoref;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;

/**
 * Provides support functions for direct database queries related to
 * videoref resources.
 */
public class VideoDaoSupport extends JdbcDaoSupport {

    private PropertyTypeDefinition videoIdProperty;
    
    /**
     * Counts number of references to the given video id in repository.
     * 
     * @param videoId the video id
     * @return the number of references from resources in repository. Resources
     * in trash are also included in the count.
     * 
     * @throws DataAccessException in case of database query errors
     */
    public int countReferences(VideoId videoId) throws DataAccessException {
        
        String nsUri = videoIdProperty.getNamespace().getUri();
        String name = videoIdProperty.getName();
        String value = videoId.toString();
        
        String sql = "SELECT COUNT(1) FROM extra_prop_entry"
                     + " WHERE name_space " + (nsUri == null ? "is null" : "= ?")
                     + " AND name = ? AND value = ?";
        
        if (nsUri == null) {
            return getJdbcTemplate().queryForInt(sql, name, value);
        } else {
            return getJdbcTemplate().queryForInt(sql, nsUri, name, value);
        }
    }
    
    /**
     * @param videoIdProperty the videoIdProperty to set
     */
    @Required
    public void setVideoIdProperty(PropertyTypeDefinition videoIdProperty) {
        this.videoIdProperty = videoIdProperty;
    }
    
}
