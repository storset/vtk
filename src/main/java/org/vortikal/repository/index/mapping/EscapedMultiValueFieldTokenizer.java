/* Copyright (c) 2006, University of Oslo, Norway
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of the University of Oslo nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *      
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.vortikal.repository.index.mapping;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.Tokenizer;

/**
 * EscapedMultiValueFieldTokenizer.
 * Split input into tokens at un-escaped splitting character(s). 
 * Escape-character is always '\'.
 * 
 * The escape-character ('\') is always removed from the tokens before they
 * are returned for indexing. Thus, searching on fields analyzed by this analyzer 
 * should require no pre-escaping of the splitting character. To avoid
 * losing literal back-slashes from the tokens, they must themselves be escaped: '\\'.
 * 
 * When a single back-slash precedes any other character than the splitting 
 * character and the back-slash itself, it has no special meaning and becomes
 * part of the token.
 * 
 * Empty tokens are discarded.
 * 
 * @TODO: Make MAX_TOKEN_LEN a constructor parameter
 * @deprecated
 * 
 * @author oyviste
 */
public final class EscapedMultiValueFieldTokenizer extends Tokenizer {

    public static final char ESCAPE_CHAR = '\\';
    public static final int MAX_TOKEN_LEN = 2048;
    
    private final char splitChar;
    private char[] tokenBuffer;
    private int streamOffset;
    
    public EscapedMultiValueFieldTokenizer(Reader in, char splitChar) {
        super(in);
        this.splitChar = splitChar;
        this.streamOffset = 0;
        this.tokenBuffer = new char[MAX_TOKEN_LEN];
    }
    
    public final Token next() throws IOException {
        
        int c, tSize = 0, start = this.streamOffset;
        boolean escape = false;
        while ((c = this.input.read()) != -1) {
            ++this.streamOffset;

            if (c == this.splitChar) {
                if (escape) {
                    --tSize;
                } else {
                    if (tSize == 0) { // Drop empty tokens
                        escape = false;
                        start = this.streamOffset;
                        continue;
                    }
                    break;
                }
            } else if (c == ESCAPE_CHAR && escape) {
                escape = false;
                continue;
            }
            
            this.tokenBuffer[tSize++] = (char)c; // Add character to token

            if (tSize == MAX_TOKEN_LEN) {
                // Max token length reached, return it (forced break)
                break;
            }
            
            escape = (c == ESCAPE_CHAR); 
        }
        
        if (c == -1) {
            if (tSize == 0) return null; // No more tokens left.
            ++this.streamOffset;
        }

        return new Token(new String(this.tokenBuffer, 0, tSize), start, this.streamOffset-1);
    }
}
