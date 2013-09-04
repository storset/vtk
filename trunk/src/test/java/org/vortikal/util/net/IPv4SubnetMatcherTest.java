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
 * A <code>TestCase</code> for the <code>IPv4SubnetMatcher</code> class.
 * 
 * @author oyviste
 */
public class IPv4SubnetMatcherTest extends TestCase {
    
    public void testMatches() {

        // Netmask notation (8 bits)
        IPv4Matcher matcher = new IPv4SubnetMatcher("10.0.0.0", "255.0.0.0");
        assertTrue(matcher.matches("10.0.0.32"));
        assertTrue(matcher.matches("10.0.0.1"));
        assertTrue(matcher.matches("10.0.0.254"));
        assertTrue(matcher.matches("10.0.0.0"));
        assertTrue(matcher.matches("10.0.0.255"));
        assertTrue(matcher.matches("10.0.1.1"));
        assertTrue(matcher.matches("10.0.1.255"));
        assertFalse(matcher.matches("192.168.0.1"));
        assertFalse(matcher.matches("192.168.1.1"));
        assertFalse(matcher.matches("192.168.2.1"));
        assertFalse(matcher.matches("255.255.255.1"));
        
        // CIDR notation (8 bits)
        matcher = new IPv4SubnetMatcher("10.0.0.0", 8);
        assertTrue(matcher.matches("10.0.0.32"));
        assertTrue(matcher.matches("10.0.0.1"));
        assertTrue(matcher.matches("10.0.0.254"));
        assertTrue(matcher.matches("10.0.0.0"));
        assertTrue(matcher.matches("10.0.0.255"));
        assertTrue(matcher.matches("10.0.1.1"));
        assertTrue(matcher.matches("10.0.1.255"));
        assertFalse(matcher.matches("192.168.0.1"));
        assertFalse(matcher.matches("192.168.1.1"));
        assertFalse(matcher.matches("192.168.2.1"));
        assertFalse(matcher.matches("255.255.255.1"));

        // UiO example - netmask notation (23 bits)
        matcher = new IPv4SubnetMatcher("129.240.92.0", "255.255.254.0");
        assertFalse(matcher.matches("128.240.92.0"));
        assertFalse(matcher.matches("128.240.92.144"));
        assertFalse(matcher.matches("128.240.92.255"));
        assertFalse(matcher.matches("128.240.93.150"));
        assertFalse(matcher.matches("128.240.93.200"));
        assertFalse(matcher.matches("128.240.93.255"));
        assertTrue(matcher.matches("129.240.92.0"));
        assertTrue(matcher.matches("129.240.92.144"));
        assertTrue(matcher.matches("129.240.92.255"));
        assertTrue(matcher.matches("129.240.93.150"));
        assertTrue(matcher.matches("129.240.93.200"));
        assertTrue(matcher.matches("129.240.93.255"));
        assertFalse(matcher.matches("129.240.94.1"));
        assertFalse(matcher.matches("129.240.94.254"));
        assertFalse(matcher.matches("129.240.91.255"));
        assertFalse(matcher.matches("129.240.90.1"));
        assertFalse(matcher.matches("129.240.99.1"));
        assertFalse(matcher.matches("130.100.1.1"));
        assertFalse(matcher.matches("131.110.94.1"));

        // UiO example subnet (CIDR notation, 23 bits)
        matcher = new IPv4SubnetMatcher("129.240.92.0", 23);
        assertFalse(matcher.matches("128.240.92.0"));
        assertFalse(matcher.matches("128.240.92.144"));
        assertFalse(matcher.matches("128.240.92.255"));
        assertFalse(matcher.matches("128.240.93.150"));
        assertFalse(matcher.matches("128.240.93.200"));
        assertFalse(matcher.matches("128.240.93.255"));
        assertTrue(matcher.matches("129.240.92.0"));
        assertTrue(matcher.matches("129.240.92.144"));
        assertTrue(matcher.matches("129.240.92.255"));
        assertTrue(matcher.matches("129.240.93.150"));
        assertTrue(matcher.matches("129.240.93.200"));
        assertTrue(matcher.matches("129.240.93.255"));
        assertFalse(matcher.matches("129.240.94.1"));
        assertFalse(matcher.matches("129.240.94.254"));
        assertFalse(matcher.matches("129.240.91.255"));
        assertFalse(matcher.matches("129.240.90.1"));
        assertFalse(matcher.matches("129.240.99.1"));
        assertFalse(matcher.matches("130.100.1.1"));
        assertFalse(matcher.matches("131.110.94.1"));
        
        matcher  = new IPv4SubnetMatcher("192.168.0.0", 16);
        assertTrue(matcher.matches("192.168.4.5"));
        assertFalse(matcher.matches("192.167.255.255"));
        
        matcher = new IPv4SubnetMatcher("1.2.3.0", 0);
        assertTrue(matcher.matches("1.2.3.0"));
        assertTrue(matcher.matches("255.255.255.255"));
        assertTrue(matcher.matches("0.0.0.0"));

        matcher = new IPv4SubnetMatcher("1.2.3.0", 32);
        assertTrue(matcher.matches("1.2.3.0"));
        assertFalse(matcher.matches("255.255.255.255"));
        
        matcher = new IPv4SubnetMatcher("224.100.100.0", 31);
        assertTrue(matcher.matches("224.100.100.0"));
        assertTrue(matcher.matches("224.100.100.1"));
        assertFalse(matcher.matches("224.100.100.2"));
        
    }
    
