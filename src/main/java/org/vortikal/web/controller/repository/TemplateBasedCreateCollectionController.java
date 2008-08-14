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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.templates.ResourceTemplate;
import org.vortikal.web.templates.ResourceTemplateManager;


public class TemplateBasedCreateCollectionController extends SimpleFormController
  implements InitializingBean {

	private static final String NORMAL_FOLDER_IDENTIFYER = "NORMAL_FOLDER";
	
	private ResourceTemplateManager templateManager;
	
    private Repository repository = null;
    
    private PropertyTypeDefinition userTitlePropDef;
    private boolean downcaseCollectionNames = false;
    private Map<String, String> replaceNameChars; 
    
    public boolean isDowncaseCollectionNames() {
		return downcaseCollectionNames;
	}

	public void setDowncaseCollectionNames(boolean downcaseCollectionNames) {
		this.downcaseCollectionNames = downcaseCollectionNames;
	}

	public Map<String, String> getReplaceNameChars() {
		return replaceNameChars;
	}

	public void setReplaceNameChars(Map<String, String> replaceNameChars) {
		this.replaceNameChars = replaceNameChars;
	}

	@Required public void setUserTitlePropDef(PropertyTypeDefinition userTitlePropDef) {
		this.userTitlePropDef = userTitlePropDef;
	}

    @Required public void setRepository(Repository repository) {
        this.repository = repository;
    } 

    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        Service service = requestContext.getService();
        
        Resource resource = this.repository.retrieve(securityContext.getToken(),
                                                requestContext.getResourceURI(), false);
        String url = service.constructLink(resource, securityContext.getPrincipal());
         
        CreateCollectionCommand command = new CreateCollectionCommand(url);
        
        String uri = requestContext.getResourceURI();
		String token = SecurityContext.getSecurityContext().getToken();
        
        ArrayList <ResourceTemplate> l = (ArrayList<ResourceTemplate>) templateManager.getFolderTemplates(token, uri);        
        
        // Set first available template as the selected 
        if (!l.isEmpty()) {
        	command.setSourceURI(NORMAL_FOLDER_IDENTIFYER);
        } 
        
        return command;
    }


    @SuppressWarnings("unchecked")
    protected Map referenceData(HttpServletRequest request) throws Exception {
        
        RequestContext requestContext = RequestContext.getRequestContext();        
        SecurityContext securityContext = SecurityContext.getSecurityContext();
    	
    	Map<String, Object> model = new HashMap<String, Object>();
    	
        String uri = requestContext.getResourceURI();
		String token = securityContext.getToken();
				
	    List <ResourceTemplate> l = templateManager.getFolderTemplates(token, uri);
	    
	    LinkedHashMap <String,String> tmp = new LinkedHashMap <String,String>();
	    if(!l.isEmpty()){
	    	tmp.put(NORMAL_FOLDER_IDENTIFYER,"Standard");
	    }
	    
        for (ResourceTemplate t: l) {
        	tmp.put(t.getUri(), t.getTitle());
	    }
        
		       
        model.put("templates", tmp); 
		    	
        return model;
    }
    

    protected void doSubmitAction(Object command) throws Exception {        
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        
        CreateCollectionCommand createFolderCommand =
            (CreateCollectionCommand) command;
        if (createFolderCommand.getCancelAction() != null) {
            createFolderCommand.setDone(true);
            return;
        }
        String uri = requestContext.getResourceURI();
        String token = securityContext.getToken();

        // The location of the folder that we shall copy
        String sourceURI = createFolderCommand.getSourceURI();

        if (sourceURI == null || sourceURI.equals(NORMAL_FOLDER_IDENTIFYER)){ // Just create a new folder if no "folder-template" is selected
        	createNewFolder(command,uri,token);
            createFolderCommand.setDone(true);            
            return;
        }

        // Setting the destination to the current folder/uri
        String destinationURI = uri;
        if (!"/".equals(uri)) destinationURI += "/";
        destinationURI += fixCollectionName(createFolderCommand.getName());
        
       
        // Copy folder-template to destination (implicit rename) 
        this.repository.copy(token, sourceURI, destinationURI, "0", false, false);
        Resource dest = this.repository.retrieve(token, destinationURI, false);
        dest.removeProperty(this.userTitlePropDef);
        this.repository.store(token, dest);
        
        createFolderCommand.setDone(true);
        
    }
    
    private void createNewFolder(Object command, String uri, String token) throws Exception{
    	CreateCollectionCommand createCollectionCommand = (CreateCollectionCommand) command;
    	String newURI = uri;
    	
        if (!"/".equals(uri)) newURI += "/";
        String title = createCollectionCommand.getName();
        String name = fixCollectionName(title);
        newURI += name;
        Resource collection = this.repository.createCollection(token, newURI);
        if (!title.equals(name)) {
            Property titleProp = collection.createProperty(this.userTitlePropDef);
            titleProp.setStringValue(title);
            this.repository.store(token, collection);
        }
    }

	public void setTemplateManager(ResourceTemplateManager templateManager) {
		this.templateManager = templateManager;
	}

	public void afterPropertiesSet() throws Exception {
		// TODO Auto-generated method stub
		
	}
	
   private String fixCollectionName(String name) {
        if (this.downcaseCollectionNames) {
            name = name.toLowerCase();
        }
        if (this.replaceNameChars != null) {
            for (String regex: this.replaceNameChars.keySet()) {
                String replacement = this.replaceNameChars.get(regex);
                name = name.replaceAll(regex, replacement);
            }
        }
        return name;
    }
   
   @Override
   protected void onBindAndValidate(HttpServletRequest request,
           Object command, BindException errors) throws Exception {
       super.onBindAndValidate(request, command, errors);
       RequestContext requestContext = RequestContext.getRequestContext();
       SecurityContext securityContext = SecurityContext.getSecurityContext();
       
       CreateCollectionCommand createCollectionCommand =
           (CreateCollectionCommand) command;
       if (createCollectionCommand.getCancelAction() != null) {
           return;
       }
       String uri = requestContext.getResourceURI();
       String token = securityContext.getToken();

       String name = createCollectionCommand.getName();       
       if (null == name || "".equals(name.trim())) {
           errors.rejectValue("name",
                              "manage.create.collection.missing.name",
                              "A name must be provided for the collection");
           return;
       }

       if (name.indexOf("/") >= 0) {
           errors.rejectValue("name",
                              "manage.create.collection.invalid.name",
                              "This is an invalid collection name");
       }
       name = fixCollectionName(name);
       String newURI = uri;
       if (!"/".equals(uri)) newURI += "/";
       newURI += name;

       boolean exists = this.repository.exists(token, newURI);
       if (exists) {
           errors.rejectValue("name",
                   "manage.create.collection.exists",
           "A collection with this name already exists");
       }
       
       if (uri.startsWith(newURI)) {
           errors.rejectValue("name",
                   "manage.create.collection.invalid.destination",
                   "Cannot copy a collection into itself");
       }
   }

}

