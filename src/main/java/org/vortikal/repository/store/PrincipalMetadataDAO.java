/* Copyright (c) 2008, University of Oslo, Norway
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
package org.vortikal.repository.store;

import java.util.List;

import org.vortikal.repository.RepositoryException;
import org.vortikal.security.Principal;
import org.vortikal.security.Principal.Type;

public interface PrincipalMetadataDAO {

    /**
     * Get principal description.
     * 
     * @param The <code>Principal</code> id to get the description for.
     * @return A <code>String</code> with description or <code>null</code> if none found.
     * 
     * @throws RepositoryException
     */
    public String getDescription(String uid) throws RepositoryException;
    
    
    /**
     * Returns a URL for given principal id, or <code>null</code> if unknown.
     * @param principal
     * @return
     */
    public String getUrl(String uid, String domain);
    

    /**
     * Searches for a set of principals that satisfy the supplied search string
     * @param searchString String to use in search
     * @param type Type of search to perform (users or groups)
     * @return List of principals who satisfy the given search string
     * @throws RepositoryException
     */
    public List<Principal> search(String searchString, Type type) throws RepositoryException;
    
}
