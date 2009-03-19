package org.vortikal.repository.index.mapping;

import java.io.Reader;

import org.apache.solr.analysis.HTMLStripReader;

public class HTMLWhitespaceStripReader extends HTMLStripReader {

	public HTMLWhitespaceStripReader(Reader source) {
		super(source);
	}

}
