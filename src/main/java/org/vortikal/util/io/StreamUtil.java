/* Copyright (c) 2004,2010 University of Oslo, Norway
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
package org.vortikal.util.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class StreamUtil {

    private static final Log LOGGER = LogFactory.getLog(StreamUtil.class);

    /**
     * Reads an input stream into a byte array. Closes the stream
     * after the data has been read.
     *
     * Buffer allocations are kept to a minimum by using multiple read buffers as
     * needed and avoiding buffer copy+throw-away when growing. This provides better
     * performance mainly because of reduced copying and allocations. It creates
     * less garbage than the traditional ByteArrayOutputStream approach.
     *
     * @param content an <code>InputStream</code> value
     * @return a <code>byte[]</code> containg the read data.
     *         Length is exactly the number of bytes read from the input stream.
     * @exception IOException if an error occurs
     */
    public static byte[] readInputStream(InputStream content)
        throws IOException {
        try {
            byte[][] buffers = new byte[10][];
            byte[] currentbuf = new byte[8192];
            buffers[0] = currentbuf;
            int n, pos = 0, total = 0, bufcount = 1;
            while ((n = content.read(currentbuf, pos, currentbuf.length - pos)) > 0) {
                pos += n;
                total += n;
                if (pos == currentbuf.length) {
                    if (bufcount == buffers.length) {
                        // Allocate for more buffers
                        buffers = Arrays.copyOf(buffers, buffers.length << 1);
                    }
                    byte[] newbuffer = new byte[currentbuf.length << 1]; // Double size of next buffer
                    buffers[bufcount++] = newbuffer;
                    currentbuf = newbuffer;
                    pos = 0;
                }
            }

            // Assemble allocated buffers to single properly sized return value
            final byte[] returnbuf = new byte[total];
            int remaining = total;
            for (int i=0; i<bufcount; i++) {
                byte[] buf = buffers[i];
                int copycount = Math.min(buf.length, remaining);
                System.arraycopy(buf, 0, returnbuf, total - remaining, copycount);
                remaining -= copycount;
            }

            return returnbuf;
        } finally {
            try {
                content.close();
            } catch (IOException e) {
                LOGGER.warn("Error closing input stream", e);
            }
        }
    }

    /**
     * Reads the first N bytes of an input stream into a byte
     * array. Closes the stream after the data has been read. No more than
     * <code>maxLength</code> bytes will be read from the input stream.
     *
     * @param content an <code>InputStream</code> value
     * @param maxLength the maximum number of bytes to read from the
     * stream.
     * @return a <code>byte[]</code> array. The length of the byte
     * array will be less than or equal to the <code>maxLength</code>
     * argument.
     * @exception IOException if an error occurs
     */
    public static byte[] readInputStream(InputStream content, int maxLength)
        throws IOException {
        try {
            if (maxLength <= 0) return new byte[0];

            byte[][] buffers = new byte[10][];
            byte[] currentbuf = new byte[8192];
            buffers[0] = currentbuf;
            int n, pos = 0, total = 0, bufcount = 1;
            int chunksize = Math.min(currentbuf.length, maxLength);
            while ((n = content.read(currentbuf, pos, chunksize)) > 0) {
                pos += n;
                total += n;

                if (total >= maxLength) {
                    total = maxLength;
                    break;
                }

                if (pos == currentbuf.length) {
                    if (bufcount == buffers.length) {
                        // Allocate for more buffers
                        buffers = Arrays.copyOf(buffers, buffers.length << 1);
                    }
                    byte[] newbuffer = new byte[currentbuf.length << 1];
                    buffers[bufcount++] = newbuffer;
                    currentbuf = newbuffer;
                    pos = 0;
                    chunksize = currentbuf.length;
                }

                if (chunksize + pos > currentbuf.length) {
                    // Truncate next read chunk to fit current buffer
                    chunksize = currentbuf.length - pos;
                }

                // Truncate next read chunk to remaining bytes if we're nearing maxLength
                chunksize = Math.min(chunksize, maxLength - total);
            }

            // Assemble allocated buffers to single properly sized return value
            final byte[] returnbuf = new byte[total];
            int remaining = total;
            for (int i=0; i<bufcount; i++) {
                byte[] buf = buffers[i];
                int copycount = Math.min(buf.length, remaining);
                System.arraycopy(buf, 0, returnbuf, total - remaining, copycount);
                remaining -= copycount;
            }

            return returnbuf;
        } finally {
            try {
                content.close();
            } catch (IOException e) {
                LOGGER.warn("Error closing input stream", e);
            }
        }
    }

    /**
     * Buffered copy using a default buffer size. The input stream is closed,
     * while the output stream remains open after completion.
     * 
     * @see #pipe(java.io.InputStream, java.io.OutputStream, int)
     */
    public static long pipe(InputStream in, OutputStream out)
        throws IOException {
        return pipe(in, out, 4096, false);
    }

    /**
     * Like {@link #pipe(InputStream, OutputStream, int, boolean)}, but start copying 
     * at an offset and limit to a provided number of bytes.
     * @param in the stream to copy
     * @param out the destination
     * @param offset the position to start copying from
     * @param limit the maximum number of bytes to copy
     * @param bufferSize the buffer size
     * @param closeOutput whether to close output stream after copying
     * @return
     * @throws IOException
     */
    public static long pipe(InputStream in, OutputStream out, long offset, long limit,
            final int bufferSize, final boolean closeOutput) throws IOException {
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("Buffer size must be > 0");
        }
        if (offset < 0 ) {
            throw new IllegalArgumentException("Offset must be >= 0");
        }
        if (limit <= 0 ) {
            throw new IllegalArgumentException("Limit must be > 0");
        }

        try {
            byte[] buffer = new byte[bufferSize];
            if (offset > 0) {
                long skipped = in.skip(offset);
                if (skipped != offset) {
                    throw new IOException("Unable to skip to offset: " + offset);
                }
            }
            long count = 0;
            int n;
            while ((n = in.read(buffer, 0, buffer.length)) > 0) {
                if (count + n >= limit) {
                    n = (int) (limit - count);
                    out.write(buffer, 0, n);
                    count += n;
                    break;
                }
                out.write(buffer, 0, n);
                count += n;
            }
            return count;
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                LOGGER.warn("Error closing input stream", e);
            }
            if (closeOutput) {
                try {
                    out.close();
                } catch (IOException e) {
                    LOGGER.warn("Error closing output stream", e);
                }
            }
        }
        
    }
    
    /**
     * Buffered copy of <em>all</em> available data from an input stream to an output stream.
     * The operation stops when either of the following conditions occur:
     * 1. No more data can be read from the input stream (EOF).
     * 2. An exception occurs, either when reading the input stream or writing to the
     *    output stream.
     *
     * The input stream is always closed upon completion or exception.
     * The output stream can be optionally closed under the same conditions
     * if argument closeOutput is true.
     *
     * @param in
     * @param bufferSize
     * @param out
     * @return The total number of bytes piped from the input stream to the output stream.
     * @throws IOException
     */
    public static long pipe(InputStream in, OutputStream out, final int bufferSize, final boolean closeOutput)
        throws IOException {

        if (bufferSize <= 0) {
            throw new IllegalArgumentException("Buffer size must be > 0");
        }

        try {
            byte[] buffer = new byte[bufferSize];
            long count = 0;
            int n;
            while ((n = in.read(buffer, 0, buffer.length)) > 0) {
                out.write(buffer, 0, n);
                count += n;
            }
            return count;
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                LOGGER.warn("Error closing input stream", e);
            }
            if (closeOutput) {
                try {
                    out.close();
                } catch (IOException e) {
                    LOGGER.warn("Error closing output stream", e);
                }
            }
        }
    }

    /**
     * Dump all data in buffer to output stream. Optionally close output stream
     * when all data has been written.
     *
     * @param data
     * @param out
     * @param closeOutput
     * @throws IOException
     */
    public static void dump(byte[] data, OutputStream out, boolean closeOutput)
        throws IOException {
        try {
            out.write(data, 0, data.length);
        } finally {
            if (closeOutput) {
                try {
                    out.close();
                } catch (IOException io) {
                    LOGGER.warn("Error closing output stream", io);
                }
            }
        }
    }


    /**
     * Make an input stream from the given string using the given
     * character encoding.
     * @param s
     * @param encoding
     * @return
     * @throws IOException
     */
    public static InputStream stringToStream(String s, String encoding) throws IOException {
        return new ByteArrayInputStream(s.getBytes(encoding));
    }

    /**
     * Make in input stream from the given string using the system defeault
     * character encoding.
     * @param s
     * @return
     * @throws IOException
     */
    public static InputStream stringToStream(String s) throws IOException {
        return stringToStream(s, System.getProperty("file.encoding"));
    }

    /**
     * Read input stream and return a <code>String</code> using the given
     * character encoding.
     *
     * The input stream is closed.
     * 
     * @param in
     * @param encoding
     * @return
     * @throws IOException
     */
    public static String streamToString(InputStream in, String encoding) throws IOException {
        return new String(readInputStream(in), encoding);
    }
    
    /**
     * Read input stream and return a <code>String</code> using the system
     * default character encoding.
     *
     * The input stream is closed.
     *
     * @param in
     * @param encoding
     * @return
     * @throws IOException
     */
    public static String streamToString(InputStream in) throws IOException {
        return streamToString(in, System.getProperty("file.encoding"));
    }

    // Old impls of readInputStream methods kept for reference:
