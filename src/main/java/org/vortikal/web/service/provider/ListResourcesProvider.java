/* Copyright (c) 2011, University of Oslo, Norway
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
package org.vortikal.web.service.provider;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Acl;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.Path;
import org.vortikal.repository.Privilege;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceNotFoundException;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalFactory;
import org.vortikal.util.repository.ResourceSorter;
import org.vortikal.util.repository.ResourceSorter.Order;

public class ListResourcesProvider {

    private Repository repository;
    private org.springframework.web.servlet.support.RequestContext springRequestContext;

    public List<Resource> buildSearchAndPopulateResources(Path uri, String token,
            HttpServletRequest request) {
    	
    	this.springRequestContext = new org.springframework.web.servlet.support.RequestContext(request);

        Resource[] resourcesArr = null;
        
        try {
        	resourcesArr = this.repository.listChildren(token, uri, false);
		} catch (ResourceNotFoundException e1) {
		} catch (AuthorizationException e1) {
		} catch (AuthenticationException e1) {
		} catch (Exception e1) { }
        
        
        List<Resource> resources = new ArrayList<Resource>();
        
        if(resourcesArr != null) {
        	// Sort by name
        	ResourceSorter.sort(resourcesArr, Order.BY_NAME, false, springRequestContext);
        	for(Resource r : resourcesArr) {
        		resources.add(r);
        	}
        }

        return resources;
    }
    
    public boolean authorizedToRead(Acl acl, Principal principal) {
    	return repository.authorize(principal, acl, Privilege.READ);
    }
    
    public String[] getAclFormatted(Acl acl, HttpServletRequest request) {
    	String[] aclFormatted = {"", "", ""}; // READ, READ_WRITE, ALL
 
        for (Privilege action : Privilege.values()) {
            String actionName = action.getName();
            Principal[] privilegedUsers = acl.listPrivilegedUsers(action);
            Principal[] privilegedGroups = acl.listPrivilegedGroups(action);
            Principal[] privilegedPseudoPrincipals = acl.listPrivilegedPseudoPrincipals(action);
            StringBuilder combined = new StringBuilder();
            int i = 0;
            int len = privilegedPseudoPrincipals.length + privilegedUsers.length + privilegedGroups.length;
            boolean all = false;

            for (Principal p : privilegedPseudoPrincipals) {
                String pseudo = this.getLocalizedTitle(request, "pseudoPrincipal." + p.getName(), null);
                if (p.getName() == PrincipalFactory.NAME_ALL) {
                    all = true;
                    combined.append(pseudo);
                }
                if ((len == 1 || i == len - 1) && !all) {
                    combined.append(pseudo);
                } else if (!all) {
                    combined.append(pseudo + ", ");
                }
                i++;
            }
            if (!all) {
                for (Principal p : privilegedUsers) {
                    if (len == 1 || i == len - 1) {
                        combined.append(p.getDescription());
                    } else {
                        combined.append(p.getDescription() + ", ");
                    }
                    i++;
                }
                for (Principal p : privilegedGroups) {
                    if (len == 1 || i == len - 1) {
                        combined.append(p.getDescription());
                    } else {
                        combined.append(p.getDescription() + ", ");
                    }
                    i++;
                }
            }
            if (actionName == "read") {
            	aclFormatted[0] = combined.toString();
            } else if (actionName == "read-write") {
            	aclFormatted[1] = combined.toString();
            } else if (actionName == "all") {
            	aclFormatted[2] = combined.toString();
            }
        }
        return aclFormatted;
    }


    public String getLocalizedTitle(HttpServletRequest request, String key, Object[] params) {
        if (params != null) {
            return this.springRequestContext.getMessage(key, params);
        }
        return this.springRequestContext.getMessage(key);
    }


    @Required
    public void setRepository(Repository repository) {
        this.repository = repository;
    }
}
