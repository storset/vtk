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
package org.vortikal.util.net;

/**
 * Matches IPv4 addresses specified in dotted-decimal form with an optional
 * wildcard for any of the four octets. The wildcard '*' can only be used 
 * <em>once</em> in a pattern, 
 * and the wildcard will greedily match as many octets as possible.
 * 
 * <p>Examples of valid wildcard patterns:</p>
 * <ul>
 *  <li>10.*</li>
 *  <li>129.240.*</li>
 *  <li>*.10</li>
 *  <li>192.168.0.1</li>
 *  <li>192.168.0.*</li>
 *  <li>192.168.*</li>
 *  <li>192.*</li>
 *  <li>*</li>
 * </ul>
 * 
 * @author oyviste
 */
public class IPv4WildcardMatcher extends AbstractIPv4Matcher 
                                            implements IPv4Matcher {
    
    public static final char WILDCARD = '*';
    private static final int OCTET_PATTERN_WILDCARD = -1;
    
    private final int[] octetPattern;
    
    public IPv4WildcardMatcher(String ipv4Wildcard) {
        
        String[] parts = ipv4Wildcard.split("\\.");
        
        if (parts.length > 4) {
            throw new IllegalArgumentException("Invalid IPv4 wildcard pattern : '" 
                    + ipv4Wildcard + "' (more than a maximum of 4 parts)");
        }
        
        int wildcardCount = 0;
        for (int i=0; i<parts.length; i++) {
            if (isWildcard(parts[i])) {
                ++wildcardCount;
            }
        }
        if (wildcardCount > 1) {
            throw new IllegalArgumentException("Invalid IPv4 wildcard pattern: '"
                    + ipv4Wildcard + "' (wildcard occurs more than once)");
        } else if (wildcardCount == 0 && parts.length < 4) {
            throw new IllegalArgumentException("Invalid IPv4 wildcard pattern: '"
                    + ipv4Wildcard + "' (too few parts and no wildcard)");
        }
        
        int[] octetPattern = new int[4];
        int expand = 4 - parts.length;
        int patternCursor = 0;
        for (int i=0; i<parts.length; i++) {
            if (isWildcard(parts[i])) {
                octetPattern[patternCursor++] = OCTET_PATTERN_WILDCARD;
                for (int j=0; j<expand; j++) {
                    octetPattern[patternCursor++] = OCTET_PATTERN_WILDCARD;
                }   
            } else {
                int octet = Integer.parseInt(parts[i]);
                if (octet < 0x00 || octet > 0xFF) {
                    throw new IllegalArgumentException("Invalid octet: " + parts[i]);
                }
                octetPattern[patternCursor++] = octet;
            }
        }
        
        this.octetPattern = octetPattern;
        
    }
    
    private final boolean isWildcard(String s) {
        return (s.length() == 1 && s.charAt(0) == WILDCARD);
    }
    
    public boolean matches(String ipv4Address) {
        int[] octets = parseDottedDecimalNumber(ipv4Address);
        
        for (int i=0; i<4; i++) {
            if (this.octetPattern[i] == OCTET_PATTERN_WILDCARD) continue;
            
            if (this.octetPattern[i] != octets[i]) {
                return false;
            }
        }
        
        return true;
    }
    
    public String toString() {
        StringBuffer buffer = new StringBuffer("IPv4WildcardMatcher[");
        for (int i=0; i<this.octetPattern.length; i++) {
            if (this.octetPattern[i] == OCTET_PATTERN_WILDCARD) {
                buffer.append(WILDCARD);
            } else {
                buffer.append(this.octetPattern[i]);
            }
            if (i < this.octetPattern.length-1) buffer.append('.');
        }
        buffer.append(']');
        
        return buffer.toString();
    }

}
