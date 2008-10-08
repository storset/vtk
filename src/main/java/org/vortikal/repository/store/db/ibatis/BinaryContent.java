package org.vortikal.repository.store.db.ibatis;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.SQLException;

public class BinaryContent implements Blob {

	private byte[] binaryContent = null;

	public BinaryContent(byte[] binaryContent) {
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

	public int setBytes(long pos, byte[] bytes, int offset, int len)
			throws SQLException {
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
			throw new java.sql.SQLException("Could not get subset of array", e);
		}
	}

}
