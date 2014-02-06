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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.util.io.StreamUtil;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;

@SuppressWarnings("deprecation")
public class FileUploadController extends SimpleFormController {

    private static Log logger = LogFactory.getLog(FileUploadController.class);

    private File tempDir = new File(System.getProperty("java.io.tmpdir"));

    private boolean downcaseNames = false;
    private Map<String, String> replaceNameChars;

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

        ServletFileUpload upload = new ServletFileUpload();

        RequestContext requestContext = RequestContext.getRequestContext();
        String token = requestContext.getSecurityToken();
        Repository repository = requestContext.getRepository();
        Path uri = requestContext.getResourceURI();
        
        String userProcessed = request.getParameter("userProcessed");
        String userProcessedUrisSkip = request.getParameter("userProcessedUrisSkip");
        String userProcessedUrisOverwrite = request.getParameter("userProcessedUrisOverwrite");
        String[] urisSkip  = {};
        String[] urisOverwrite = {};
        if(userProcessedUrisSkip != null) {
            urisSkip = userProcessedUrisSkip.split(",");
        }
        if(userProcessedUrisOverwrite != null) {
            urisOverwrite = userProcessedUrisOverwrite.split(",");
        }

        // Check request to see if there is any cached info about file item names

        List<String> fileItemNames = (List<String>) request.getAttribute("org.vortikal.MultipartUploadWrapper.FileItemNames");
        if (fileItemNames != null) {
            ArrayList<Path> existingUris = new ArrayList<Path>();
            
            // ok, use this to check for name collisions, so we can fail as early as possible.
            for (String name: fileItemNames) {
                name = stripWindowsPath(name);
                if (name == null || name.trim().equals("")) {
                    errors.rejectValue("file", "manage.upload.resource.name-problem", "A resource has an illegal name");
                    return showForm(request, response, errors);
                }
                Path itemPath = uri.extend(fixFileName(name));
                if (repository.exists(token, itemPath)) {
                    if (userProcessed != null) {
                       if(!(Arrays.asList(urisOverwrite).contains(itemPath.toString()) 
                         || Arrays.asList(urisSkip).contains(itemPath.toString()))) {
                           existingUris.add(itemPath);  
                       }
                    } else {
                       errors.rejectValue("file", "manage.upload.resource.exists", "A resource of this name already exists");
                       return showForm(request, response, errors);
                    }
                }
            }
            
            // return existing paths to make the user choose between skipping or overwriting them
            if(!existingUris.isEmpty()) {
                errors.rejectValue("file", "manage.upload.resource.exists", "A resource of this name already exists");
                fileUploadCommand.setExistingUris(existingUris);
                return processFormSubmission(request, response, fileUploadCommand, errors);
            }
        }
       
        
        // Iterate input stream. We can only safely consume the data once.
        FileItemIterator iter = upload.getItemIterator(request);
        Map<Path, StreamUtil.TempFile> fileMap = new LinkedHashMap<Path, StreamUtil.TempFile>();
        while (iter.hasNext()) {
            FileItemStream uploadItem = iter.next();
            if (!uploadItem.isFormField()) {
                String name = stripWindowsPath(uploadItem.getName());
                if (name == null || name.trim().equals("")) {
                    errors.rejectValue("file", "manage.upload.resource.name-problem", "A resource has an illegal name");
                    return showForm(request, response, errors);
                }
                
                String fixedName = fixFileName(name);
                Path itemPath = uri.extend(fixedName);
                
                // Skip or overwrite
                if (repository.exists(token, itemPath)) {
                    if (userProcessed != null) {
                        if(!Arrays.asList(urisOverwrite).contains(itemPath.toString())) {
                            continue;
                        }
                    } else {
                        errors.rejectValue("file", "manage.upload.resource.exists", "A resource of this name already exists");
                        // Clean up already created temporary files
                        for (StreamUtil.TempFile t: fileMap.values()) {
                            t.delete();
                        }
                        return showForm(request, response, errors);
                    }
                }

                StreamUtil.TempFile tmpFile = StreamUtil.streamToTempFile(uploadItem.openStream(), this.tempDir);
                fileMap.put(itemPath, tmpFile);
            }
        }
        
        // Write files
        for (Map.Entry<Path,StreamUtil.TempFile> entry: fileMap.entrySet()) {
            Path path = entry.getKey();
            StreamUtil.TempFile tempFile = entry.getValue();
            if (logger.isDebugEnabled()) {
                logger.debug("Uploaded resource will be: " + path);
            }
            try {
                if(repository.exists(token, path)) {
                    repository.delete(token, path, false);
                }
                repository.createDocument(token, path, tempFile.getFileInputStream());
                tempFile.delete();
            } catch (Exception e) {
                logger.warn("Caught exception while performing file upload", e);
                errors.rejectValue("file", "manage.upload.error",
                        "An unexpected error occurred while processing file upload");
                // Clean now to free up temp files faster
                for (StreamUtil.TempFile t : fileMap.values()) {
                    t.delete();
                }
                return showForm(request, response, errors);
            }
        }
        
        fileUploadCommand.setDone(true);
        return new ModelAndView(getSuccessView());
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

    public void setReplaceNameChars(Map<String, String> replaceNameChars) {
        this.replaceNameChars = replaceNameChars;
    }

    public void setDowncaseNames(boolean downcaseNames) {
        this.downcaseNames = downcaseNames;
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

}
