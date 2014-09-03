/* Copyright (c) 2014, University of Oslo, Norway
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

package org.vortikal.repository.index.mapping;

import org.vortikal.repository.Acl;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.search.ResultSetImpl;

/**
 * Result set with support for retriving result ACLs.
 * 
 * <p>Note that ACLs will only be available when the fields are selected
 * for loading at query time.
 * 
 * TODO consider providing direct search result set mapping from document mapper
 * to avoid exposing this class too much.
 */
public class ResultSetWithAcls extends ResultSetImpl {

    public ResultSetWithAcls() {
    }

    public ResultSetWithAcls(int initialCapacity) {
        super(initialCapacity);
    }

    @Override
    public boolean isInheritedAcl(int index) {
        PropertySet result = results.get(index);
        return ((LazyMappedPropertySet)result).isInheritedAcl();
    }

    @Override
    public Acl getAcl(int index) {
        PropertySet result = results.get(index);
        return ((LazyMappedPropertySet)result).getAcl();
    }

    /**
     * Add a result to this result set.
     * @param propSet an instance of {@link LazyMappedPropertySet}
     */
    @Override
    public void addResult(PropertySet propSet) {
        if (propSet.getClass() != LazyMappedPropertySet.class) {
            throw new IllegalArgumentException("Property set must be an instance of " + LazyMappedPropertySet.class + " for this result set implementation");
        }
        
        super.addResult(propSet);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(this.getClass().getName());
        sb.append(": [size=").append(super.results.size());
        sb.append(", totalHits=").append(super.totalHits).append("]");
        return sb.toString();
    }
    
    
}
