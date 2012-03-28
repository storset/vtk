/* Copyright (c) 2011, University of Oslo, Norway
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
package org.vortikal.repository.store.db.ibatis;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.SQLException;

import org.vortikal.util.io.StreamUtil;

import com.ibatis.sqlmap.client.extensions.ParameterSetter;
import com.ibatis.sqlmap.client.extensions.ResultGetter;
import com.ibatis.sqlmap.client.extensions.TypeHandlerCallback;

/**
 * Returns or sets binary content as byte[] array.
 * 
 * TODO Investigate if we can just replace this class with Spring's BlobByteArrayTypeHandler.
 *      Let the configured LobHandler impl handle the differences between Oracle and others.
 *      (We already explicitly set OracleLobHandler in config when running on Oracle db.)
 * 
 */
public class BinaryContentTypeHandlerCallBack implements TypeHandlerCallback {

    @Override
    public Object getResult(ResultGetter getter) throws SQLException {
        Blob blob = getter.getBlob();
        try {
            return StreamUtil.readInputStream(blob.getBinaryStream());
        } catch (IOException e) {
            throw new SQLException("Failed to read binary stream from database", e);
        }
    }

    @Override
    public void setParameter(ParameterSetter setter, Object parameter) throws SQLException {
        setter.setBlob(new ByteArrayBlobImpl((byte[])parameter));
    }

    @Override
    public Object valueOf(String s) {
        throw new UnsupportedOperationException();
    }

    private static final class ByteArrayBlobImpl implements Blob {

        private final byte[] bytes;

        ByteArrayBlobImpl(byte[] bytes) {
            if (bytes == null) { 
                throw new IllegalArgumentException("bytes cannot be null");
            }
            this.bytes = bytes;
        }

        @Override
        public InputStream getBinaryStream() throws SQLException {
            return new ByteArrayInputStream(this.bytes);
        }
        
        @Override
        public long length() throws SQLException {
            return this.bytes.length;
        }
        
        @Override
        public void free() throws SQLException {
            // No-op
        }

        // All other ops are unsupported:
        @Override
        public InputStream getBinaryStream(long pos, long length) throws SQLException {
            throw new UnsupportedOperationException();
        }

        @Override
        public byte[] getBytes(long pos, int length) throws SQLException {
            throw new UnsupportedOperationException();
        }

        @Override
        public long position(byte[] pattern, long start) throws SQLException {
            throw new UnsupportedOperationException();
        }

        @Override
        public long position(Blob pattern, long start) throws SQLException {
            throw new UnsupportedOperationException();
        }

        @Override
        public OutputStream setBinaryStream(long pos) throws SQLException {
            throw new UnsupportedOperationException();
        }

        @Override
        public int setBytes(long pos, byte[] bytes) throws SQLException {
            throw new UnsupportedOperationException();
        }

        @Override
        public int setBytes(long pos, byte[] bytes, int offset, int len) throws SQLException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void truncate(long len) throws SQLException {
            throw new UnsupportedOperationException();
        }
    }
}
