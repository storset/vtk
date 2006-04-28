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
 * MultiValueFieldTokenizer.
 * Split at un-escaped splitting char.
 * 
 * Remove escape characters from tokens so that escaping while
 * searching becomes un-necessary.
 * 
 * @author oyviste
 *
 */
public class MultiValueFieldTokenizer extends Tokenizer {

    private static final int MAX_TOKEN_LEN = 2048;
    
    public MultiValueFieldTokenizer(Reader in) {
        super(in);
    }
    
    private char[] ioBuffer = new char[4096];
    private int ioBufferIndex = 0;
    private int dataLen = 0;

    private int start = 0; // Position of first token
    
    public final Token next() throws IOException {
        int tokenLength = 0;
        char[] tokenBuffer = new char[MAX_TOKEN_LEN];
        int offset = 0;
        while (true) {
            
            dataLen = input.read(ioBuffer);
            
            break;// XXX: not finished. We might not need this, depends on behaviour when adding same field
            // multiple times to index (we may be able to map pretty good to what we do in the database
            // for multi-valued props).
            
            // <Read into buf>
            // <scan forward for un-escaped split char>
            // <if none found in current io-buffer, then break, and let the
            //  iobuffer fill up again>
            //   <if iobuffer empty, return null (signals end of token stream)
            // <if found, then un-escape string and create token>
            //   <return token>
            
        }
        
        return null;
    }

}
