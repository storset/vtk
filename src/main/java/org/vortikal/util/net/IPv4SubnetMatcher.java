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
 * Match IPv4 addresses in dotted-decimal notation against a given subnet.
 * (I.e. test if a given IPv4 address belongs to the subnet specified when
 * creating the matcher instance).
 * 
 * @author oyviste
 */
public class IPv4SubnetMatcher extends AbstractIPv4Matcher implements IPv4Matcher {

    private final int[] networkOctets ;
    private final int[] maskOctets;
    
    /**
     * Construct an IPv4 subnet matcher with the given network address and mask.
     * 
     * @param networkAddress
     * @param mask
     */
    public IPv4SubnetMatcher(String networkAddress, String mask) {
        int[] addressOctets = parseDottedDecimalNumber(networkAddress);
        
        int[] maskOctets = parseDottedDecimalNumber(mask);
        if (! isValidMask(maskOctets)) {
            throw new IllegalArgumentException("Invalid IPv4 address mask: '" 
                                                                    + mask + "'");
        }
        
        int[] networkOctets = new int[4];
        for (int i=0; i<4; i++){
            networkOctets[i] = (addressOctets[i] & maskOctets[i]);
        }
        
        this.maskOctets = maskOctets;
        this.networkOctets = networkOctets;
    }
    
    /**
     * Construct an IPv4 subnet matcher with the given network address
     * and number of subnet bits (useful when dealing with CIDR notation).
     * 
     * @param networkAddress
     * @param subnetBits
     */
    public IPv4SubnetMatcher(String networkAddress, int subnetBits) {
        if (subnetBits < 0 || subnetBits > 32) {
            throw new IllegalArgumentException("Invalid number of subnet bits (must be between 0 and 32): " 
                    + subnetBits);
        }

        int mask = 0;
        for (int i=0; i<subnetBits; i++) {
            mask |= (0x1 << (31-i));
        }

        int[] maskOctets = new int[4];
        for (int i=3; i>=0; i--) {
            maskOctets[3-i] = (mask >>> (8*i)) & 0xFF;
        }

        int[] addressOctets = parseDottedDecimalNumber(networkAddress);
        int[] networkOctets = new int[4];
        for (int i=0; i<4; i++){
            networkOctets[i] = (addressOctets[i] & maskOctets[i]);
        }
        
        this.maskOctets = maskOctets;
        this.networkOctets = networkOctets;
        
    }
    
    /**
     * Test if an IPv4 address matches the subnet (i.e. if the IP is on the subnet
     * specified when constructing the matcher).
     */
    public boolean matches(String ipv4Address) {
        
        int[] octets = parseDottedDecimalNumber(ipv4Address);
        if (octets.length != 4) {
            throw new IllegalArgumentException("An IPv4 address must consist of exactly 4 octets");
        }
        
        for (int i=0; i<4; i++) {
            int octet = (this.maskOctets[i] & octets[i]);
            
            if (octet != this.networkOctets[i]) {
                return false;
            }
        }
        
        return true;
    }
    
    
    /**
     * Validates a subnet mask. An IPv4 subnet mask can have only leading 1's and
     * trailing 0's with no mixing. It must be 32 bits wide (4 octets).
     * 
     * @param maskOctets
     * @return
     */
    private final boolean isValidMask(int[] maskOctets) {
        
        if (maskOctets.length != 4) return false;
        
        boolean zeroExpected = false;
        for (int i=0; i<4; i++) {
            int octet = maskOctets[i];
            if (zeroExpected) {
                if (octet != 0x00) return false;
                else
                    continue;
            }
            
            switch(octet) {
            
            case(0xFE):
            case(0xFC):
            case(0xF8):
            case(0xF0):
            case(0xE0):
            case(0xC0):
            case(0x80):
            case(0x00):
                zeroExpected = true;
                break;
            
            case(0xFF): 
                break;
            
            default:
                return false;
            }
        }
        
        return true;
    }
    
    public String toString() {
        StringBuffer buffer = new StringBuffer("IPv4SubnetMatcher[");
        
        for (int i=0; i<4; i++) {
            buffer.append(this.networkOctets[i]);
            if (i < 3) buffer.append('.');
        }
        
        buffer.append("/");
        
        for (int i=0; i<4; i++){
            buffer.append(this.maskOctets[i]);
            if (i < 3) buffer.append('.');
        }
        
        buffer.append(']');
        return buffer.toString();
    }
    

}
