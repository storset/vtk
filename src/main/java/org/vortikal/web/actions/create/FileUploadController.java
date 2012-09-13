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
package org.vortikal.web.actions.create;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;

@SuppressWarnings("deprecation")
public class FileUploadController extends SimpleFormController {

    private static Log logger = LogFactory.getLog(FileUploadController.class);

    private File tempDir = new File(System.getProperty("java.io.tmpdir"));

    // Default value in DiskFileItemFactory is 10 KB (10240 bytes) but we keep
    // this variable in case we want it configured from bean.
    private int sizeThreshold = DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD;

    private DiskFileItemFactory dfif = new DiskFileItemFactory(this.sizeThreshold, this.tempDir);

    private boolean downcaseNames = false;
    private Map<String, String> replaceNameChars;

    public void setSizeThreshold(int sizeThreshold) {
        this.sizeThreshold = sizeThreshold;
    }

    public void setTempDir(String tempDirPath) {
        File tmp = new File(tempDirPath);
        if (!tmp.exists()) {
            throw new IllegalArgumentException("Unable to set tempDir: file " + tmp + " does not exist");
        }
        if (!tmp.isDirectory()) {
            throw new IllegalArgumentException("Unable to set tempDir: file " + tmp + " is not a directory");
        }
        this.tempDir = tmp;
    }

    @Override
    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        Service service = requestContext.getService();
        Repository repository = requestContext.getRepository();

        Resource resource = repository.retrieve(requestContext.getSecurityToken(), requestContext.getResourceURI(),
                false);

        String url = service.constructLink(resource, requestContext.getPrincipal());

        FileUploadCommand command = new FileUploadCommand(url);
        return command;
    }

    @Override
    protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors) throws Exception {

        FileUploadCommand fileUploadCommand = (FileUploadCommand) command;

        if (fileUploadCommand.getCancelAction() != null) {
            fileUploadCommand.setDone(true);
            return new ModelAndView(getSuccessView());
        }

        ServletFileUpload upload = new ServletFileUpload(dfif);

        List<FileItem> items = new ArrayList<FileItem>();

        @SuppressWarnings("unchecked")
        List<FileItem> fileItems = upload.parseRequest(request);
        for (FileItem item : fileItems) {
            if (!item.isFormField()) {
                items.add(item);
            }
        }

        RequestContext requestContext = RequestContext.getRequestContext();
        String token = requestContext.getSecurityToken();
        Repository repository = requestContext.getRepository();
        Path uri = RequestContext.getRequestContext().getResourceURI();

        // Check for existing files
        for (FileItem uploadItem : items) {
            String name = stripWindowsPath(uploadItem.getName());

            if (name == null || name.trim().equals("")) {
                return new ModelAndView(getSuccessView());
            }
            Path itemURI = uri.extend(fixFileName(name));
            boolean exists = repository.exists(token, itemURI);
            if (exists) {
                cleanUp(items);
                errors.rejectValue("file", "manage.upload.resource.exists", "A resource with this name already exists");
                return showForm(request, response, errors);
            }
        }

        // Write files
        for (FileItem uploadItem : items) {

            if (uploadItem == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("The user didn't upload anything");
                }
                return new ModelAndView(getSuccessView());
            }

            String name = stripWindowsPath(uploadItem.getName());
            Path itemURI = uri.extend(fixFileName(name));

            if (logger.isDebugEnabled()) {
                logger.debug("Uploaded resource will be: " + itemURI);
            }

            try {
                InputStream inStream = uploadItem.getInputStream();
                repository.createDocument(token, itemURI, inStream);
            } catch (Exception e) {
                logger.warn("Caught exception while performing file upload", e);
                cleanUp(items);
                errors.rejectValue("file", "manage.upload.error",
                        "An unexpected error occurred while processing file upload");
                return showForm(request, response, errors);
            } finally {
                if (logger.isDebugEnabled()) {
                    logger.debug("Deleting: " + uploadItem.getName() + " from DiskFileItemFactory");
                }
                uploadItem.delete();
            }
        }

        fileUploadCommand.setDone(true);
        return new ModelAndView(getSuccessView());

    }

    private void cleanUp(List<FileItem> items) {
        if (items != null)
            for (FileItem uploadItem : items) {
                if (uploadItem != null)
                    if (logger.isDebugEnabled() && uploadItem.getName() != null) {
                        logger.debug("Cleanup: Deleting " + uploadItem.getName() + " from DiskFileItemFactory");
                    }
                    uploadItem.delete();
            }
    }

    /**
     * Attempts to extract only the file name from a Windows style pathname, by
     * stripping away everything up to and including the last backslash in the
     * path.
     */
    static String stripWindowsPath(String fileName) {

        if (fileName == null || fileName.trim().equals("")) {
            return null;
        }

        int pos = fileName.lastIndexOf("\\");

        if (pos > fileName.length() - 2) {
            return fileName;
        } else if (pos >= 0) {
            return fileName.substring(pos + 1, fileName.length());
        }

        return fileName;
    }

    public void setReplaceNameChars(Map<String, String> replaceNameChars) {
        this.replaceNameChars = replaceNameChars;
    }

    public void setDowncaseNames(boolean downcaseNames) {
        this.downcaseNames = downcaseNames;
    }

    private String fixFileName(String name) {
        if (this.downcaseNames) {
            name = name.toLowerCase();
        }

        if (this.replaceNameChars != null) {
            for (String regex : this.replaceNameChars.keySet()) {
                String replacement = this.replaceNameChars.get(regex);
                name = name.replaceAll(regex, replacement);
            }
        }
        return name;
    }

}
