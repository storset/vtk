package org.vortikal.repository.index.mapping;

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
public final class StringArrayTokenStream extends TokenStream {

    private String[] values;
    private int currentValueIndex;
    private int currentTermOffset;
    
    public StringArrayTokenStream(String[] values) {
        this.values = values;
        this.currentValueIndex = 0;
        this.currentTermOffset = 0;
    }
    
    @Override
    public Token next(final Token reusableToken) throws IOException {
        if (currentValueIndex == values.length) {
            return null; // Signals EOS
        }
        
        String termText = values[currentValueIndex++];

        int endOffset = currentTermOffset + termText.length();
        
        char[] termBuffer = reusableToken.termBuffer();
        if (termBuffer.length < termText.length()) {
            termBuffer = reusableToken.resizeTermBuffer(termText.length());
        }
        termText.getChars(0, termText.length(), termBuffer, 0);
        reusableToken.setStartOffset(currentTermOffset);
        reusableToken.setEndOffset(endOffset);
        reusableToken.setTermLength(termText.length());
        
        currentTermOffset = endOffset;
        
        return reusableToken;
    }
    
}
