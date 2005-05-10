/* Copyright (c) 2004, University of Oslo, Norway
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
package org.vortikal.web.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;



/**
 * A response wrapper that buffers the content written to it. Status
 * code and headers are passed through to the wrapped response.
 */
public class BufferedResponseWrapper extends HttpServletResponseWrapper {

    private ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
    private String contentType = null;
    private String characterEncoding = null;    
    private boolean isCommitted = false;


    public BufferedResponseWrapper(HttpServletResponse resp) {
        super(resp);
    }


    public byte[] getContentBuffer() {
        return this.bufferStream.toByteArray();
    }


    public String getContentType() {
        if (this.characterEncoding != null) {
            return this.contentType + ";charset=" + this.characterEncoding;
        }
        return this.contentType;
    }
    

    public void setContentType(String contentType) {
        super.setContentType(contentType);
        processContentTypeHeader(contentType);
    }


    public void addHeader(String name, String value) {
        super.addHeader(name, value);
        if ("Content-Type".equals(name)) {
            processContentTypeHeader(value);
        }
    }


    public void setHeader(String name, String value) {
        super.setHeader(name, value);
        if ("Content-Type".equals(name)) {
            processContentTypeHeader(value);
        }
    }


    public String getCharacterEncoding() {
        if (this.characterEncoding != null) {
            return this.characterEncoding;
        }
        return super.getCharacterEncoding();
    }
    

    public boolean isCommitted() {
        return this.isCommitted || super.isCommitted();
    }

    public ServletOutputStream getOutputStream() throws IOException {
        if (this.isCommitted) {
            throw new IllegalStateException(
                "getWriter() has already been called on this response.");
        }
        this.isCommitted = true;
        return new ByteArrayServletOutputStream(bufferStream, this.getCharacterEncoding());
    }


    public PrintWriter getWriter() throws IOException {
        if (this.isCommitted) {
            throw new IllegalStateException(
                "getOutputStream() has already been called on this response.");
        }
        this.isCommitted = true;
        return new ByteArrayServletOutputStreamWriter(new ByteArrayServletOutputStream(
                                            bufferStream, this.getCharacterEncoding()));
    }
    

    public HttpServletResponse getHttpServletResponse() {
        return (HttpServletResponse) getResponse();
    } 



    private class ByteArrayServletStreamWriter extends PrintWriter {
        private ByteArrayServletOutputStream stream;
        private Object error = null;
        

        public ByteArrayServletStreamWriter(ByteArrayServletOutputStream stream) {
            super(stream);
            this.stream = stream;
        }
        

        public boolean checkError() {
            return this.error == null;
        }
        

        public void close() {
        }
        

        public void flush() {
        }
        

        public void print(boolean b) {
            try {
                stream.print(b);
            } catch (IOException e) {
                this.error = e;
            }
        }
        

        public void print(char c) {
            try {
                stream.print(c);
            } catch (IOException e) {
                this.error = e;
            }
        }
        

        public void print(char[] s) {
            try {
                stream.print(new String(s));
            } catch (IOException e) {
                this.error = e;
            }
        }
        

        public void print(double d) {
            try {
                stream.print(d);
            } catch (IOException e) {
                this.error = e;
            }
        }
        

        public void print(float f) {
            try {
                stream.print(f);
            } catch (IOException e) {
                this.error = e;
            }
        }
        

        public void print(int i) {
            try {
                stream.print(i);
            } catch (IOException e) {
                this.error = e;
            }
        }
        

        public void print(long l) {
            try {
                stream.print(l);
            } catch (IOException e) {
                this.error = e;
            }
        }
        

        public void print(Object obj) {
            try {
                stream.print(obj.toString());
            } catch (IOException e) {
                this.error = e;
            }
        }
        

        public void print(String s) {
            try {
                stream.print(s);
            } catch (IOException e) {
                this.error = e;
            }
        }
        

        public void println() {
            try {
                stream.println();
            } catch (IOException e) {
                this.error = e;
            }
        }
        
        
        public void println(boolean x) {
            try {
                stream.println(x);
            } catch (IOException e) {
                this.error = e;
            }
        }
        

        public void println(char x) {
            try {
                stream.println(x);
            } catch (IOException e) {
                this.error = e;
            }
        }
        

        public void println(char[] x) {
            try {
                stream.println(new String(x));
            } catch (IOException e) {
                this.error = e;
            }
        }
        

        public void println(double x) {
            try {
                stream.println(x);
            } catch (IOException e) {
                this.error = e;
            }
        }
        
        
        public void println(float x) {
            try {
                stream.println(x);
            } catch (IOException e) {
                this.error = e;
            }
        }
        
        
        public void println(int x) {
            try {
                stream.println(x);
            } catch (IOException e) {
                this.error = e;
            }
        }
        
        
        public void println(long x) {
            try {
                stream.println(x);
            } catch (IOException e) {
                this.error = e;
            }
        }
        

        public void println(Object x) {
            try {
                stream.println(x.toString());
            } catch (IOException e) {
                this.error = e;
            }
        }
        

        public void println(String x) {
            try {
                stream.println(x);
            } catch (IOException e) {
                this.error = e;
            }
        }
        

        public void write(char[] buf) {
            try {
                stream.print(new String(buf));
            } catch (IOException e) {
                this.error = e;
            }
        }
        
        
        public void write(char[] buf,  int off,  int len) {
            try {
                stream.print(new String(buf, off, len));
            } catch (IOException e) {
                this.error = e;
            }
        }
        
        
        public void write(int c) {
            try {
                stream.write(c);
            } catch (IOException e) {
                this.error = e;
            }
        }
        
        
        public void write(String s) {
            try {
                stream.write(s.getBytes(stream.getCharacterEncoding()));
            } catch (IOException e) {
                this.error = e;
            }
            
        }
        
        
        public void write(String s, int off, int len) {
            try {
                stream.write(s.getBytes(stream.getCharacterEncoding()), off, len);
            } catch (IOException e) {
                this.error = e;
            }
        }
    }
    
    private void processContentTypeHeader(String value) {
        if (value.matches(".+/.+;.*charset.*=.+")) {

            String contentType = value.substring(
                0, value.indexOf(";")).trim();
            String characterEncoding = value.substring(
                value.indexOf("=") + 1).trim();

            if (characterEncoding.startsWith("\"")
                && characterEncoding.endsWith("\"")) {

                characterEncoding = characterEncoding.substring(
                    1, characterEncoding.length() - 1).trim();
            }
            this.contentType = contentType;
            this.characterEncoding = characterEncoding;

        } else {
            this.contentType = value;
        }
    }


}
