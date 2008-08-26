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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.vortikal.repository.Path;


public class CascadingFileMapper implements FileMapper {

    private File base;
    private File tmp;
    private FileMapper prev;
    private Map<Path, String> fileMap = new HashMap<Path, String>();

    public CascadingFileMapper(File base, File tmp, FileMapper prev) {
        this.base = base;
        this.tmp = tmp;
        this.prev = prev;
    }

    public String newTempFileName() throws IOException {
        // XXX:
        File f = File.createTempFile("ContentStore.FileMapper", null, this.tmp);
        String name = f.getAbsolutePath();
        if (!f.delete()) {
            throw new IOException("Unable to generate unique file name: " + name);
        }
        return name;
    }

    public File getFile(Path uri) throws IOException {
        if (this.prev == null) {
            return new File(this.base.toString() + uri);
        }

        List<Path> incrPath = uri.getPaths();
        List<String> path = uri.getElements();
        for (int i = 0; i < path.size(); i++) {
            Path incr = incrPath.get(i);
            if (this.fileMap.containsKey(incr)) {
                String mapping = this.fileMap.get(incr);
                for (int j = i + 1; j < path.size(); j++) {
                    mapping += "/" + path.get(j);
                }
                return new File(mapping);
            }
        }
        return prev.getFile(uri);
    }

    public void mapFile(Path uri, File file) {
        this.fileMap.put(uri, file.getAbsolutePath());
    }
}
