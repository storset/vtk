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
package org.vortikal.repositoryimpl.query;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.Tokenizer;

/**
 * EscapedMultiValueFieldTokenizer.
 * Split input into tokens at un-escaped splitting characters. 
 * Escape-character is always '\\'.
 * 
 * The escape-character ('\\') is always removed from the tokens before they
 * are returned for indexing. Thus, searching on fields analyzed by this analyzer 
 * should require no pre-escaping of the splitting character.
 * 
 * Empty tokens are discarded.
 * 
 * @author oyviste
 */
public class EscapedMultiValueFieldTokenizer extends Tokenizer {

    public static final char ESCAPE_CHAR = '\\';
    
    private static final int MAX_TOKEN_LEN = 2048;
    
    private char splitChar;
    private char[] tokenBuffer;
    private int streamOffset;
    
    public EscapedMultiValueFieldTokenizer(Reader in, char splitChar) {
        super(in);
        this.splitChar = splitChar;
        this.streamOffset = 0;
        this.tokenBuffer = new char[MAX_TOKEN_LEN];
    }
    
    public final Token next() throws IOException {
        int c, tOff = 0, start = streamOffset;
        boolean esc = false;
        while ((c = input.read()) != -1) {
            ++streamOffset;

            if (c == splitChar) {
                if (esc) {
                    tOff--;
                } else {
                    if (tOff == 0) { // Drop empty tokens
                        esc = false;
                        start = streamOffset;
                        continue;
                    } else break; 
                }
            }
            
            if (tOff == MAX_TOKEN_LEN-1) {
                // Max token length reached, return it (forced break)
                break;
            }
            
            tokenBuffer[tOff++] = (char)c; // Add character to token
            esc = c == ESCAPE_CHAR ? true : false;
        }
        
        if (c == -1) {
            if (tOff == 0) return null; // No more tokens left.
            ++streamOffset;
        }
        
        return new Token(new String(tokenBuffer, 0, tOff), start, streamOffset-1);
    }

}
