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
package org.vortikal.web.controller.repository;

import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.SimpleFormController;



public class FileUploadController extends SimpleFormController {

    private static Log logger = LogFactory.getLog(FileUploadController.class);

    private Repository repository = null;

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    protected Object formBackingObject(HttpServletRequest request)
            throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        Service service = requestContext.getService();

        Resource resource = repository.retrieve(
            securityContext.getToken(), requestContext.getResourceURI(), false);

        String url = service.constructLink(
            resource, securityContext.getPrincipal());

        FileUploadCommand command = new FileUploadCommand(url);

        return command;
    }

    protected void doSubmitAction(Object command) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();

        String uri = requestContext.getResourceURI();
        String token = securityContext.getToken();

        // cast the bean
        FileUploadCommand fileUploadCommand = (FileUploadCommand) command;

        if (fileUploadCommand.getCancel() != null) {
            fileUploadCommand.setDone(true);
            return;
        }

        // let's see if there's content there
        MultipartFile file = fileUploadCommand.getFile();

        if (file == null) {
            logger.info("The user didn't upload anything");
            //FIXME: what to do?
            // hmm, that's strange, the user did not upload anything
        }

        String name = file.getOriginalFilename();
        try {

            name = stripWindowsPath(name);

            if (name == null || name.trim().equals("")) {

                // FIXME: shouldn't be here
                return;
            }

            String itemURI = uri.equals("/") ? "/" + name : uri + "/" + name;

            if (logger.isDebugEnabled()) {
                logger.debug("Uploaded resource will be: " + itemURI);
            }

            boolean exists = repository.exists(token, itemURI);
            if (exists) {

                if (logger.isDebugEnabled()) {
                    logger.debug("Uploaded file already existed.");
                }
                // FIXME: shouldn't be here
                return;
            }

            Resource newResource = repository.createDocument(token, itemURI);
            if (file.getContentType() != null) {

                if (logger.isDebugEnabled()) {
                    logger.debug("Setting content type of resource to "
                                 + file.getContentType());
                }
                newResource.setContentType(file.getContentType());
                repository.store(token, newResource);
            }

            InputStream inStream = file.getInputStream();
            repository.storeContent(token, itemURI, inStream);

        } catch (Exception e) {
            logger.info("Caught exception while performing file upload", e);
            // FIXME: what to do?
        }

        fileUploadCommand.setDone(true);

    }

    /**
     * Attempts to extract only the file name from a Windows style
     * pathname, by stripping away everything up to and including the
     * last backslash in the path.
     */
    static String stripWindowsPath(String fileName) {

        if (fileName == null || fileName.trim().equals("")) {
            return fileName;
        }

        if (fileName.indexOf("\\") < 0) {

            return new String(fileName);
        }

        int pos = fileName.lastIndexOf("\\");

        if (pos > fileName.length() - 2) {

            return new String(fileName);
        }

        return fileName.substring(pos + 1, fileName.length());
    }

}

