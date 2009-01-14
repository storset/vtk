package org.vortikal.repository.store;

import org.vortikal.repository.ContentStream;

public interface BinaryContentDataAccessor {

	public ContentStream getBinaryStream(String binaryName, String binaryRef);
	
	public String getBinaryMimeType(String binaryName, String binaryRef);

}