//    /**
//     * Reads an input stream into a byte array. Closes the stream
//     * after the data has been read.
//     *
//     * @param content an <code>InputStream</code> value
//     * @return a <code>byte[]</code> containg the read data.
//     *         Length is exactly the number of bytes read from the input stream.
//     * @exception IOException if an error occurs
//     */
//    public static byte[] readInputStreamOLD(InputStream content)
//            throws IOException {
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//
//        try {
//            byte[] buffer = new byte[10000];
//            int n;
//            while ((n = content.read(buffer)) > 0) {
//                out.write(buffer, 0, n);
//            }
//            return out.toByteArray();
//        } finally {
//            try {
//                content.close();
//            } catch (IOException e) {
//                LOGGER.warn("Error closing input stream", e);
//            }
//        }
//    }
//
//    /**
//     * Reads the first N bytes of an input stream into a byte
//     * array. Closes the stream after the data has been read.
//     *
//     * @param content an <code>InputStream</code> value
//     * @param maxLength the maximum number of bytes to read from the
//     * stream.
//     * @return a <code>byte[]</code> array. The length of the byte
//     * array will be less than or equal to the <code>maxLength</code>
//     * argument.
//     * @exception IOException if an error occurs
//     */
//    public static byte[] readInputStreamOLD(InputStream content, int maxLength)
//            throws IOException {
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//
//        try {
//            byte[] buffer = new byte[10000];
//            int n;
//            int total = 0;
//            while ((n = content.read(buffer)) > 0 && total < maxLength) {
//
//                if (n + total > maxLength) {
//                    n = maxLength - total;
//                }
//                out.write(buffer, 0, n);
//                total += n;
//            }
//            return out.toByteArray();
//        } finally {
//            try {
//                content.close();
//            } catch (IOException e) {
//                LOGGER.warn("Error closing input stream", e);
//            }
//        }
//    }
    
}
