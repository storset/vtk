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
 * URI wildcard filtering.
 * The matching is anchored and greedy. The <code>WILDCARD</code> will match
 * everything, including itself and the empty string, both at the start, in the 
 * middle and at the end.
 * 
 * <p>
 * Matching examples:
 * <ul>
 *  <li>'*.xml' will match both 'foo.xml' and '/foo/bar/doc.xml'</li>
 *  <li>'*bar/*' will match every resource under subtrees called 'bar'.</li>
 *  <li>'/prefixdir*' will match every resource with a prefix of '/prefixdir'</li>
 *  <li>'*.secret/*.xml*' will match xml files under folders called '.secret'</li>
 * </ul> 
 * 
 * <p>
 * The evaluation order is first check the list of <code>includePatterns</code>, then 
 * <code>excludePatterns</code>. This means that if the same URI pattern exists in the set 
 * of inclusions and exclusions, it will be excluded. An empty list of inclusions 
 * results in everything being included while en empty list of exclusions will not 
 * exclude anything.
 * 
 * @author oyviste
 */
public class URIWildcardFilterCriterion implements FilterCriterion,
        InitializingBean {

    public static final char WILDCARD = '*';
    
    private String[] includePatterns;
    private String[] excludePatterns;

    public void afterPropertiesSet() {
        if (this.includePatterns == null) {
            this.includePatterns = new String[]{};
        }
        
        if (this.excludePatterns == null) {
            this.excludePatterns = new String[]{};
        }

        for (int i=0; i<this.includePatterns.length; i++) {
            this.includePatterns[i] = this.includePatterns[i].replaceAll("\\*+", "*");
        }
        
        for (int i=0; i<this.excludePatterns.length; i++) {
            this.excludePatterns[i] = this.excludePatterns[i].replaceAll("\\*+", "*");
        }
    }
    
    /**
     * @see FilterCriterion#isFiltered(String)
     */
    public boolean isFiltered(String uri) {
        boolean filter = this.includePatterns.length == 0 ? false : true;
        
        for (int i=0; i<this.includePatterns.length; i++) {
            if (match(uri, this.includePatterns[i])) {
                filter = false;
            }
        }

        for (int i=0; i<this.excludePatterns.length; i++) {
            if (match(uri, this.excludePatterns[i])) {
                filter = true;
            }
        }
        
        return filter;
    }

    /**
     * Check if a string matches a wildcard-pattern (like /*.foo*bar.*).
     * The matching is always anchored, which means that the pattern must match
     * the entire string. The wildcard is greedy, and will eat as much as possible.
     * The wildcard will always match the empty string, and itself.
     * 
     * @param s The string to test.
     * @param p The wildcard pattern to test against.
     * @return <code>true</code> iff there is a match, <code>false</code>
     *          otherwise.
     */
    private final boolean match(String s, String p) {
        int sLength = s.length();
        int pLength = p.length();

        if (pLength == 0) {
            if (sLength == 0) return true;

            return false;
        }

        int sp = 0, pp = 0;
        while (pp < pLength && sp < sLength) {
            if (p.charAt(pp) == WILDCARD) {
                if (pp == pLength - 1) return true;   
                int nextW = p.indexOf(WILDCARD, ++pp);
                sp = nextW == -1 ? s.lastIndexOf(p.substring(pp, pLength)) :
                                   s.lastIndexOf(p.substring(pp, nextW));
                if (sp == -1) return false;
            } else {
                if (p.charAt(pp++) != s.charAt(sp++)) return false;
            }
        }
        
        return (sp == sLength && pp == pLength) ||
               (pp == pLength-1 && p.charAt(pp) == WILDCARD);
    }

    public void setExcludePatterns(String[] excludePatterns) {
        this.excludePatterns = excludePatterns;
    }

    public void setIncludePatterns(String[] includePatterns) {
        this.includePatterns = includePatterns;
    }
    
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("URIWildcard[");
        for (int i=0; i<this.includePatterns.length; i++) {
            buffer.append("inc=");
            buffer.append(this.includePatterns[i]);
            if (i < this.includePatterns.length-1) buffer.append(", ");
        }
        
        if (this.excludePatterns.length > 0 && this.includePatterns.length > 0) 
            buffer.append(", ");
        
        for (int i=0; i<this.excludePatterns.length; i++) {
            buffer.append("ex=");
            buffer.append(this.excludePatterns[i]);
            if (i < this.excludePatterns.length-1) buffer.append(", ");
        }
        
        buffer.append("]");
        return buffer.toString();
    }    
}
