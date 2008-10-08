package org.vortikal.repository.store.db.ibatis;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.ibatis.sqlmap.client.extensions.ParameterSetter;
import com.ibatis.sqlmap.client.extensions.ResultGetter;
import com.ibatis.sqlmap.client.extensions.TypeHandlerCallback;

public class BinaryContentTypeHandlerCallBack implements TypeHandlerCallback {
	
	private static final Logger log = Logger.getLogger(BinaryContentTypeHandlerCallBack.class);

	public Object getResult(ResultGetter getter) throws SQLException {
		Blob blob = getter.getBlob();
		InputStream is = blob.getBinaryStream();
        byte[] binaryContent = new byte[(int)blob.length()];
        try {
			is.read(binaryContent);
			return binaryContent;
		} catch (IOException e) {
			log.error("An error occured while getting binary content from BLOB", e);
		}
		return null;
	}

	public void setParameter(ParameterSetter setter, Object parameter) throws SQLException {
		Blob blob = new BinaryContent((byte[]) parameter);
		setter.setBlob(blob);
	}

	public Object valueOf(String s) {
		return null;
	}

}
