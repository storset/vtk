/* Copyright (c) 2007, University of Oslo, Norway
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
package org.vortikal.repository.store.fs.jca;

import java.io.File;
import java.io.IOException;

import org.vortikal.repository.Path;

public class CreateOperation extends AbstractFileOperation {
    private Path uri;
    private File tmp;
    private FileMapper mapper;

    public CreateOperation(Path uri, boolean isCollection, FileMapper mapper) throws IOException {
        this.uri = uri;
        this.mapper = mapper;
        String tmpFileName = mapper.newTempFileName();

        this.tmp = new File(tmpFileName);
        if (isCollection) {
            this.tmp.mkdir();
        } else {
            this.tmp.createNewFile();
        }
        mapper.mapFile(uri, this.tmp);
    }

    public void persist() throws IOException {
        File file = this.mapper.getFile(this.uri);
        move(this.tmp, file);
    }
    
    public void forget() throws IOException {
        delete(this.tmp);
    }

    public String toString() {
        return getClass().getName() + "[create: " + this.uri + " via tmp file "
            + this.tmp + "]";
    }
}
