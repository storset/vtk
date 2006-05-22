/* Copyright (c) 2006, University of Oslo, Norway
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
package org.vortikal.repositoryimpl.query.parser;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.vortikal.repository.PropertySet;



/**
 * Simple cached result set. Uses <code>LinkedList</code> internally, as it
 * suits the current usage pattern best.  
 * 
 * XXX: Result type is assumed to be <code>PropertySet</code> instances.
 * 
 * @author oyviste
 */
public class ResultSetImpl implements ResultSet {

    private List results;
    
    public ResultSetImpl() {
        results = new LinkedList();
    }
    
    /**
     * Not efficient with linked lists.
     */
    public Object getResult(int index) {
        return results.get(index);
    }

    public List getResults(int maxIndex) {
        int max = Math.min(maxIndex, this.results.size());
        
        return results.subList(0, max);
    }
   
    /**
     * Probably not efficient with linked lists.
     */
    public List getResults(int fromIndex, int toIndex) {
        return results.subList(fromIndex, toIndex);
    }

    public List getAllResults() {
        return this.results;
    }

    public int getSize() {
        return results.size();
    }
    
    public void addResult(PropertySet propSet) {
        this.results.add(propSet);
    }
    
    public Iterator iterator() {
        return this.results.iterator();
    }

}
