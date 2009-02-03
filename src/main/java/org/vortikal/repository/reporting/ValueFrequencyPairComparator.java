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
package org.vortikal.repository.reporting;

import java.util.Comparator;

import org.vortikal.repository.resourcetype.Value;

public class ValueFrequencyPairComparator implements Comparator<Pair<Value, Integer>> {

    PropertyValueFrequencyQuery.Ordering ordering;
    
    public ValueFrequencyPairComparator(PropertyValueFrequencyQuery.Ordering ordering) {
        this.ordering = ordering;
    }
    
    public int compare(Pair<Value, Integer> o1, Pair<Value, Integer> o2) {
        switch(this.ordering) {
        case ASCENDING_BY_FREQUENCY:
            return o1.second().compareTo(o2.second());
            
        case DESCENDING_BY_FREQUENCY:
            return o2.second().compareTo(o1.second());
            
        case ASCENDING_BY_PROPERTY_VALUE:
            return o1.first().compareTo(o2.first());
            
        case DESCENDING_BY_PROPERTY_VALUE:
            return o2.first().compareTo(o1.first());
            
        default:
            return o1.first().compareTo(o2.first());
        }
    }

    
}