    public void testMatchesWithInvalidValues() {
        IPv4Matcher matcher = new IPv4SubnetMatcher("10.0.0.0", "255.0.0.0");
        try {
            matcher.matches("10");
            fail("Did not throw IllegalArgumentException when trying to match '10'");
        } catch (IllegalArgumentException e) {
        }
        
        try {
            matcher.matches("10.0.2");
            fail("Did not throw IllegalArgumentException when trying to match '10.0.2'");
        } catch (IllegalArgumentException e) {
        }
        
        try {
            matcher.matches("10.0.0.1024");
            fail("Did not throw IllegalArgumentException when trying to match '10.0.0.1024'");
        } catch (IllegalArgumentException e) {
        }
        
        try {
            matcher.matches("1.2.3.4.5");
            fail("Did not throw IllegalArgumentException when trying to match '1.2.3.4.5'");
        } catch (IllegalArgumentException e) {
        }

        try {
            matcher.matches("..");
            fail("Did not throw IllegalArgumentException when trying to match '..'");
        } catch (IllegalArgumentException e) {
        }
    }
    
    public void testConstructionWithInvalidValues() {

        try {
            String network = "10.";
            String mask = "255.255.254.0";
            new IPv4SubnetMatcher(network, mask);

            fail("Did not throw IllegalArgumentException when trying to construct a matcher using ('" 
                    + network + "', '" + mask + "')");
        } catch (IllegalArgumentException e) {}

        try {
            String network = "1.2.3.4";
            String mask = "1024.255.254.0";
            new IPv4SubnetMatcher(network, mask);
            
            fail("Did not throw IllegalArgumentException when trying to construct a matcher using ('" 
                    + network + "', '" + mask + "')");
        } catch (IllegalArgumentException e) {}

        try {
            String network = "10.0.0.-1";
            String mask = "255.255.254.0";
            new IPv4SubnetMatcher(network, mask);
            fail("Did not throw IllegalArgumentException when trying to construct a matcher using ('" 
                    + network + "', '" + mask + "')");
        } catch (IllegalArgumentException e) {}

        // Testing illegal subnet masks (but otherwise correct dotted-decimal)
        try {

            String network = "10.0.0.2";
            String mask = "254.255.255.0";
            new IPv4SubnetMatcher(network, mask);
            fail("Did not throw IllegalArgumentException when trying to construct a matcher using ('" 
                    + network + "', '" + mask + "')");
        } catch (IllegalArgumentException e) {}

        try {
            String network = "10.0.0.2";
            String mask = "0.255.255.0";
            new IPv4SubnetMatcher(network, mask);
            fail("Did not throw IllegalArgumentException when trying to construct a matcher using ('" 
                    + network + "', '" + mask + "')");
        } catch (IllegalArgumentException e) {}
                
        try {
            String network = "10.0.0.2";
            String mask = "0.0.255.0";
            new IPv4SubnetMatcher(network, mask);
            fail("Did not throw IllegalArgumentException when trying to construct a matcher using ('" 
                    + network + "', '" + mask + "')");
        } catch (IllegalArgumentException e) {}

        try {
            String network = "10.0.0.2";
            String mask = "0.0.0.255";
            new IPv4SubnetMatcher(network, mask);
            fail("Did not throw IllegalArgumentException when trying to construct a matcher using ('" 
                    + network + "', '" + mask + "')");
        } catch (IllegalArgumentException e) {}
        
        try {
            String network = "10.0.0.2";
            String mask = "0.0.0.-1";
            new IPv4SubnetMatcher(network, mask);

            fail("Did not throw IllegalArgumentException when trying to construct a matcher using ('" 
                    + network + "', '" + mask + "')");
        } catch (IllegalArgumentException e) {}
        
        try {
            String network = "10.0.0.2";
            String mask = "255.128.0.1";
            new IPv4SubnetMatcher(network, mask);

            fail("Did not throw IllegalArgumentException when trying to construct a matcher using ('" 
                    + network + "', '" + mask + "')");
        } catch (IllegalArgumentException e) {}

    }
    
}
