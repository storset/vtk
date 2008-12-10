package org.vortikal.repository.store;

import org.vortikal.repository.store.db.ibatis.BinaryStream;

public interface BinaryContentDataAccessor {

	public BinaryStream getBinaryStream(String binaryName, String binaryRef);
	
	public String getBinaryMimeType(String binaryName, String binaryRef);

}
