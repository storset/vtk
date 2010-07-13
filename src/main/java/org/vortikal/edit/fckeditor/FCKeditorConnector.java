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
package org.vortikal.edit.fckeditor;

import java.io.File;
import java.io.InputStream;
import java.text.Collator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.TypeInfo;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.repository.MimeHelper;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;



public class FCKeditorConnector implements Controller {
    private Repository repository;
    private Service viewService;
    private String browseViewName;
    private String uploadStatusViewName;
    private int maxUploadSize = 1000000;
    

    @Required public void setRepository(Repository repository) {
        this.repository = repository;
    }
    
    @Required public void setViewService(Service viewService) {
        this.viewService = viewService;
    }
    
    @Required public void setBrowseViewName(String browseViewName) {
        this.browseViewName = browseViewName;
    }
    
    @Required public void setUploadStatusViewName(String uploadStatusViewName) {
        this.uploadStatusViewName = uploadStatusViewName;
    }
    
    public void setMaxUploadSize(int maxUploadSize) {
        if (maxUploadSize <= 0) {
            throw new IllegalArgumentException("Max upload size must be a positive integer");
        }
        this.maxUploadSize = maxUploadSize;
    }
    
    
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        FCKeditorFileBrowserCommand command = new FCKeditorFileBrowserCommand(request);

        SecurityContext securityContext = SecurityContext.getSecurityContext();
        String token = securityContext.getToken();

        Locale locale = RequestContextUtils.getLocale(request);
        
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("currentFolder", ensureTrailingSlash(command.getCurrentFolder()));
        model.put("command", command.getCommand().name());
        model.put("resourceType", command.getResourceType());

        Filter fileFilter = null;

        FCKeditorFileBrowserCommand.ResourceType type = command.getResourceType();
        switch (type) {
            case Image:
                fileFilter = IMAGE_FILTER;
                break;
            case Flash:
                fileFilter = FLASH_FILTER;
                break;
            case Media:
                fileFilter = MEDIA_FILTER;
                break;
            default:
                fileFilter = FILE_FILTER;
                break;
        }
        
        FCKeditorFileBrowserCommand.Command c = command.getCommand();
        switch (c) {
            case GetFolders:
                try {
                    model.put("folders", listResources(token, command, COLLECTION_FILTER, locale));
                } catch (Exception e) {
                    model.put("error", 1);
                    model.put("customMessage", getErrorMessage(e));
                }
                break;

            case GetFoldersAndFiles:
                
                try {
                    model.put("folders", listResources(token, command, COLLECTION_FILTER, locale));
                    model.put("files", listResources(token, command, fileFilter, locale));
                } catch (Exception e) {
                    model.put("error", 1);
                    model.put("customMessage", getErrorMessage(e));
                }
                break;

            case CreateFolder:
                model.put("error", createFolder(command, token));
                break;

            case FileUpload:
                return uploadFile(command, token, request); 

            default:
                model.put("error", 1);
                model.put("customMessage", "Unknown command");
        }

