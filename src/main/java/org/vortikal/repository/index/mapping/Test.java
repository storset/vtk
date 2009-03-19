package org.vortikal.repository.index.mapping;

import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.solr.analysis.HTMLStripReader;
import org.apache.solr.analysis.HTMLStripWhitespaceTokenizerFactory;

public class Test {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		String html = "<p><b><a href=/test.html>test</a></b> og enda <i>en</i></p><br />";
		
		HTMLStripReader stripReader = new HTMLStripReader(new StringReader(html));
        StringBuilder builder = new StringBuilder();
        int ch = -1;
        while ((ch = stripReader.read()) != -1) {
        	builder.append((char) ch); 
        }
        System.out.println(builder.toString());
		
		HTMLStripWhitespaceTokenizerFactory factory = new HTMLStripWhitespaceTokenizerFactory();
		StringReader reader = new StringReader(html);
		TokenStream tokenStream = factory.create(reader);
		while (true) {
			final Token reusableToken = new Token();
			Token t = tokenStream.next(reusableToken);
			if (t == null) {
				break;
			}
			String tokenText = new String(t.termBuffer(), 0, t.termLength());
			System.out.println(tokenText);
		}
		
		HTMLWhitespaceStripReader whitespaceStripReader = new HTMLWhitespaceStripReader(new StringReader(html));
        StringBuilder b = new StringBuilder();
        int ch2 = -1;
        while ((ch2 = whitespaceStripReader.read()) != -1) {
        	b.append((char) ch2); 
        }
        System.out.println(b.toString());
 
	}

}
