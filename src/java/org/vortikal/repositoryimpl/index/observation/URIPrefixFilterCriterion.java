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

import org.springframework.beans.factory.InitializingBean;

/**
 * Filter out resources based on URI prefix inclusions and exclusions.
 * The evaluation order is first check inclusions, then exclusions. This means
 * that if the same URI prefix exists in the set of inclusions and exclusions, it will
 * be excluded. An empty list of inclusions results in everything being included, 
 * while en empty list of exclusions will not exclude anything.
 *
 * Wildcards cannot be used. See @link{URIWildcardFilterCriterion} instead.
 * 
 * @author oyviste
 * TODO: Probably not the most efficient way of implementing this. 
 */
public class URIPrefixFilterCriterion implements FilterCriterion, InitializingBean {
    
    /** URI prefixes to include */
    private String[] inclusions;
    
    /** URI prefixes to exclude */
    private String[] exclusions;
    
    public void afterPropertiesSet() {
        if (this.inclusions == null) {
            this.inclusions = new String[]{};
        } 
        
        if (this.exclusions == null) {
            this.exclusions = new String[]{};
        }
    }
    
    /** Creates a new instance of URIFilter */
    public URIPrefixFilterCriterion() {
    }
    
    public boolean isFiltered(String uri) {
        boolean include;

        // Initialize default include policy.
        include = this.inclusions.length == 0 ? true: false;
        
        // Test against include list.
        for (int i=0; i<this.inclusions.length; i++) {
            if (uri.startsWith(this.inclusions[i])) {
                include = true;
                break;
            }
        }
        
        // Test against exclude list.
        for (int i=0; i<this.exclusions.length; i++) {
            if (uri.startsWith(this.exclusions[i])) {
                include = false;
                break;
            }
        }
        
        return !include;
    }
    
    public void setInclusions(String[] inclusions) {
        this.inclusions = inclusions;
    }
    
    public void setExclusions(String[] exclusions) {
        this.exclusions = exclusions;
    }
    
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("URIPrefix[");
        for (int i=0; i<this.inclusions.length; i++) {
            buffer.append("inc=");
            buffer.append(this.inclusions[i]);
            if (i < this.inclusions.length-1) buffer.append(", ");
        }
        
        if (this.exclusions.length > 0 && this.inclusions.length > 0) 
            buffer.append(", ");
        
        for (int i=0; i<this.exclusions.length; i++) {
            buffer.append("ex=");
            buffer.append(this.exclusions[i]);
            if (i < this.exclusions.length-1) buffer.append(", ");
        }
        
        buffer.append("]");
        return buffer.toString();
    }
}
