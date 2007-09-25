package org.vortikal.repositoryimpl.index.mapping;

import java.io.IOException;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;

/**
 * Simple {@link TokenStream} implementation with a <code>String</code> array
 * as input.
 * 
 * @author oyviste
 *
 */
public class StringArrayTokenStream extends TokenStream {

    private String[] values;
    private int currentValueIndex;
    private int currentTermOffset;

    
    public StringArrayTokenStream(String[] values) {
        this.values = values;
        this.currentValueIndex = 0;
        this.currentTermOffset = 0;
    }

    @Override
    public Token next() throws IOException {
        if (currentValueIndex == values.length) {
            return null; // Signals EOS
        }
        
        String termText = values[currentValueIndex++];
        int endOffset = currentTermOffset + termText.length();
        Token token = new Token(termText, currentTermOffset, endOffset);
        currentTermOffset = endOffset;
        
        return token;
    }
    
}
