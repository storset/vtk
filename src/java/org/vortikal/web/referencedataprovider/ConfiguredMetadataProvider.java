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
package org.vortikal.web.referencedataprovider;


import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.web.URLUtil;
import org.vortikal.web.RequestContext;

/**
 * Model builder that provides configured metadata based on the resource url. 
 */

public class ConfiguredMetadataProvider implements Provider {
    private static Log logger = LogFactory.getLog(ConfiguredMetadataProvider.class);
	private Repository repository = null;
	
	public final Repository getRepository() {
        return repository;
    }

    public final void setRepository(final Repository newRepository) {
        this.repository = newRepository;
    }

    public void referenceData(Map model, HttpServletRequest request) {

    	    Map configuredMetadataModel = new LinkedHashMap();	 	       
 
    	    SecurityContext securityContext = SecurityContext.getSecurityContext();
        String token = securityContext.getToken();
        Principal principal = securityContext.getPrincipal();
        RequestContext requestContext = RequestContext.getRequestContext();
        String uri = requestContext.getResourceURI();

        String[] path = URLUtil.splitUri(uri);
        String[] incrementalPath = URLUtil.splitUriIncrementally(uri);

        logger.debug("Antall nivŒer: " + path.length);
        
        try {
            for (int i = path.length - 2; i >= 0; i--) {	
            	   Resource r = repository.retrieve(token, incrementalPath[i], true);

                // "Tema" is obligatory for UHTML	
                Property tema = r.getProperty("http://www.uio.no/visuell-profil","tema");
                
                	if (tema != null) {
                		logger.debug("Fant tema: " + tema.getValue());
                		Property enhet_id = r.getProperty("http://www.uio.no/visuell-profil","enhet.id");
                		Property redaksjon_navn = r.getProperty("http://www.uio.no/visuell-profil","redaksjon.navn");
                		Property redaksjon_vev = r.getProperty("http://www.uio.no/visuell-profil","redaksjon.vev");
                		Property redaksjon_epost = r.getProperty("http://www.uio.no/visuell-profil","redaksjon.epost");
                		
                		configuredMetadataModel.put("tema", tema.getValue());
                		
                		if (enhet_id != null) {
                			configuredMetadataModel.put("enhet_id", enhet_id.getValue());
                		}
                		if (redaksjon_navn != null) {
                			configuredMetadataModel.put("redaksjon_navn", redaksjon_navn.getValue());
                		}
                   	if (redaksjon_vev != null) {
                			configuredMetadataModel.put("redaksjon_vev", redaksjon_vev.getValue());
                		}
                   	if (redaksjon_epost != null) {
            			configuredMetadataModel.put("redaksjon_epost", redaksjon_epost.getValue());
                   	}	
                   	
                		break;
                		}
                	}		
            }	
        catch (Exception e) {
        		logger.warn("Unable to get possible metadata from ancestor(s) ", e);
        	}
        
        model.put("configuredMetadata", configuredMetadataModel);
    }

}
