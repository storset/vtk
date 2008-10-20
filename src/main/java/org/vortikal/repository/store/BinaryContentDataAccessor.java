package org.vortikal.repository.store;

import java.io.InputStream;

public interface BinaryContentDataAccessor {

	public InputStream getBinaryStream(String binaryName, String binaryRef);
	
	public String getBinaryMimeType(String binaryName, String binaryRef);

}
