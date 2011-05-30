/* Copyright (c) 2006, 2007, University of Oslo, Norway
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
package org.vortikal.repository.store.fs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.IllegalOperationException;
import org.vortikal.repository.Path;
import org.vortikal.repository.RecoverableResource;
import org.vortikal.repository.store.ContentStore;
import org.vortikal.repository.store.DataAccessException;
import org.vortikal.util.io.StreamUtil;
import org.vortikal.web.service.URL;

/**
 * File system content store implementation operating directly on the repository
 * data.
 */
public class SimpleFileSystemContentStore implements InitializingBean, ContentStore {

    private static Log logger = LogFactory.getLog(SimpleFileSystemContentStore.class);

    private String repositoryDataDirectory;
    private String repositoryTrashCanDirectory;

    private boolean urlEncodeFileNames = false;

    public void createResource(Path uri, boolean isCollection) throws DataAccessException {

        String fileName = getLocalFilename(uri);

        try {
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
        } catch (IOException e) {
            throw new DataAccessException("Create resource [" + uri + "] failed", e);
        }
    }

    public long getContentLength(Path uri) throws DataAccessException {
        String fileName = getLocalFilename(uri);

        try {
            File f = new File(fileName);
            if (!f.exists()) {
                throw new DataAccessException("No file exists for URI " + uri + " at " + f.getCanonicalPath());
            }
            if (f.isFile()) {
                return f.length();
            }
            throw new IllegalOperationException("Length is undefined for collections");
        } catch (IOException e) {
            throw new DataAccessException("Get content length [" + uri + "] failed", e);
        }
    }

    public void deleteResource(Path uri) {
        String fileName = getLocalFilename(uri);
        // Don't delete root
        if (!uri.equals(Path.ROOT)) {
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

    public InputStream getInputStream(Path uri) throws DataAccessException {
        String fileName = getLocalFilename(uri);
        try {
            return new java.io.FileInputStream(new File(fileName));
        } catch (IOException e) {
            throw new DataAccessException("Get input stream [" + uri + "] failed", e);
        }
    }

    public void storeContent(Path uri, InputStream inputStream) throws DataAccessException {
        String fileName = getLocalFilename(uri);
        File dest = new File(fileName);
        try {
            if (inputStream instanceof FileInputStream) {
                FileChannel srcChannel = ((FileInputStream) inputStream).getChannel();
                FileChannel dstChannel = new FileOutputStream(dest).getChannel();
                dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
                srcChannel.close();
                dstChannel.close();
                return;
            }
            FileOutputStream stream = new FileOutputStream(dest);
            StreamUtil.pipe(inputStream, stream);
            stream.close();
            inputStream.close();
        } catch (IOException e) {
            throw new DataAccessException("Store content [" + uri + "] failed", e);
        }
    }

    public void copy(Path srcURI, Path destURI) throws DataAccessException {
        String fileNameFrom = getLocalFilename(srcURI);
        String fileNameTo = getLocalFilename(destURI);
        try {
            File fromDir = new File(fileNameFrom);
            if (fromDir.isDirectory()) {
                copyDir(fromDir, new File(fileNameTo));
            } else {
                copyFile(fromDir, new File(fileNameTo));
            }
        } catch (IOException e) {
            throw new DataAccessException("Store content [" + fileNameFrom + ", " + fileNameTo + "] failed", e);
        }
    }

    private void copyDir(File fromDir, File toDir) throws IOException {

        toDir.mkdir();

        File[] children = fromDir.listFiles();
        for (int i = 0; i < children.length; i++) {
            File newFile = new File(toDir.getCanonicalPath() + File.separator + children[i].getName());
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

    public void move(Path srcURI, Path destURI) throws DataAccessException {
        String fileNameFrom = getLocalFilename(srcURI);
        String fileNameTo = getLocalFilename(destURI);
        if (!new File(fileNameFrom).renameTo(new File(fileNameTo))) {
            throw new DataAccessException("Unable to rename file " + fileNameFrom + " to " + fileNameTo);
        }
    }

    @Override
    public void moveToTrash(Path srcURI, final String trashIdDir) throws DataAccessException {

        String from = getLocalFilename(srcURI);
        File src = new File(from);

        String trashCanDir = this.repositoryTrashCanDirectory + "/" + trashIdDir;
        File trashDir = new File(trashCanDir);
        trashDir.mkdir();

        Path path = this.getEncodedPathIfConfigured(srcURI);
        File dest = new File(trashCanDir + "/" + path.getName());

        if (!src.renameTo(dest)) {
            throw new DataAccessException("Unable to move " + from + " to trash can");
        }
    }

    @Override
    public void recover(Path destURI, RecoverableResource recoverableResource) throws DataAccessException {
        String dest = this.getLocalFilename(destURI);
        String recover = dest + "/" + recoverableResource.getName();
        String trashPath = this.repositoryTrashCanDirectory + "/" + recoverableResource.getTrashUri();
        if (!new File(trashPath).renameTo(new File(recover))) {
            throw new DataAccessException("Unable to recover file " + recoverableResource.getTrashUri());
        }
        File trashDir = new File(this.repositoryTrashCanDirectory + "/" + recoverableResource.getTrashID());
        trashDir.delete();
    }

    @Override
    public void deleteRecoverable(RecoverableResource recoverableResource) throws DataAccessException {
        String filePath = this.repositoryTrashCanDirectory + "/" + recoverableResource.getTrashID();
        this.deleteFiles(new File(filePath));
    }

    private String getLocalFilename(Path uri) {
        Path path = this.getEncodedPathIfConfigured(uri);
        return this.repositoryDataDirectory + path.toString();
    }

    private Path getEncodedPathIfConfigured(Path original) {
        if (this.urlEncodeFileNames) {
            return URL.encode(original);
        }
        return original;
    }

    @Required
    public void setRepositoryDataDirectory(String repositoryDataDirectory) {
        this.repositoryDataDirectory = repositoryDataDirectory;
    }

    @Required
    public void setRepositoryTrashCanDirectory(String repositoryTrashCanDirectory) {
        this.repositoryTrashCanDirectory = repositoryTrashCanDirectory;
    }

    public void setUrlEncodeFileNames(boolean urlEncodeFileNames) {
        this.urlEncodeFileNames = urlEncodeFileNames;
    }

    public void afterPropertiesSet() throws Exception {
        this.createRootDirectory(this.repositoryDataDirectory);
        this.createRootDirectory(this.repositoryTrashCanDirectory);
    }

    private void createRootDirectory(String directoryPath) {
        File root = new File(directoryPath);

        if (!root.isAbsolute()) {
            directoryPath = System.getProperty("vortex.home") + File.separator + directoryPath;
            root = new File(directoryPath);
        }

        if (!root.exists()) {
            root.mkdir();
        }
    }

}
