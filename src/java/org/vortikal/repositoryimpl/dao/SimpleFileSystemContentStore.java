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
package org.vortikal.repositoryimpl.dao;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.IllegalOperationException;
import org.vortikal.util.web.URLUtil;

public class SimpleFileSystemContentStore implements InitializingBean, ContentStore {

    private static Log logger = LogFactory.getLog(SimpleFileSystemContentStore.class);

    private String repositoryDataDirectory;

    private boolean urlEncodeFileNames = false;

    public void createResource(String uri, boolean isCollection) 
        throws IOException {

        String fileName = getLocalFilename(uri);

        if (isCollection) {
            if (logger.isDebugEnabled()) {
                logger.debug("Creating directory " + fileName);
            }

            new File(fileName).mkdir();
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Creating file " + fileName);
            }

            new File(fileName).createNewFile();
        }
    }
    
    public long getContentLength(String uri) throws IllegalOperationException {
        String fileName = getLocalFilename(uri);

        File f = new File(fileName);
        if (f.isFile()) {
            return f.length();
        } 
        
        if (f.isDirectory()) {
            throw new IllegalOperationException("Length is undefined for collections");
        }  
        
        return 0L; // Same as java.io.File#length() when file does not exist 
    }

    public void deleteResource(String uri) {
        String fileName = getLocalFilename(uri);
        //Don't delete root
        if (!uri.equals("/")) {
            deleteFiles(new File(fileName));
        }
    }

    private void deleteFiles(File f) {
        if (!f.isDirectory()) {
            f.delete();

            return;
        }

        File[] children = f.listFiles();

        for (int i = 0; i < children.length; i++) {
            deleteFiles(children[i]);
        }

        f.delete();
    }

    public InputStream getInputStream(String uri) throws IOException {
        String fileName = getLocalFilename(uri);

        return new java.io.FileInputStream(new File(fileName));
    }

    public void storeContent(String uri, InputStream inputStream)
            throws IOException {
        String fileName = getLocalFilename(uri);

        OutputStream stream = new java.io.FileOutputStream(new File(fileName));

        // XXX: Review impl.
        /* Write the input data to the resource: */
        byte[] buffer = new byte[1000];
        int n = 0;

        while ((n = inputStream.read(buffer, 0, buffer.length)) != -1) {
            stream.write(buffer, 0, n);
        }

        stream.flush();
        stream.close();
        inputStream.close();

    }
    
    public void copy(String srcURI, String destURI) throws IOException {
        String fileNameFrom = getLocalFilename(srcURI);
        String fileNameTo = getLocalFilename(destURI);

        File fromDir = new File(fileNameFrom);
        if (fromDir.isDirectory()) {
            copyDir(fromDir, new File(fileNameTo));
        } else {
            copyFile(fromDir, new File(fileNameTo));
        }
    }
    
    public boolean isCollection(String uri) throws IOException {
        String filename = getLocalFilename(uri);
        File f = new File(filename);
        if (! f.exists()) {
            throw new IOException("File does not exist");
        }
        
        return f.isDirectory();
    }
    
    public boolean exists(String uri) {
        String filename = getLocalFilename(uri);
        return new File(filename).exists();
    }
    
    private void copyDir(File fromDir, File toDir) throws IOException {

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
    

    private void copyFile(File from, File to) throws IOException {

        FileChannel srcChannel = new FileInputStream(from).getChannel();
        FileChannel dstChannel = new FileOutputStream(to).getChannel();
        dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
        srcChannel.close();
        dstChannel.close();
    }

    private String getLocalFilename(String uri) {
        return this.repositoryDataDirectory
        + ((this.urlEncodeFileNames) ? URLUtil.urlEncode(uri) : uri);
    }
    
    public void setRepositoryDataDirectory(String repositoryDataDirectory) {
        this.repositoryDataDirectory = repositoryDataDirectory;
    }

    public void setUrlEncodeFileNames(boolean urlEncodeFileNames) {
        this.urlEncodeFileNames = urlEncodeFileNames;
    }

    public void afterPropertiesSet() throws Exception {

        if (repositoryDataDirectory == null) {
            throw new IOException("Missing property \"repositoryDataDirectory\"");
        }

        File root = new File(this.repositoryDataDirectory);

        if (!root.isAbsolute()) {
            this.repositoryDataDirectory = System.getProperty("vortex.home") + File.separator
                    + repositoryDataDirectory;
            root = new File(this.repositoryDataDirectory);
        }

        if (!root.exists()) {
            root.mkdir();
        }
    }
    
}
