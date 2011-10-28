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

import org.apache.log4j.Logger;
import org.vortikal.repository.ContentStream;
import org.vortikal.util.io.StreamUtil;

import com.ibatis.sqlmap.client.extensions.ParameterSetter;
import com.ibatis.sqlmap.client.extensions.ResultGetter;
import com.ibatis.sqlmap.client.extensions.TypeHandlerCallback;

public class BinaryContentTypeHandlerCallBack implements TypeHandlerCallback {

    private static final Logger log = Logger.getLogger(BinaryContentTypeHandlerCallBack.class);

    @Override
    public Object getResult(ResultGetter getter) throws SQLException {
        Blob blob = getter.getBlob();
        try {
            byte[] blobdata = StreamUtil.readInputStream(blob.getBinaryStream());
            return new ContentStream(new ByteArrayInputStream(blobdata), blobdata.length);
        } catch (IOException e) {
            log.error("An error occured while getting binary stream for a blob", e);
        }
        return null;
    }

    @Override
    public void setParameter(ParameterSetter setter, Object parameter) throws SQLException {
        ContentStream cs;
        if (parameter instanceof byte[]) {
            byte[] b = (byte[]) parameter;
            cs = new ContentStream(new ByteArrayInputStream(b), b.length);
        } else {
            cs = (ContentStream) parameter;
        }
        Blob blob = new ContentStreamBlob(cs);
        setter.setBlob(blob);
    }

    @Override
    public Object valueOf(String s) {
        return null;
    }

    private static final class ContentStreamBlob implements Blob {

        private ContentStream stream = null;

        public ContentStreamBlob(ContentStream stream) {
            this.stream = stream;
        }

        @Override
        public void free() throws SQLException {
            this.stream = null;
        }

        @Override
        public InputStream getBinaryStream() throws SQLException {
            return this.stream.getStream();
        }

        @Override
        public InputStream getBinaryStream(long pos, long length) throws SQLException {
            throw new UnsupportedOperationException("TODO: implement getBinaryStream(pos, length)");
        }

        @Override
        public byte[] getBytes(long pos, int length) throws SQLException {
            throw new UnsupportedOperationException("TODO: implement getBytes(pos, length)");
        }

        @Override
        public long length() throws SQLException {
            return this.stream.getLength();
        }

        @Override
        public long position(byte[] pattern, long start) throws SQLException {
            // TODO implement
            return 0;
        }

        @Override
        public long position(Blob pattern, long start) throws SQLException {
            // TODO implement
            return 0;
        }

        @Override
        public OutputStream setBinaryStream(long pos) throws SQLException {
            // TODO implement
            return null;
        }

        @Override
        public int setBytes(long pos, byte[] bytes) throws SQLException {
            // TODO implement
            return 0;
        }

        @Override
        public int setBytes(long pos, byte[] bytes, int offset, int len) throws SQLException {
            // TODO implement
            return 0;
        }

        @Override
        public void truncate(long len) throws SQLException {
            // TODO implement
        }
    }
}
