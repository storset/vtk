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


import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.vortikal.util.text.TextUtils;


/**
 * A utility class for computing MD5 checksums and returning the
 * result in a hex string representation.
 */
public class MD5 {

    /**
     * Computes the MD5 sum of a string.
     * @param str the input
     * @return the hex representation of the MD5 sum
     */
    public static String md5sum(String str) {
        if (str == null) {
            throw new IllegalArgumentException("Argument is NULL");
        }
        MessageDigest md = instance();
        byte[] digest = md.digest(str.getBytes());
        char[] result = TextUtils.toHex(digest);
        return new String(result);
    }
    
    /**
     * Computes the MD5 sum of a byte buffer. 
     * @param bytes the byte buffer
     * @return a string containing the hex representation of the MD5 sum
     * @throws IOException if an I/O error occurs
     */
    public static String md5sum(byte[] bytes) {
        MessageDigest md = instance();
        byte[] digest = md.digest(bytes);
        char[] result = TextUtils.toHex(digest);
        return new String(result);
    }

    /**
     * Computes the MD5 sum of an input stream. 
     * (Does not close the stream afterwards.)
     * @param in the input stream
     * @return a string containing the hex representation of the MD5 sum
     * @throws IOException if an I/O error occurs
     */
    public static String md5sum(InputStream in) throws IOException {
        if (in == null) {
            throw new IllegalArgumentException("Argument is NULL");
        }
        MessageDigest md = instance();
        byte[] buffer = new byte[1024];
        int n = 0;
        while ((n = in.read(buffer)) > 0) {
            md.update(buffer, 0, n);
        }
        byte[] digest = md.digest();
        char[] result = TextUtils.toHex(digest);
        return new String(result);
    }
    

    /**
     * Creates a wrapper around an input stream that computes the MD5 sum 
     * while the stream is being read.
     * @param in the input stream
     * @return a string containing the hex representation of the MD5 sum
     * @throws IOException if an I/O error occurs
     */
    public static MD5InputStream wrap(InputStream in) {
        MessageDigest md = instance();
        return new MD5InputStream(in, md);
    }
    
    public static final class MD5InputStream extends DigestInputStream {
        private MessageDigest md = null;
        private MD5InputStream(InputStream in, MessageDigest md) {
            super(in, md);
            this.md = md;
        }
        public String md5sum() {
            byte[] digest = this.md.digest();
            char[] result = TextUtils.toHex(digest);
            return new String(result);
        }
    }
    
    private static MessageDigest instance() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 digest not available in JVM");
        }
    }
}
