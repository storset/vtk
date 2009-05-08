package org.vortikal.repository.store.db.ibatis;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.vortikal.repository.ContentStream;

import com.ibatis.sqlmap.client.extensions.ParameterSetter;
import com.ibatis.sqlmap.client.extensions.ResultGetter;
import com.ibatis.sqlmap.client.extensions.TypeHandlerCallback;

public class BinaryContentTypeHandlerCallBack implements TypeHandlerCallback {

	private static final Logger log = Logger
			.getLogger(BinaryContentTypeHandlerCallBack.class);

	public Object getResult(ResultGetter getter) throws SQLException {
		Blob blob = getter.getBlob();
		try {
			InputStream is = blob.getBinaryStream();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			int i;
			while ((i = is.read()) != -1) {
				out.write(i);
			}
			return new ContentStream(new ByteArrayInputStream(out.toByteArray()), blob.length());
		} catch (IOException e) {
			log.error("An error occured while getting binary stream for a blob", e);
		}
		return null;

	}

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

	public Object valueOf(String s) {
		return null;
	}

	
    public class ContentStreamBlob implements Blob {

        private ContentStream stream = null;

        public ContentStreamBlob(ContentStream stream) {
            this.stream = stream;
        }

        public void free() throws SQLException {
        }

        public InputStream getBinaryStream() throws SQLException {
            return this.stream.getStream();
        }

        public InputStream getBinaryStream(long pos, long length) throws SQLException {
            throw new IllegalStateException("TODO: implement getBinaryStream(pos, length)");
        }

        public byte[] getBytes(long pos, int length) throws SQLException {
            throw new IllegalStateException("TODO: implement getBytes(pos, length)");
        }

        public long length() throws SQLException {
            return this.stream.getLength();
        }

        public long position(byte[] pattern, long start) throws SQLException {
            // TODO Auto-generated method stub
            return 0;
        }

        public long position(Blob pattern, long start) throws SQLException {
            // TODO Auto-generated method stub
            return 0;
        }

        public OutputStream setBinaryStream(long pos) throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public int setBytes(long pos, byte[] bytes) throws SQLException {
            // TODO Auto-generated method stub
            return 0;
        }

        public int setBytes(long pos, byte[] bytes, int offset, int len) throws SQLException {
            // TODO Auto-generated method stub
            return 0;
        }

        public void truncate(long len) throws SQLException {
            // TODO Auto-generated method stub

        }
        
        private byte[] getSubset(int pos, int length) throws SQLException {
            throw new IllegalStateException("TODO: implement getSubset(pos, length)");
        }
    }
	
	
	public class BinaryValueBlob implements Blob {

	    private byte[] binaryContent = null;

	    public BinaryValueBlob(byte[] binaryContent) {
	        this.binaryContent = binaryContent;
	    }

	    public void free() throws SQLException {
	    }

	    public InputStream getBinaryStream() throws SQLException {
	        return new ByteArrayInputStream(binaryContent);
	    }

	    public InputStream getBinaryStream(long pos, long length) throws SQLException {
	        if (pos == 0 && length == binaryContent.length) {
	            return getBinaryStream();
	        }
	        return new ByteArrayInputStream(getSubset((int) pos, (int) length));
	    }

	    public byte[] getBytes(long pos, int length) throws SQLException {
	        if (pos == 0 && length == binaryContent.length) {
	            return binaryContent;
	        }
	        return getSubset((int)pos, length);
	    }

	    public long length() throws SQLException {
	        return binaryContent.length;
	    }

	    public long position(byte[] pattern, long start) throws SQLException {
	        // TODO Auto-generated method stub
	        return 0;
	    }

	    public long position(Blob pattern, long start) throws SQLException {
	        // TODO Auto-generated method stub
	        return 0;
	    }

	    public OutputStream setBinaryStream(long pos) throws SQLException {
	        // TODO Auto-generated method stub
	        return null;
	    }

	    public int setBytes(long pos, byte[] bytes) throws SQLException {
	        // TODO Auto-generated method stub
	        return 0;
	    }

	    public int setBytes(long pos, byte[] bytes, int offset, int len) throws SQLException {
	        // TODO Auto-generated method stub
	        return 0;
	    }

	    public void truncate(long len) throws SQLException {
	        // TODO Auto-generated method stub

	    }
	    
	    private byte[] getSubset(int pos, int length) throws SQLException {
	        try {
	            byte[] newbytes = new byte[length];
	            System.arraycopy(binaryContent, pos, newbytes, 0, length);
	            return newbytes;
	        } catch (Exception e) {
	            throw new SQLException("Could not get subset of array");
	        }
	    }
	}

}
