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
package org.vortikal.repositoryimpl.index.observation;

import java.util.List;

/**
 * Interface for fetching changes to Vortex resources from en event source.
 *
 * @author oyviste
 */
public interface ResourceChangeFetcher {
    
    /**
     * Fetch all currently stored changes.
     * @return list of event objects
     */
    public List fetchChanges();

    /**
     * Fetch at most n stored changes.
     * @param n number of changes to be returned
     * @return list of event objects (size==n)
     */
    public List fetchChanges(int n);

    /**
     * Fetch only the most recent change for every resource, ignoring previous
     * changes for a given resource. 
     * NOTE: All resources in the returned list should be unique.
     * @return list of the last changes
     */
    public List fetchLastChanges();
    
    /**
     * Fetch at most n of only the most recent change for every resource.
     * NOTE: All resources in the returned list should be unique.
     * @param n number of changes to be returned
     * @return list of event objects
     */
    public List fetchLastChanges(int n);
    
    /**
     * Notify of the changes that have been processed, so that they can be removed
     * from storage.
     *
     * Any stored earlier changes to the resources should also be
     * removed from storage.
     * @param changes list of changes, all changes up until the latest occur change
     *        will be removed.
     */
    public void removeChanges(List changes);

    /**
     * Return number of pending/stored changes.
     * @return number of pending changes
     */
    public int countPendingChanges();
    
}
