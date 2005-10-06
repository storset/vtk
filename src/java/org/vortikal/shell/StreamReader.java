/* Copyright (c) 2004, University of Oslo, Norway
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
package org.vortikal.shell;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;



/**
 * Implementation of the CommandReader interface reading from an input
 * stream. The source may be specified either as an InputStream
 * directly, or as a file name, in which case an InputStream for that
 * file is opened.
 */
public class StreamReader implements CommandReader {

    private String prompt = "$ ";
    private BufferedReader reader = null;


    public void setInputStream(InputStream inStream) throws IOException {
        if (this.reader != null) {
            this.reader.close();
        }
        this.reader = new BufferedReader(new InputStreamReader(inStream));
    }
    

    public void setFileName(String fileName) throws IOException {
        if (this.reader != null) {
            this.reader.close();
        }
        
        this.reader = new BufferedReader(
            new InputStreamReader(new FileInputStream(fileName)));
    }
    

    

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
    

    public String readLine(PrintStream out) throws IOException {
        out.print(prompt);
        return reader.readLine();
    }
    

    public void close() throws IOException {

    }
    
}
