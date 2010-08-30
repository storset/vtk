/* Copyright (c) 2010, University of Oslo, Norway
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

/**
 * Simple XOR string encoder/decoder.
 *
 * This class does not provide real security unless the key used is truly random
 * and of at least the same length as the data being encoded, AND that the value
 * being encoded is not possible to guess.
 *
 * Preferably key should be longer than, or at least as long as the data to be encoded
 * using the key (key must be one byte at minimum).
 *
 * Note again that the key can easily be derived from the encoded data if the value
 * that the encoded data represents is known or can be guessed by third party.
 *
 * Useful mostly for obfuscation purposes. Don't use this class if real security
 * is required.
 */
public class XOREncoder {

    /**
     * XOR encodes the given string value using the given key, then
     * returns a base64-encoded version of the data, as a new string.
     *
     * @param value The String value to encode.
     * @param key The key to use for XOR encoding.
     * @return Obfuscated/encoded string value, base-64-encoded.
     * @throws NullPointerException if value or key is null.
     */
    public static String encode(String value, String key) {
        if (key.length() < 1) {
            throw new IllegalArgumentException("Key needs to be at least one character");
        }

        byte[] keyBytes = key.getBytes();

        byte[] bytes = value.getBytes();
        for (int i=0; i<bytes.length; i++) {
            bytes[i] = (byte)((bytes[i] ^ keyBytes[i % keyBytes.length]) & 0xFF);
        }

        // Base64 encode
        org.apache.commons.codec.binary.Base64 encoder =
            new org.apache.commons.codec.binary.Base64();

        return new String(encoder.encode(bytes));
    }

    /**
     * Returns original string of value encoded by
     * {@link #encode(java.lang.String, java.lang.String)}.
     *
     * Key must of course be the same used in encode(), otherwise gibberish will
     * be returned.
     *
     * @param encodedValue An encoded value
     * @param key The key that was use to encode the value.
     * @return Decoded value.
     * @throws NullPointerException if encoded value or key is null.
     */
    public static String decode(String encodedValue, String key) {
        if (key.length() < 1) {
            throw new IllegalArgumentException("Key needs to be at least one character");
        }

        byte[] keyBytes = key.getBytes();

        // Base64 decode
        org.apache.commons.codec.binary.Base64 decoder =
            new org.apache.commons.codec.binary.Base64();

        byte[] bytes = decoder.decode(encodedValue.getBytes());

        for (int i=0; i<bytes.length; i++) {
            bytes[i] = (byte)((bytes[i] ^ keyBytes[i % keyBytes.length]) & 0xFF);
        }

        return new String(bytes);
    }
}
