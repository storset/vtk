/* Copyright (c) 2007, University of Oslo, Norway
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

import java.util.Date;

import org.vortikal.repositoryimpl.index.PropertySetIndex;
import org.vortikal.repositoryimpl.index.consistency.ConsistencyCheck;

/**
 * Thin management wrapper for performing asynchronous maintenance operations on
 * an index instance and retrieving results at a later time.
 *   
 * @author oyviste
 */
public interface IndexOperationManager {

    // id
    public PropertySetIndex getManagedInstance();
    
    // Locking
    public boolean lock();
    
    public void unlock();
    
    public boolean isLocked();
    
    // Close / re-initialize
    public void close() throws IllegalStateException;
    
    public void reinitialize() throws IllegalStateException;
    
    public boolean isClosed();
    
    // Re-indexing
    public void reindex(boolean asynchronous) throws IllegalStateException;
    
    public boolean isReindexing();
    
    public boolean lastReindexingCompletedNormally() throws IllegalStateException;
    
    public Exception getLastReindexingException();
    
    public Date getLastReindexingCompletionTime();
    
    public boolean hasReindexingResults();
    
    public void clearLastReindexingResults() throws IllegalStateException;
    
    public int getLastReindexingResourceCount() throws IllegalStateException;

    // Consistency check
    public void checkConsistency(boolean asynchronous) throws IllegalStateException;
    
    public ConsistencyCheck getLastConsistencyCheck() throws IllegalStateException;
    
    public Exception getLastConsistencyCheckException();
    
    public Date getLastConsistencyCheckCompletionTime();
    
    public boolean lastConsistencyCheckCompletedNormally() throws IllegalStateException;
    
    public boolean isCheckingConsistency();
    
    public void clearLastConsistencyCheckResults() throws IllegalStateException;
    
}
