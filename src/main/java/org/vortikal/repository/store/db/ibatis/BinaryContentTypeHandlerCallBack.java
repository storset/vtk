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
