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

package org.vortikal.repositoryimpl.index.observation;


/**
 * A filter criterion determines if a resource should be filtered from 
 * indexing or not. This is really only meant to be used for light filtering at the
 * observation level. Heavy filters requiring the use of the repository will probably
 * void the performance benefits of filtering altogether, and should thus be used
 * carefully (<code>ResourcePropertyFilterCriterion</code>, for instance).
 * 
 * @author oyviste
 */
public interface FilterCriterion {

    /**
     * Determine if a resource should be filtered based on its URI.
     * URI was chosen so that it is not necessary to explicitly fetch resources
     * from the repository, if a resource object is not immediately available.
     * This is for done to minimize the number of necessary repository accesses 
     * in the indexing process while at the same time ensuring that filter criteria 
     * can be used at any level.
     * 
     * @param uri
     * @return <code>true</code> iff the resource should be filtered away, 
     *         <code>false</code> otherwise.
     */
    public boolean isFiltered(String uri);
    
}
