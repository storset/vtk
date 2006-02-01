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

import org.vortikal.repositoryimpl.index.observation.FilterCriterion;

/**
 * Management interface for a reindexer implementation.
 * @author oyviste
 * TODO: Javadoc
 */
public interface Reindexer extends ManagementOperation {

    /**
     * Reindex everything from the root.
     * TODO: general management exception might not be the appropriate type here, 
     *       but it's what we got so far.
     */
    public void start() throws ManagementException;

    /**
     * Only reindex a particular subtree.
     * @param subtreeURI
     */
    public void start(String subtreeURI) throws ManagementException;

    /**
     * If reindexing is running asynchronously, this method can be called to
     * stop it. 
     *
     */
    public void stop() throws ManagementException;

    /**
     * TODO: javadoc
     */
    public boolean isRunning();

    /**
     * TODO: javadoc
     * @return
     */
    public String getWorkerThreadName();

    /**
     * TODO: javadoc
     * @return
     */
    public String getCurrentWorkingTree();

    /**
     * TODO: javadoc
     * @return
     */
    public boolean isAsynchronous();

    /**
     * TODO: javadoc
     * @return
     */
    public FilterCriterion getFilter();
    
    /**
     * TODO: javadoc
     * @return
     */
    public boolean isSkipFilteredSubtrees();

    /**
     * TODO: javadoc
     * @return
     */
    public String getIndexId();
}
