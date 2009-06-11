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
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.repository.MimeHelper;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;


public class FileUploadController extends SimpleFormController {

    private static Log logger = LogFactory.getLog(FileUploadController.class);

    private File tempDir = new File(System.getProperty("java.io.tmpdir"));
    
    private Repository repository = null;
    private int maxUploadSize = 100000000;

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setMaxUploadSize(int maxUploadSize) {
        this.maxUploadSize = maxUploadSize;
    }
    
    public void setTempDir(String tempDirPath) {
        File tmp = new File(tempDirPath);
        if (!tmp.exists()) {
            throw new IllegalArgumentException("Unable to set tempDir: file " 
                    + tmp + " does not exist");
        }
        if (!tmp.isDirectory()) {
            throw new IllegalArgumentException("Unable to set tempDir: file " 
                    + tmp + " is not a directory");
        }
        this.tempDir = tmp;
    }

    protected Object formBackingObject(HttpServletRequest request)
            throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        Service service = requestContext.getService();

        Resource resource = this.repository.retrieve(
            securityContext.getToken(), requestContext.getResourceURI(), false);

        String url = service.constructLink(
            resource, securityContext.getPrincipal());

        FileUploadCommand command = new FileUploadCommand(url);
        return command;
    }

        

    protected ModelAndView onSubmit(HttpServletRequest request,
                                    HttpServletResponse response,
                                    Object command,
                                    BindException errors)
        throws Exception {

        FileUploadCommand fileUploadCommand = (FileUploadCommand) command;

        if (fileUploadCommand.getCancelAction() != null) {
            fileUploadCommand.setDone(true);
            return new ModelAndView(getSuccessView());
        }

        FileItemFactory factory = new DiskFileItemFactory(
            this.maxUploadSize, this.tempDir);
        ServletFileUpload upload = new ServletFileUpload(factory);
    

        FileItem uploadItem = null;

        @SuppressWarnings("unchecked")
        List<FileItem> fileItems = upload.parseRequest(request);
        for (FileItem item: fileItems) {
            if (!item.isFormField()) {
                uploadItem = item;
            }
        }
        
        if (uploadItem == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("The user didn't upload anything");
            }
            return new ModelAndView(getSuccessView());
        }

        String name = stripWindowsPath(uploadItem.getName());
        
        if (name == null || name.trim().equals("")) {
            return new ModelAndView(getSuccessView());
        }

        Path uri = RequestContext.getRequestContext().getResourceURI();
        Path itemURI = uri.extend(name);

        if (logger.isDebugEnabled()) {
            logger.debug("Uploaded resource will be: " + itemURI);
        }

        String token = SecurityContext.getSecurityContext().getToken();

        try {

            boolean exists = this.repository.exists(token, itemURI);
            if (exists) {
                errors.rejectValue("file",
                                   "manage.upload.resource.exists",
                                   "A resource with this name already exists");
                return showForm(request, response, errors);
            }

            Resource newResource = this.repository.createDocument(token, itemURI);

            String contentType = uploadItem.getContentType();
            
            if (StringUtils.isBlank(contentType) || 
            		MimeHelper.DEFAULT_MIME_TYPE.equals(StringUtils.trim(contentType))) {
                contentType = MimeHelper.map(newResource.getName());
            }
            newResource.setContentType(contentType);
            this.repository.store(token, newResource);

            InputStream inStream = uploadItem.getInputStream();
            this.repository.storeContent(token, itemURI, inStream);

        } catch (Exception e) {
            logger.warn("Caught exception while performing file upload", e);
            errors.rejectValue("file",
                               "manage.upload.error",
                               "An unexpected error occurred while processing file upload");
            return showForm(request, response, errors);
        }

        fileUploadCommand.setDone(true);
        return new ModelAndView(getSuccessView());

    }



    /**
     * Attempts to extract only the file name from a Windows style
     * pathname, by stripping away everything up to and including the
     * last backslash in the path.
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

}

