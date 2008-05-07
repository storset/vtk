/* Copyright (c) 2005, 2008, University of Oslo, Norway
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
package org.vortikal.util.codec;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 * A utility class for computing MD5 checksums and returning the
 * result in a hex string representation.
 */
public class MD5 {
    
    private static final char[] HEX = 
                              {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                               'a', 'b', 'c', 'd', 'e', 'f'};

    public static String md5sum(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");

            byte[] digest = md.digest(str.getBytes());
            char[] result = new char[32];
            for (int i = 0; i < 16; i++) {
                result[i << 1] = HEX[(digest[i] & 0xF0) >>> 4];
                result[(i << 1)+1] = HEX[digest[i] & 0x0F];
            }
            
            return new String(result);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                "MD5 digest not available in JVM");
        }
    }
    
}