        return new ModelAndView(this.browseViewName, model);
    }

    
    private Map<String, Map<String, Object>> listResources(String token, FCKeditorFileBrowserCommand command,
                                           Filter filter, Locale locale) throws Exception {

        Resource[] children = this.repository.listChildren(
            token, command.getCurrentFolder(), true);

        Map<String, Map<String, Object>> result = 
            new TreeMap<String, Map<String, Object>>(Collator.getInstance(locale));

        for (Resource r: children) {
            if (!filter.isAccepted(r)) {
                continue;
            }
            Map<String, Object> entry = new HashMap<String, Object>();
            URL url = this.viewService.constructURL(r, null);
            entry.put("resource", r);
            entry.put("url", url);
            if (!r.isCollection()) {
                entry.put("contentLength", r.getContentLength());
            }
            
            result.put(r.getURI().toString(), entry);
        }
        return result;
    }
    
    

    private int createFolder(FCKeditorFileBrowserCommand command, String token) {

        Path curFolder = command.getCurrentFolder();
        Path newFolderURI = curFolder.extend(command.getNewFolderName());
        try {
            if (this.repository.exists(token, newFolderURI)) {
                return 101;
            }
            this.repository.createCollection(token, newFolderURI);
            return 0;
        } catch (AuthorizationException e) {
            return 103;
        } catch (Throwable t) {
            return 0;
        }
    }
    
    @SuppressWarnings("unchecked")
    private ModelAndView uploadFile(FCKeditorFileBrowserCommand command, String token, HttpServletRequest request) {
        Map<String, Object> model = new HashMap<String, Object>();
        
        FileItemFactory factory = new DiskFileItemFactory(
            this.maxUploadSize, new File(System.getProperty("java.io.tmpdir")));
        ServletFileUpload upload = new ServletFileUpload(factory);
    
        FileItem uploadItem = null;
        try {
            List<FileItem> fileItems = upload.parseRequest(request);
            for (FileItem item: fileItems) {
                if (!item.isFormField()) {
                    uploadItem = item;
                    break;
                }
            }
            String name = cleanupFileName(uploadItem.getName());
            Path uri = command.getCurrentFolder().extend(name);
            boolean existed = false;
            if (this.repository.exists(token, uri)) {
                existed = true;
                uri = newFileName(command, token, uploadItem);
            }

            InputStream inStream = uploadItem.getInputStream();
            this.repository.createDocument(token, uri, inStream);

            Resource newResource = this.repository.retrieve(token, uri, true);
            TypeInfo typeInfo = this.repository.getTypeInfo(token, uri);
            
            String contentType = uploadItem.getContentType();
            if (contentType == null || MimeHelper.DEFAULT_MIME_TYPE.equals(contentType)) {
                contentType = MimeHelper.map(newResource.getName());
            }

            Property prop = typeInfo.createProperty(
                    Namespace.DEFAULT_NAMESPACE, PropertyType.CONTENTTYPE_PROP_NAME);
            prop.setStringValue(contentType);
            newResource.addProperty(prop);
            this.repository.store(token, newResource);
            
            URL fileURL = this.viewService.constructURL(uri);

            model.put("existed", existed);
            model.put("fileName", name);
            model.put("fileURL", fileURL);
            model.put("error", existed ? 201 : 0);

        } catch (AuthorizationException e) {
            model.put("error", 203);
            
        } catch (Exception e) {
            model.put("error", 1);
            model.put("customMessage", e.getMessage());
        }

        return new ModelAndView(this.uploadStatusViewName, model);
    }
    


    private String ensureTrailingSlash(Path path) {
        if (path.isRoot()) return path.toString();
        return path.toString() + "/";
    }
    
    private Path newFileName(FCKeditorFileBrowserCommand command,
                                   String token, FileItem item) throws Exception {
        
        String name = item.getName();
        Path base = command.getCurrentFolder();

        String extension = "";
        String dot = "";
        int number = 1;

        if (name.endsWith(".")) {
            name = name.substring(0, name.lastIndexOf("."));

        } else if (name.contains(".")) {
            extension = name.substring(name.lastIndexOf(".") + 1, name.length());
            dot = ".";
            name = name.substring(0, name.lastIndexOf("."));
        }
        Path newURI = base.extend(name);
        while (this.repository.exists(token, newURI)) {
            newURI = base.extend(name + "(" + number + ")" + dot + extension);
            number++;
        }
        return  newURI;
    }
    

    static String cleanupFileName(String fileName) {
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


    private String getErrorMessage(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            message = e.getClass().getName();
        }
        return message;
    }
    

    private interface Filter {
        public boolean isAccepted(Resource r);
    }
    

    private static final Filter FILE_FILTER = new Filter() {
        public boolean isAccepted(Resource resource) {
            return !resource.isCollection();
        }
    };

    private static final Filter COLLECTION_FILTER = new Filter() {
        public boolean isAccepted(Resource resource) {
            return resource.isCollection();
        }
    };

    private static final Filter IMAGE_FILTER = new Filter() {
         public boolean isAccepted(Resource resource) {
             return !resource.isCollection() &&
                 resource.getContentType().startsWith("image/");
         }
    };
        
    private static final Filter MEDIA_FILTER = new Filter() {
        public boolean isAccepted(Resource resource) {
            return !resource.isCollection() &&
                (resource.getContentType().startsWith("audio/") 
                        || resource.getContentType().startsWith("video/"));
        }
   };
       
    private static final Filter FLASH_FILTER = new Filter() {
         public boolean isAccepted(Resource resource) {
             return !resource.isCollection() &&
                 resource.getContentType().equalsIgnoreCase("application/x-shockwave-flash");
         }
    };
}
