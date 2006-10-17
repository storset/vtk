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
package org.vortikal.repositoryimpl.index.management;

import java.util.List;

import org.vortikal.repositoryimpl.index.Index;
import org.vortikal.repositoryimpl.index.observation.ResourceChangeObserver;


/**
 * Runtime management of all indexes in an application context.
 * Enumerates various index-related bean instances, provides access
 * to management operations, etc.
 * TODO: Javadoc
 * TODO: Re-initialization of indexes.
 * TODO: Deleting all contents of an index.
 * 
 * @author oyviste
 *
 */
public interface RuntimeManager {
    
    /**
     * Get a list of all configured index ids. Should be used as a handle
     * when doing other operations.
     */
    public List getIndexes() throws ManagementException;
    
    /**
     * Get an <code>Index</code> instance given by id.
     * @param id The index id.
     * @return An {@link org.vortikal.repositoryimpl.index.Index instance}
     */
    public Index getIndex(String indexId) throws ManagementException;
    

    /**
     * Get a list of all configured observers (usually one per index, if there
     * is no filtering, or otherwise complex configuration).
     */
    public List getObservers() throws ManagementException;
    

    /**
     * 
     * @param observerId
     * @throws ManagementException
     */
    public ResourceChangeObserver getObserver(String observerId)
        throws ManagementException;

    /**
     * 
     * @throws ManagementException
     */
    public void disableAllObservers() throws ManagementException;
    
    /**
     * 
     * @throws ManagementException
     */
    public void enableAllObservers() throws ManagementException;
    
    /**
     * Get a reindexer-instance for the given index id. Can be used
     * to start/stop re-indexing and perform status queries. Reindexers are
     * currently mapped 1:1 to index instances, and one re-indexer must be
     * configured per index.
     * 
     * @return The <code>Reindexer</code> instance. Returns <code>null</code> if no
     *         reindexer was configured for the given index, or the given index id
     *         was not found.
     */
    public Reindexer getReindexerForIndex(Index index) throws ManagementException;
    
    /**
     * Get status information for the given index.
     * @throws ManagementException
     */
    public IndexStatus getStatusForIndex(Index index) throws ManagementException;
    
    /**
     * Request optimization of the given index.
     * @throws ManagementException
     */
    public void optimizeIndex(Index index) throws ManagementException;
}
