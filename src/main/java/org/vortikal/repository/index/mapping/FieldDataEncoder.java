/* Copyright (c) 2006, University of Oslo, Norway
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
package org.vortikal.repository.index.mapping;

import java.math.BigInteger;

/**
 * Low-level index field value encoder/decoder for some single-value data types.
 * Encodes/decodes to/from lexicographically sortable string representations and
 * pure binary representations (for efficient storage and re-creation).
 * 
 * @author oyviste
 * 
 */
public final class FieldDataEncoder {

    private static final char[] HEX_CHARS = 
                            { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                              'a', 'b', 'c', 'd', 'e', 'f' };

    private static final int BYTE_MASK = 0xFF;

    private FieldDataEncoder() {
    } // Util


    /**
     * TODO: javadoc
     * 
     * @param dateValue
     * @return
     */
    public static String encodeDateValueToString(long dateValue) {
        // Since we're using a resolution of one second, this case can be
        // optimized.
        // Avoid using Lucene's DateTools class (uses synchronized
        // SimpleDateFormat
        // and Calendar internally to support all kinds of other resolutions).
        return encodeLongToString(dateValue - (dateValue % 1000));
    }


    /**
     * TODO: javadoc
     * 
     * @param encodedDateValue
     * @return
     */
    public static long decodeDateValueFromString(String encodedDateValue)
            throws FieldDataEncodingException {

        return decodeLongFromString(encodedDateValue);
    }


    /**
     * Encode a (signed) <coe>int</code> into a hexadecimal unsigned
     * <code>String</code> representation suitable for indexing and
     * lexicographical sorting.
     * 
     * @param i
     *            number to encode
     * @return
     */
    public static String encodeIntegerToString(int i) {
        long uint = i + 0x80000000L;
        char[] hex = Long.toHexString(uint).toCharArray();

        // Apply zero-padding, essential for
        // keeping lexicographic sorting equal to numeric sorting
        char[] output = { '0', '0', '0', '0', '0', '0', '0', '0' };
        System.arraycopy(hex, 0, output, 8 - hex.length, hex.length);
        return String.valueOf(output);
    }


    /**
     * TODO: javadoc
     * 
     * @param encodedInteger
     * @return
     */
    public static int decodeIntegerFromString(String encodedInteger)
            throws FieldDataEncodingException {
        try {
            long uint = Long.parseLong(encodedInteger, 16);
            return (int) (uint - 0x80000000L);
        } catch (NumberFormatException nfe) {
            throw new FieldDataEncodingException(nfe.getMessage());
        }
    }


    /**
     * Encode a (signed) <coe>long</code> integer into a hexadecimal unsigned
     * <code>String</code> representation suitable for indexing and
     * lexicographical sorting.
     * 
     * @param l
     *            number to encode
     * @return
     */
    public static String encodeLongToString(long l) {
        // Create 64 bits unsigned long representation for lexicographical
        // sorting
        byte[] ulong = new byte[8];

        if (l < 0) {
            l += Long.MAX_VALUE + 1; // Add using Java arithmetic (no overflow)
        } else {
            ulong[0] |= 0x80; // Add using bit-ops (avoid overflow)
        }

        for (int i = 0; i < 8; i++) {
            ulong[i] |= (byte) ((l >>> (56 - i * 8)) & BYTE_MASK);
        }

        // Apply zero-padding, essential for
        // keeping lexicographic sorting equal to numeric sorting
        return unsignedLongToPaddedHexString(ulong);
    }


    /**
     * TODO: javadoc
     * 
     * @param encodedLong
     * @return
     */
    public static long decodeLongFromString(String encodedLong) throws FieldDataEncodingException {

        try {
            BigInteger ulong = new BigInteger(encodedLong, 16);
            return ulong.subtract(BigInteger.valueOf(Long.MAX_VALUE)).subtract(BigInteger.ONE)
                    .longValue();
        } catch (NumberFormatException nfe) {
            throw new FieldDataEncodingException(nfe.getMessage());
        }
    }


    /**
     * 
     * @param b
     * @return
     */
    public static byte[] encodeBooleanToBinary(boolean b) {
        return b ? new byte[] { 1 } : new byte[] { 0 };
    }


    /**
     * 
     * @param value
     * @return
     */
    public static boolean decodeBooleanFromBinary(byte[] value, int offset, int length) {
        if (length != 1) {
            throw new IllegalArgumentException("Encoded booleans must be exactly one byte");
        }

        return (value[offset] == 0 ? false : true);
    }


    /**
     * 
     * @param dateValue
     * @return
     */
    public static byte[] encodeDateValueToBinary(long dateValue) {
        return encodeLongToBinary(dateValue);
    }


    /**
     * 
     * @param value
     * @return
     */
    public static long decodeDateValueFromBinary(byte[] value, int offset, int length) {
        return decodeLongFromBinary(value, offset, length);
    }


    /**
     * 
     * @param n
     * @return
     */
    public static byte[] encodeLongToBinary(long n) {
        byte[] value = new byte[8];
        for (int i = 7; i != -1; i--) {
            value[i] = (byte) ((n >>> i * 8) & BYTE_MASK);
        }

        return value;
    }


    /**
     * 
     * @param value
     * @return
     */
    public static long decodeLongFromBinary(byte[] value, int offset, int length) {
        if (length != 8) {
            throw new IllegalArgumentException("Byte array of length 8 is "
                    + "required to decode to a long");
        }

        long n = 0L;
        for (int i = offset + 7; i != offset-1; i--) {
            n |= ((long) value[i] & BYTE_MASK) << i * 8;
        }

        return n;
    }


    /**
     * Encodes integer to binary representation for efficient storage in index.
     * 
     * @param n
     * @return
     */
    public static byte[] encodeIntegerToBinary(int n) {
        byte[] value = new byte[4];
        for (int i = 3; i != -1; i--) {
            value[i] = (byte) ((n >>> i * 8) & BYTE_MASK);
        }

        return value;
    }


    /**
     * Decodes integer from binary representation encoded by
     * {@link #encodeIntegerToBinary(int)}
     * 
     * @param value
     *            A byte array of length 4 containing the represenation
     * @return
     */
    public static int decodeIntegerFromBinary(byte[] value, int offset, int length) {
        if (length != 4) {
            throw new IllegalArgumentException("Integers must be of length 4");
        }

        int n = 0;
        for (int i = offset+3; i != offset-1; i--) {
            n |= (value[i] & BYTE_MASK) << i * 8;
        }

        return n;
    }


    /**
     * TODO: javadoc
     * 
     * @param ulong
     * @return
     */
    private static String unsignedLongToPaddedHexString(byte[] ulong) {
        if (ulong.length != 8) {
            throw new IllegalArgumentException(
                    "Unsigned long integers must be represented by exactly 8 bytes.");
        }

        char[] output = { '0', '0', '0', '0', '0', '0', '0', '0',
                          '0', '0', '0', '0', '0', '0', '0', '0' };

        // Dump hex characters
        byte ch = 0x00;
        for (int i = 0; i < 8; i++) {
            ch = (byte) ((ulong[i] >>> 4) & 0x0F);
            output[2 * i] = HEX_CHARS[ch];
            ch = (byte) (ulong[i] & 0x0F);
            output[2 * i + 1] = HEX_CHARS[ch];
        }

        return String.valueOf(output);
    }

}