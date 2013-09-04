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

import junit.framework.TestCase;

/**
 * A JUnit <code>TestCase</code> for the <code>IPv4WildcardMatcher</code> class.
 * 
 * @author oyviste
 *
 */
public class IPv4WildcardMatcherTest extends TestCase {

    public void testMatches() {
        
        IPv4Matcher matcher = new IPv4WildcardMatcher("129.240.*");
        assertTrue(matcher.matches("129.240.1.1"));
        assertTrue(matcher.matches("129.240.255.255"));
        assertTrue(matcher.matches("129.240.100.100"));
        assertTrue(matcher.matches("129.240.200.200"));
        assertTrue(matcher.matches("129.240.0.0"));
        assertTrue(matcher.matches("129.240.3.4"));
        assertTrue(matcher.matches("129.240.16.17"));
        assertTrue(matcher.matches("129.240.9.10"));
        assertFalse(matcher.matches("129.241.1.1"));
        assertFalse(matcher.matches("129.239.255.255"));
        assertFalse(matcher.matches("128.240.1.1"));
        assertFalse(matcher.matches("129.0.0.0"));
        assertFalse(matcher.matches("129.255.1.1"));
        
        matcher = new IPv4WildcardMatcher("*");
        assertTrue(matcher.matches("129.240.1.1"));
        assertTrue(matcher.matches("129.240.255.255"));
        assertTrue(matcher.matches("129.240.100.100"));
        assertTrue(matcher.matches("129.240.200.200"));
        assertTrue(matcher.matches("129.240.0.0"));
        assertTrue(matcher.matches("129.240.3.4"));
        assertTrue(matcher.matches("129.240.16.17"));
        assertTrue(matcher.matches("129.240.9.10"));
        assertTrue(matcher.matches("129.241.1.1"));
        assertTrue(matcher.matches("129.239.255.255"));
        assertTrue(matcher.matches("128.240.1.1"));
        assertTrue(matcher.matches("129.0.0.0"));
        assertTrue(matcher.matches("129.255.1.1"));
        assertTrue(matcher.matches("255.255.255.255"));
        assertTrue(matcher.matches("0.0.0.0"));
        assertTrue(matcher.matches("192.168.1.1"));
        assertTrue(matcher.matches("10.0.0.1"));
        
        matcher = new IPv4WildcardMatcher("10.*.1");
        assertTrue(matcher.matches("10.0.0.1"));
        assertTrue(matcher.matches("10.2.3.1"));
        assertTrue(matcher.matches("10.255.255.1"));
        assertFalse(matcher.matches("11.0.0.1"));
        assertFalse(matcher.matches("10.3.3.2"));
        assertFalse(matcher.matches("10.0.0.10"));
        
        matcher = new IPv4WildcardMatcher("250.250.250.250");
        assertTrue(matcher.matches("250.250.250.250"));
        assertFalse(matcher.matches("250.250.250.249"));
        
        matcher = new IPv4WildcardMatcher("192.168.1.*");
        assertTrue(matcher.matches("192.168.1.10"));
        assertTrue(matcher.matches("192.168.1.255"));
        assertFalse(matcher.matches("192.168.2.1"));

        matcher = new IPv4WildcardMatcher("192.*.1.2");
        assertTrue(matcher.matches("192.10.1.2"));
        assertTrue(matcher.matches("192.0.1.2"));
        assertFalse(matcher.matches("192.0.2.1"));

        
        matcher = new IPv4WildcardMatcher("*");
        assertTrue(matcher.matches("129.240.1.1"));
        assertTrue(matcher.matches("129.240.255.255"));
        assertTrue(matcher.matches("129.240.100.100"));
        assertTrue(matcher.matches("129.240.200.200"));
        assertTrue(matcher.matches("129.240.0.0"));
        assertTrue(matcher.matches("129.240.3.4"));
        assertTrue(matcher.matches("129.240.16.17"));
        assertTrue(matcher.matches("129.240.9.10"));
        assertTrue(matcher.matches("129.241.1.1"));
        assertTrue(matcher.matches("129.239.255.255"));
        assertTrue(matcher.matches("128.240.1.1"));
        assertTrue(matcher.matches("129.0.0.0"));
        assertTrue(matcher.matches("129.255.1.1"));
        assertTrue(matcher.matches("255.255.255.255"));
        assertTrue(matcher.matches("0.0.0.0"));
        assertTrue(matcher.matches("192.168.1.1"));
        assertTrue(matcher.matches("10.0.0.1"));
        assertTrue(matcher.matches("10.0.0.1"));
        assertTrue(matcher.matches("10.2.3.1"));
        assertTrue(matcher.matches("10.255.255.1"));
        
    }
    public void testInvalidMatches() {
        
        try {
            IPv4Matcher matcher = new IPv4WildcardMatcher("*");
            matcher.matches("...");
            fail("No IllegalArgumentException thrown when trying to match '...'");
        } catch (IllegalArgumentException e){}

        try {
            IPv4Matcher matcher = new IPv4WildcardMatcher("*");
            matcher.matches("1.1");
            fail("No IllegalArgumentException thrown when trying to match '1.1'");
        } catch (IllegalArgumentException e){}

        try {
            IPv4Matcher matcher = new IPv4WildcardMatcher("*");
            matcher.matches("-1.255.255.254");
            fail("No IllegalArgumentException thrown when trying to match '-1.255.255.254'");
        } catch (IllegalArgumentException e){}
        
        try {
            IPv4Matcher matcher = new IPv4WildcardMatcher("*");
            matcher.matches("129.245.256.1");
            fail("No IllegalArgumentException thrown when trying to match '129.245.256.1'");
        } catch (IllegalArgumentException e){}
        
    }
    
    @SuppressWarnings("unused")
    public void testIllegalValues() {
        try {
            IPv4Matcher matcher = new IPv4WildcardMatcher("10.**");
            fail("No IllegalArgumentException thrown when constructing a matcher with the wildcard '10.**'");
        } catch (IllegalArgumentException e) {}
        
        try {
            IPv4Matcher matcher = new IPv4WildcardMatcher("*.255.*");
            fail("No IllegalArgumentException thrown when constructing a matcher with the wildcard '*.255.*'");
        } catch (IllegalArgumentException e) {}
        
        try {
            IPv4Matcher matcher = new IPv4WildcardMatcher("10.**.1.1");
            fail("No IllegalArgumentException thrown when constructing a matcher with the wildcard '10.**.1.1'");
        } catch (IllegalArgumentException e) {}

        try {
            IPv4Matcher matcher = new IPv4WildcardMatcher("*.*");
            fail("No IllegalArgumentException thrown when constructing a matcher with the wildcard '10.**.1.1'");
        } catch (IllegalArgumentException e) {}

        try {
            IPv4Matcher matcher = new IPv4WildcardMatcher("10.0.0.-1");
            fail("No IllegalArgumentException thrown when constructing a matcher with the wildcard '10.0.0.-1'");
        } catch (IllegalArgumentException e) {}
    }

}
