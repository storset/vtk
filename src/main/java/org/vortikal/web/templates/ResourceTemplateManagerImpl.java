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

import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import org.vortikal.util.repository.PropertiesResource;

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
    private Path documentTemplatesBaseUri;
    private PropertiesResource documentTemplatesConfiguration;

    // Default set of resource types for document templates
    private Set<ResourceTypeDefinition> documentTemplateResourceTypes;
    
    // Configuration for folder templates
    private Path folderTemplatesBaseUri;
    private PropertiesResource folderTemplatesConfiguration;

    // Default set of resource types for folder templates
    private Set<ResourceTypeDefinition> folderTemplateResourceTypes;
    
    private Path documentTemplatesDefaultUri;
    private Path folderTemplatesDefaultUri;
    
    /**
     * @see ResourceTemplateManager#getDocumentTemplates(String, String) 
     */
    public List<ResourceTemplate> getDocumentTemplates(String token, Path uri) {

    	Set <Path> baseUris = this.getDocumentTemplateBaseUris(uri);
    	
    	return templateLocator.findTemplates(token, baseUris, documentTemplateResourceTypes);
    	
    }

    /**
     * @see ResourceTemplateManager#getFolderTemplates(String, String)
     */
    public List<ResourceTemplate> getFolderTemplates(String token, Path uri) {
        
        Set<Path> baseUris = this.getFolderTemplateBaseUris(uri);

        return templateLocator.findTemplatesNonRecursively(token, 
                                    baseUris, this.folderTemplateResourceTypes);
    }
    
    private Set<Path> getDocumentTemplateBaseUris(Path uri) {
        
        return getBaseUris(uri, this.documentTemplatesConfiguration,
                                this.documentTemplatesBaseUri,
                                this.documentTemplatesDefaultUri);
    }
     
    private Set<Path> getFolderTemplateBaseUris(Path uri) {
        
        return getBaseUris(uri, this.folderTemplatesConfiguration,
                                this.folderTemplatesBaseUri,
                                this.folderTemplatesDefaultUri);
    }
    
    private Set<Path> getBaseUris(Path uri, 
                                    Properties config, 
                                    Path templatesBase,
                                    Path defaultBaseUri) {
        
        Set<Path> baseUris = new HashSet<Path>();
        
        // Try direct match from config first
        String matchValue = config.getProperty(uri.toString());
        if (matchValue == null) {
            matchValue = config.getProperty(uri + "/");
        }
        
        // If no direct match, try ancestor URIs upwards until we find a match
        if (matchValue == null) {
            List<Path> ancestors = uri.getAncestors();
            for (int i = ancestors.size()-1; i >= 0; i--) {
                Path ancestorUri = ancestors.get(i);
                matchValue = config.getProperty(ancestorUri.toString());
                if (matchValue == null) {
                    matchValue = config.getProperty(ancestorUri + "/");
                }
                if (matchValue != null) break; // Found a match
            }
        }
        
        if (matchValue != null) {
            // OK, something is configured for the given URI, parse the value
            StringTokenizer tokens = new StringTokenizer(matchValue, ",");
            while (tokens.hasMoreElements()) {
                Path baseUri = Path.fromString(templatesBase + "/" + tokens.nextToken().trim());
                baseUris.add(baseUri);
            }
        } else {
            // Nothing configured, return default base URI
            baseUris.add(defaultBaseUri);
        }
        
        return baseUris;
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
        this.documentTemplatesBaseUri = Path.fromString(documentTemplatesBaseUri);
    }
    
    @Required
    public void setDocumentTemplatesResourceTypes(Set<ResourceTypeDefinition> def) {
        this.documentTemplateResourceTypes = def;
    }

    // Folder templates below (not required, for now)

    @Required
    public void setFolderTemplatesConfiguration(PropertiesResource folderTemplatesConfiguration) {
        this.folderTemplatesConfiguration = folderTemplatesConfiguration;
    }
    
    @Required
    public void setFolderTemplatesBaseUri(String folderTemplatesBaseUri) {
        this.folderTemplatesBaseUri = Path.fromString(folderTemplatesBaseUri);
    }

    @Required
    public void setFolderTemplateResourceType(
            Set<ResourceTypeDefinition> folderTemplateResourceTypes) {
        this.folderTemplateResourceTypes = folderTemplateResourceTypes;
    }

    @Required
	public void setDocumentTemplatesDefaultUri(String documentTemplatesDefaultUri) {
		this.documentTemplatesDefaultUri = Path.fromString(documentTemplatesDefaultUri);
	}

    @Required
    public void setFolderTemplatesDefaultUri(String folderTemplatesDefaultUri) {
        this.folderTemplatesDefaultUri = Path.fromString(folderTemplatesDefaultUri);
    }
    
    

}
