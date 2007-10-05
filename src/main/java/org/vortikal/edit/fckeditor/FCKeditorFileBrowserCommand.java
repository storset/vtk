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

import javax.servlet.http.HttpServletRequest;


public class FCKeditorFileBrowserCommand {
    private String currentFolder = "/";
    private String newFolderName;
    private Command command;
    private ResourceType type = ResourceType.File;
    
    public static enum Command {
        GetFolders, GetFoldersAndFiles, CreateFolder, FileUpload;
    }

    public static enum ResourceType {
        File, Media, Image, Flash;
    }
    

    public FCKeditorFileBrowserCommand(HttpServletRequest request) {
        String currentFolder = request.getParameter("CurrentFolder");
        if (currentFolder == null) {
            throw new IllegalArgumentException("Missing parameter 'CurrentFolder'");
        }

        if (!"/".equals(currentFolder) && currentFolder.endsWith("/")) {
            currentFolder = currentFolder.substring(0, currentFolder.length() -1);
        }
        this.currentFolder = currentFolder;
        
        String command = request.getParameter("Command");
        if (command == null) {
            command = "GetFoldersAndFiles";
        }
        this.command = Command.valueOf(command);

        String type = request.getParameter("Type");
        if (type == null) {
            type = "File";
        }
        this.type = ResourceType.valueOf(type);

        if (this.command == Command.CreateFolder) {
            this.newFolderName = request.getParameter("NewFolderName");
        }
    }
    
    
    public String getCurrentFolder() {
        return this.currentFolder;
    }

    public Command getCommand() {
        return this.command;
    }

    public ResourceType getResourceType() {
        return this.type;
    }
    
    public String getNewFolderName() {
        return this.newFolderName;
    }
    
}
