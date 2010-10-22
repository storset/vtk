package org.vortikal.repository.index.mapping;

import java.io.IOException;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;

/**
 * Simple {@link TokenStream} implementation with a <code>String</code> array
 * as input.
 *
 * TODO Does not support token offset attributes, but that can be easily added if
 * necessary.
 * 
 * @author oyviste
 *
 */
public final class StringArrayTokenStream extends TokenStream {

    private String[] values;
    private int currentValueIndex;
    private final TermAttribute termAttr;
    
    public StringArrayTokenStream(String[] values) {
        super();
        this.values = values;
        this.currentValueIndex = 0;
        this.termAttr = addAttribute(TermAttribute.class);
    }

    @Override
    public void reset() throws IOException {
        this.currentValueIndex = 0;
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (this.currentValueIndex == this.values.length) {
            return false; // Signals EOS
        }

        clearAttributes();

        this.termAttr.setTermBuffer(this.values[this.currentValueIndex++]);

        return true;
    }
    
}
