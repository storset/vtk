/* Copyright (c) 2010 University of Oslo, Norway
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Thorough test-case for StreamUtil class.
 */
public class StreamUtilTest {

    public StreamUtilTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of pipe method, of class StreamUtil.
     */
    @Test
    public void testPipe_InputStream_OutputStream() throws Exception {
        String s = "data to copy";
        int len = s.getBytes().length;
        InputStream in = new ByteArrayInputStream(s.getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long piped = StreamUtil.pipe(in, out);
        String result = new String(out.toByteArray());
        assertEquals(s, result);
        assertEquals(len, piped);

        byte[] data = generateRandomDataBuffer(30000);
        in = new ByteArrayInputStream(data);
        out = new ByteArrayOutputStream();
        piped = StreamUtil.pipe(in, out);
        byte[] resultData = out.toByteArray();
        assertTrue(buffersEqual(data, resultData));
        assertEquals(data.length, piped);
        
    }

    @Test
    public void testDump_3args() throws Exception {
        CheckForCloseByteArrayOutputStream out = new CheckForCloseByteArrayOutputStream();
        byte[] data = generateRandomDataBuffer(10000);

        StreamUtil.dump(data, out, true);
        assertTrue(buffersEqual(data, out.toByteArray()));
        assertTrue(out.isClosed());

        out = new CheckForCloseByteArrayOutputStream();
        data = "foobar".getBytes();
        StreamUtil.dump(data, out, false);
        assertTrue(buffersEqual(data, out.toByteArray()));
        assertFalse(out.isClosed());
    }

    /**
     * Test of pipe method, of class StreamUtil.
     */
    @Test
    public void testPipe_3args() throws Exception {
        String s = "data to copy";
        int len = s.getBytes().length;
        InputStream in = new ByteArrayInputStream(s.getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long piped = StreamUtil.pipe(in, out, 1, true);
        String result = new String(out.toByteArray());
        assertEquals(s, result);
        assertEquals(len, piped);

        byte[] data = generateRandomDataBuffer(10000);
        in = new ByteArrayInputStream(data);
        out = new ByteArrayOutputStream();
        piped = StreamUtil.pipe(in, out, 10, true);
        byte[] resultData = out.toByteArray();
        assertTrue(buffersEqual(data, resultData));
        assertEquals(data.length, piped);

        data = generateRandomDataBuffer(5000);
        in = new SmallChunkInputStream(data);
        out = new CheckForCloseByteArrayOutputStream();
        piped = StreamUtil.pipe(in, out, 64, true);
        resultData = out.toByteArray();
        assertTrue(buffersEqual(data, resultData));
        assertTrue(((SmallChunkInputStream)in).isClosed());
        assertTrue(((CheckForCloseByteArrayOutputStream)out).isClosed());
        assertEquals(data.length, piped);

        data = generateRandomDataBuffer(5000);
        in = new SmallChunkInputStream(data);
        out = new CheckForCloseByteArrayOutputStream();
        piped = StreamUtil.pipe(in, out, 64, false);
        resultData = out.toByteArray();
        assertTrue(buffersEqual(data, resultData));
        assertTrue(((SmallChunkInputStream)in).isClosed());
        assertFalse(((CheckForCloseByteArrayOutputStream)out).isClosed());
        assertEquals(data.length, piped);
    }

    @Test
    public void testPipe_5args() throws Exception {
        String s = "data to copy";
        InputStream in = new ByteArrayInputStream(s.getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long piped = StreamUtil.pipe(in, out, 2L, 5L, 1, true);
        String result = new String(out.toByteArray());
        assertEquals("ta to", result);
        assertEquals(5L, piped);
    }

    /**
     * Test of readInputStream method, of class StreamUtil.
     */
    @Test
    public void testReadInputStream_InputStream() throws Exception {
        byte[] data = new byte[0];
        ByteArrayInputStream content = new ByteArrayInputStream(data);
        byte[] result = StreamUtil.readInputStream(content);
        assertEquals(-1, content.read());
        assertTrue(buffersEqual(data, result));

        data = generateRandomDataBuffer(1);
        content = new ByteArrayInputStream(data);
        result = StreamUtil.readInputStream(content);
        assertEquals(-1, content.read());
        assertTrue(buffersEqual(data, result));

        data = generateRandomDataBuffer(2);
        content = new ByteArrayInputStream(data);
        result = StreamUtil.readInputStream(content);
        assertEquals(-1, content.read());
        assertTrue(buffersEqual(data, result));

        data = generateRandomDataBuffer(1000);
        content = new ByteArrayInputStream(data);
        result = StreamUtil.readInputStream(content);
        assertEquals(-1, content.read());
        assertTrue(buffersEqual(data, result));

        data = generateRandomDataBuffer(8191);
        content = new ByteArrayInputStream(data);
        result = StreamUtil.readInputStream(content);
        assertEquals(-1, content.read());
        assertTrue(buffersEqual(data, result));

        data = generateRandomDataBuffer(12288);
        content = new ByteArrayInputStream(data);
        result = StreamUtil.readInputStream(content);
        assertEquals(-1, content.read());
        assertTrue(buffersEqual(data, result));

        data = generateRandomDataBuffer(100000);
        content = new ByteArrayInputStream(data);
        result = StreamUtil.readInputStream(content);
        assertEquals(-1, content.read());
        assertTrue(buffersEqual(data, result));

        data = generateRandomDataBuffer(30001);
        content = new SmallChunkInputStream(data);
        result = StreamUtil.readInputStream(content);
        assertEquals(-1, content.read());
        assertTrue(((SmallChunkInputStream)content).isClosed());
        assertTrue(buffersEqual(data, result));

        data = generateRandomDataBuffer(1000);
        content = new SmallChunkInputStream(data);
        result = StreamUtil.readInputStream(content);
        assertTrue(((SmallChunkInputStream)content).isClosed());
        assertEquals(-1, content.read());
        assertTrue(buffersEqual(data, result));
    }


    /**
     * Test of readInputStream method of class StreamUtil.
     * With maxLength.
     */
    @Test
    public void testReadInputStream_InputStream_int() throws Exception {
        byte[] data = new byte[0];
        ByteArrayInputStream content = new ByteArrayInputStream(data);
        byte[] result = StreamUtil.readInputStream(content, 0);
        assertEquals(-1, content.read());
        assertTrue(buffersEqual(data, result));

        data = generateRandomDataBuffer(1);
        content = new ByteArrayInputStream(data);
        result = StreamUtil.readInputStream(content, 100);
        assertEquals(-1, content.read());
        assertTrue(buffersEqual(data, result));

        data = generateRandomDataBuffer(1);
        content = new ByteArrayInputStream(data);
        result = StreamUtil.readInputStream(content, -1);
        assertTrue(content.read() != -1);
        assertEquals(0, result.length);

        data = generateRandomDataBuffer(2);
        content = new ByteArrayInputStream(data);
        result = StreamUtil.readInputStream(content, 100);
        assertEquals(-1, content.read());
        assertTrue(buffersEqual(data, result));

        data = generateRandomDataBuffer(1000);
        content = new ByteArrayInputStream(data);
        result = StreamUtil.readInputStream(content, 8192);
        assertEquals(-1, content.read());
        assertTrue(buffersEqual(data, result));

        data = generateRandomDataBuffer(8191);
        content = new ByteArrayInputStream(data);
        result = StreamUtil.readInputStream(content, 8192);
        assertEquals(-1, content.read());
        assertTrue(buffersEqual(data, result));

        data = generateRandomDataBuffer(8192);
        content = new ByteArrayInputStream(data);
        result = StreamUtil.readInputStream(content, 8192);
        assertEquals(-1, content.read());
        assertTrue(buffersEqual(data, result));

        data = generateRandomDataBuffer(100000);
        content = new ByteArrayInputStream(data);
        result = StreamUtil.readInputStream(content, 1224);
        assertEquals(1224, result.length);
        assertEquals(100000 - 1224, content.available());
        assertTrue(buffersEqual(Arrays.copyOf(data, 1224), result));

        data = generateRandomDataBuffer(1024);
        content = new ByteArrayInputStream(data);
        result = StreamUtil.readInputStream(content, 1);
        assertEquals(1, result.length);
        assertEquals(1023, content.available());
        assertTrue(buffersEqual(Arrays.copyOf(data, 1), result));

        data = generateRandomDataBuffer(100000);
        content = new ByteArrayInputStream(data);
        result = StreamUtil.readInputStream(content, 0);
        assertEquals(0, result.length);
        assertTrue(content.read() != -1);

        data = generateRandomDataBuffer(100000);
        content = new ByteArrayInputStream(data);
        result = StreamUtil.readInputStream(content, Integer.MAX_VALUE);
        assertEquals(-1, content.read());
        assertTrue(buffersEqual(data, result));

        data = generateRandomDataBuffer(9999);
        content = new ByteArrayInputStream(data);
        result = StreamUtil.readInputStream(content, 9999);
        assertEquals(result.length, 9999);
        assertEquals(-1, content.read());
        assertTrue(buffersEqual(data, result));

        // Do some tests with slow input stream
        data = generateRandomDataBuffer(1000);
        content = new SmallChunkInputStream(data);
        result = StreamUtil.readInputStream(content, 9999);
        assertTrue(((SmallChunkInputStream)content).isClosed());
        assertTrue(buffersEqual(data, result));

        data = generateRandomDataBuffer(1024);
        content = new SmallChunkInputStream(data);
        result = StreamUtil.readInputStream(content, 1024);
        assertTrue(((SmallChunkInputStream)content).isClosed());
        assertTrue(buffersEqual(data, result));

        data = generateRandomDataBuffer(100000);
        content = new SmallChunkInputStream(data);
        result = StreamUtil.readInputStream(content, 50000);
        assertTrue(((SmallChunkInputStream)content).isClosed());
        assertEquals(50000, result.length);
        assertEquals(50000, content.available());
        assertTrue(buffersEqual(Arrays.copyOf(data, 50000), result));
    }


    /**
     * Test of stringToStream method, of class StreamUtil.
     */
    @Test
    public void testStringToStream_String_String() throws Exception {
        String s = "foobar æøå";
        String encoding = "UTF-8";
        InputStream stream = StreamUtil.stringToStream(s, encoding);
        byte[] data = StreamUtil.readInputStream(stream);
        assertTrue(buffersEqual(data, s.getBytes(encoding)));
    }

    /**
     * Test of streamToString method, of class StreamUtil.
     */
    @Test
    public void testStreamToString_InputStream_String() throws Exception {
        String s = "foobar ææå";
        ByteArrayInputStream in = new ByteArrayInputStream(s.getBytes("utf-8"));
        String result = StreamUtil.streamToString(in, "utf-8");
        assertEquals(s, result);
    }

    private boolean buffersEqual(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }

        for (int i=0; i<a.length; i++) {
            if (a[i] != b[i])  return false;
        }

        return true;
    }

    private byte[] generateRandomDataBuffer(int size) {
        byte[] data = new byte[size];

        for (int i=0; i<size; i++) {
            data[i] = (byte)((int)(Math.random()*0xFF));
        }

        return data;
    }

    // Performance tests, comparing old and new
//    @Test
//    public void perftest() throws Exception {
//        byte[] data = generateRandomDataBuffer(65536);
//
//        long start, end;
//        for (int r = 0; r < 2; r++) {
//            start = System.currentTimeMillis();
//            for (int i = 0; i < 20000; i++) {
//                InputStream in = new ByteArrayInputStream(data);
//                byte[] result = StreamUtil.readInputStream(in);
//            }
//            end = System.currentTimeMillis();
//            System.out.println("New impl: " + (end - start) + "ms");
//
//            start = System.currentTimeMillis();
//            for (int i = 0; i < 20000; i++) {
//                InputStream in = new ByteArrayInputStream(data);
//                byte[] result = StreamUtil.readInputStreamOLD(in);
//            }
//            end = System.currentTimeMillis();
//            System.out.println("OLD impl: " + (end - start) + "ms");
//        }
//    }
//    @Test
//    public void perftestSmallChunks() throws Exception {
//        byte[] data = generateRandomDataBuffer(50000);
//        long start, end;
//
//        for (int r = 0; r < 2; r++) {
//            start = System.currentTimeMillis();
//            for (int i = 0; i < 1000; i++) {
//                InputStream in = new SmallChunkInputStream(data);
//                StreamUtil.readInputStream(in);
//            }
//            end = System.currentTimeMillis();
//            System.out.println("New impl small chunks: " + (end - start) + "ms");
//
//            start = System.currentTimeMillis();
//            for (int i = 0; i < 1000; i++) {
//                InputStream in = new SmallChunkInputStream(data);
//                StreamUtil.readInputStreamOLD(in);
//            }
//            end = System.currentTimeMillis();
//            System.out.println("OLD impl small chunks: " + (end - start) + "ms");
//        }
//    }
//    @Test
//    public void perftestCap() throws Exception {
//        byte[] data = generateRandomDataBuffer(65536);
//        long start, end;
//
//        for (int r=0; r<2; r++) {
//            start = System.currentTimeMillis();
//            for (int i = 0; i < 20000; i++) {
//                InputStream in = new ByteArrayInputStream(data);
//                StreamUtil.readInputStream(in, 30000);
//            }
//            end = System.currentTimeMillis();
//            System.out.println("New impl cap: " + (end - start) + "ms");
//
//            start = System.currentTimeMillis();
//            for (int i = 0; i < 20000; i++) {
//                InputStream in = new ByteArrayInputStream(data);
//                StreamUtil.readInputStreamOLD(in, 30000);
//            }
//            end = System.currentTimeMillis();
//            System.out.println("OLD impl cap: " + (end - start) + "ms");
//        }
//    }
//    @Test
//    public void perftestCapSmallChunks() throws Exception {
//        byte[] data = generateRandomDataBuffer(65536);
//        long start, end;
//
//        for (int r = 0; r < 2; r++) {
//            start = System.currentTimeMillis();
//            for (int i = 0; i < 5000; i++) {
//                InputStream in = new SmallChunkInputStream(data);
//                StreamUtil.readInputStream(in, 30000);
//            }
//            end = System.currentTimeMillis();
//            System.out.println("New impl cap small: " + (end - start) + "ms");
//
//            start = System.currentTimeMillis();
//            for (int i = 0; i < 5000; i++) {
//                InputStream in = new SmallChunkInputStream(data);
//                StreamUtil.readInputStreamOLD(in, 30000);
//            }
//            end = System.currentTimeMillis();
//            System.out.println("OLD impl cap small: " + (end - start) + "ms");
//        }
//    }

}

// Simulate an input stream that only reads small chunks of random size at a time.
// Also record if close() has been called.
class SmallChunkInputStream extends ByteArrayInputStream {

    boolean closed = false;

    public SmallChunkInputStream(byte[] buffer) {
        super(buffer);
    }

    @Override
    public int read(byte[] buffer, int offset, int len) {
        return super.read(buffer, offset, Math.min((int)(Math.random()*5 + 1), len));
    }

    @Override
    public void close() throws IOException {
        this.closed = true;
    }

    public boolean isClosed() {
        return this.closed;
    }
}

// Just record close() call
class CheckForCloseByteArrayOutputStream extends ByteArrayOutputStream {

    private boolean closed = false;

    public CheckForCloseByteArrayOutputStream() {
        super();
    }

    @Override
    public void close() throws IOException {
        this.closed = true;
    }

    public boolean isClosed() {
        return this.closed;
    }
}
