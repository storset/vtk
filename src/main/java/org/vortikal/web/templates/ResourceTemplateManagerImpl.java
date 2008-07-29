/* Copyright (c) 2008, University of Oslo, Norway
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
package org.vortikal.web.templates;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Repository;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.repository.PropertiesResource;
import org.vortikal.web.RequestContext;
import org.vortikal.web.templates.ResourceTemplateManager;

/**
 * Main template manager implementation.
 * 
 * Currently also considers folder templates, but that may change, if we
 * decide to put that somewhere else.
 * 
 */
public class ResourceTemplateManagerImpl implements ResourceTemplateManager {

    // Resource template locator
    private ResourceTemplateLocator templateLocator;
    
    // Configuration for document templates
    private String documentTemplatesBaseUri;
    private PropertiesResource documentTemplatesConfiguration;

    // Default resource type for document templates
    private ResourceTypeDefinition documentTemplateResourceType;
    
    // Configuration for folder templates
    private String folderTemplatesBaseUri;
    private PropertiesResource folderTemplatesConfiguration;

    // Default resource type for folder templates
    private ResourceTypeDefinition folderTemplateResourceType;
    
    private String documentTemplatesDefaultUri;
    private String folderTemplatesDefaultUri;
    
    /**
     * @see ResourceTemplateManager#getDocumentTemplates(String, String) 
     */
    public List<ResourceTemplate> getDocumentTemplates(String token, String uri) {

    	Set <String> h = this.getDocumentTemplateBaseUris(uri) ;   	 	    	
    	return templateLocator.findTemplates(token, h, documentTemplateResourceType);
    	
    }

    /**
     * @see ResourceTemplateManager#getFolderTemplates(String, String)
     */
    public List<ResourceTemplate> getFolderTemplates(String token, String uri) {
        
        Set<String> baseUris = this.getFolderTemplateBaseUris(uri);
        
        return templateLocator.findTemplatesNonRecursively(token, 
                                    baseUris, this.folderTemplateResourceType);
    }

    @SuppressWarnings("unchecked")
	private Set<String> getDocumentTemplateBaseUris(String uri) {
     
        // Read/parse configuration, find matching prefix and return list of base uris
        // to use with locator.
    	
		HashSet <String> foundTemplateBaseUris = new HashSet <String> ();		
		String[] templateLocations = null;
		
		try {
			
			// Runs through the property file trying to match property key and uri
			// stops after the first hit
			String keyInPropertyFile = "/";
			for (Enumeration <String> e = (Enumeration<String>) documentTemplatesConfiguration.propertyNames(); e.hasMoreElements() ;) {
				String propertyKey = e.nextElement();
				if( propertyKey.startsWith(uri + "/") ){ //uri does not contain the last slash
					keyInPropertyFile = propertyKey;
					break;
				}
		     }
			
			// A key can point to multiple template folders that is separated by ","
			String tmp = documentTemplatesConfiguration.getProperty(keyInPropertyFile);
			if(tmp != null){
				templateLocations = tmp.split(","); 
			}
			
			// Mapping the found template values to folders in the vortex file system
			if(templateLocations != null){
				for(int i = 0; i < templateLocations.length;i++){
					foundTemplateBaseUris.add( documentTemplatesBaseUri + "/" + templateLocations[i].trim() );
				}
			}else{
				foundTemplateBaseUris.add(documentTemplatesDefaultUri); // No config.txt is found
			}
			
    	}catch (Exception e){
			e.printStackTrace();
		}
		
        return foundTemplateBaseUris;
    }
    
    private Set<String> getFolderTemplateBaseUris(String uri) {
        // Not implemented, for now
        return null;
    }

    @Required
    public void setTemplateLocator(ResourceTemplateLocator templateLocator) {
        this.templateLocator = templateLocator;
    }

    @Required
    public void setDocumentTemplatesConfiguration(
            PropertiesResource documentTemplatesConfiguration) {
        this.documentTemplatesConfiguration = documentTemplatesConfiguration;
    }

    @Required
    public void setDocumentTemplatesBaseUri(String documentTemplatesBaseUri) {
        this.documentTemplatesBaseUri = documentTemplatesBaseUri;
    }
    
    @Required
    public void setDocumentTemplatesResourceType(ResourceTypeDefinition def) {
        this.documentTemplateResourceType = def;
    }

    // Folder templates below (not required, for now)

    @Required
    public void setFolderTemplatesConfiguration(PropertiesResource folderTemplatesConfiguration) {
        this.folderTemplatesConfiguration = folderTemplatesConfiguration;
    }
    
    @Required
    public void setFolderTemplatesBaseUri(String folderTemplatesBaseUri) {
        this.folderTemplatesBaseUri = folderTemplatesBaseUri;
    }

    @Required
    public void setFolderTemplateResourceType(
            ResourceTypeDefinition folderTemplateResourceType) {
        this.folderTemplateResourceType = folderTemplateResourceType;
    }

    @Required
	public void setDocumentTemplatesDefaultUri(String documentTemplatesDefaultUri) {
		this.documentTemplatesDefaultUri = documentTemplatesDefaultUri;
	}

    @Required
    public void setFolderTemplatesDefaultUri(String folderTemplatesDefaultUri) {
        this.folderTemplatesDefaultUri = folderTemplatesDefaultUri;
    }
    
    

}
