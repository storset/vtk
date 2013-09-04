/* Copyright (c) 2009, University of Oslo, Norway
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

import org.vortikal.repository.ChangeLogEntry;

public interface ChangeLogDAO {

    /**
     * Get changelog entries with the given logger type and id.
     * 
     * @param loggerType
     * @param loggerId
     * @return
     * @throws DataAccessException 
     */
    public List<ChangeLogEntry> getChangeLogEntries(int loggerType, int loggerId)
        throws DataAccessException;

    /**
     * Get changelog entries with the given logger type and id.
     * 
     * @param loggerType
     * @param loggerId
     * @param limit Limit the number of entries returned.
     * @return
     * @throws DataAccessException 
     */
    public List<ChangeLogEntry> getChangeLogEntries(int loggerType, int loggerId, int limit)
    	throws DataAccessException;

    /**
     * Remove changelog entries.
     * @param entries
     * @throws DataAccessException 
     */
    public void removeChangeLogEntries(List<ChangeLogEntry> entries)
        throws DataAccessException;
    
    /**
     * Add changelog entry, optionally add entries for entire subtree.
     * @param entry
     * @param recurse
     * @throws DataAccessException 
     */
    public void addChangeLogEntry(ChangeLogEntry entry, boolean recurse)
        throws DataAccessException;

    /**
     * Add change log entry for resource and all resources which inherit
     * ACL from the resource.
     * 
     * @param entry
     * @throws DataAccessException 
     */
    public void addChangeLogEntryInherited(ChangeLogEntry entry)
        throws DataAccessException;
    
    /**
     * Add change log entry for resource and all descendants which inherit
     * their ACL from the same ancestor that resource itself inherits from.
     * 
     * @param entry
     * @throws DataAccessException 
     */
    public void addChangeLogEntryInheritedToInheritance(ChangeLogEntry entry)
    throws DataAccessException;

}
