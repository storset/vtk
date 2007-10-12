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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;


public abstract class AbstractFileOperation implements FileOperation {

    protected final void writeContent(File file, InputStream inputStream) throws IOException {
        System.out.println("__write_content: " + file);
        java.io.OutputStream stream = new java.io.FileOutputStream(file);

        // XXX: Review impl.
        /* Write the input data to the resource: */
        byte[] buffer = new byte[10000];
        int n = 0;

        while ((n = inputStream.read(buffer, 0, buffer.length)) != -1) {
            stream.write(buffer, 0, n);
        }

        stream.flush();
        stream.close();
        inputStream.close();
    }
        
    protected final void delete(File f) throws IOException {
        System.out.println("__delete: " + f);
        if (!f.isDirectory()) {
            if (!f.delete()) throw new IOException("Unable to delete file " + f);
            return;
        }

        File[] children = f.listFiles();
        for (int i = 0; i < children.length; i++) {
            delete(children[i]);
        }
        if (!f.delete()) throw new IOException("Unable to delete file " + f);
    }


    protected final void copyDir(File fromDir, File toDir) throws IOException {

        toDir.mkdir();

        File[] children = fromDir.listFiles();
        for (int i = 0; i < children.length; i++) {
            File newFile = new File(toDir.getCanonicalPath()
                                    + File.separator + children[i].getName());
            if (children[i].isFile()) {
                copyFile(children[i], newFile);
            } else {
                copyDir(children[i], newFile);
            }
        }
    }
    

    protected final void copyFile(File from, File to) throws IOException {
        if (!from.exists()) {
            throw new IOException("Source file " + from + " does not exist");
        }
        if (!to.exists()) {
            throw new IOException("Destination file " + to + " does not exist");
        }
        FileChannel srcChannel = new FileInputStream(from).getChannel();
        FileChannel dstChannel = new FileOutputStream(to).getChannel();
        dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
        srcChannel.close();
        dstChannel.close();
    }

    protected final void move(File from, File to) throws IOException {
        if (!from.exists()) {
            throw new IOException("Source file " + from + " does not exist");
        }
        if (from.renameTo(to)) {
            return;
        }
        if (to.exists() && to.isDirectory()) {
            delete(to);
        } 
        if (from.isFile()) {
            copyFile(from, to);
        } else {
            copyDir(from, to);
        }
        delete(from);
    }
        

}
