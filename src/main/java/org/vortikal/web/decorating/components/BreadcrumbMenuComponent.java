/* Copyright (c) 2009, University of Oslo, Norway
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
package org.vortikal.web.decorating.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.decorating.DecoratorRequest;
import org.vortikal.web.decorating.DecoratorResponse;
import org.vortikal.web.referencedata.provider.BreadCrumbProvider;
import org.vortikal.web.referencedata.provider.BreadcrumbElement;
import org.vortikal.web.view.components.menu.MenuItem;

public class BreadcrumbMenuComponent extends ListMenuComponent {
    private static final String PARAMETER_AUTENTICATED = "authenticated";
    private static final String PARAMETER_DISPLAY_FROM_LEVEL = "display-from-level";
    private Repository repository;
    private int displayFromLevel = -1;
    private String token;

    public void processModel(Map<Object, Object> model, DecoratorRequest request, DecoratorResponse response)
            throws Exception {
        initRequestParameters(request);
        Path uri = RequestContext.getRequestContext().getResourceURI();
        Principal principal = SecurityContext.getSecurityContext().getPrincipal();

        List<BreadcrumbElement> breadCrumbElements = getBreadcrumbElements();
        if (displayFromLevel > breadCrumbElements.size()) {
            return;
        }
        for (int i = 0; i < displayFromLevel; i++) {
            breadCrumbElements.remove(0);
        }
        Resource currentResource = null;
        try {
            currentResource = repository.retrieve(token, uri, false);
        } catch (Exception e) {
            return; // no access to current resource - can't create menu
        }
        if (!currentResource.isCollection()) {
            try {
                currentResource = repository.retrieve(token, uri.getParent(), false);
            } catch (Exception e) {
                return; // no access to current resource - can't create menu
            }
            if (breadCrumbElements.size() > 0) {
                breadCrumbElements.remove(breadCrumbElements.size() - 1);
            }
        }
        String markedUrl = this.viewService.constructLink(currentResource, principal, false);
        breadCrumbElements.add(new BreadcrumbElement(null, getMenuTitle(currentResource)));

        List<MenuItem<PropertySet>> childElements = null;
        childElements = generateChildElements(currentResource.getChildURIs(), principal);

        // If there is no children of the current resource, then we shall
        // instead display the children of the parent node.
        if (childElements != null && childElements.size() == 0) {
            Resource childResource = repository.retrieve(token, currentResource.getURI().getParent(), false);
            childElements = generateChildElements(childResource.getChildURIs(), principal);
            breadCrumbElements.remove(breadCrumbElements.size() - 1);
        }

        childElements = sortDefaultOrder(childElements, request.getLocale());

        model.put("breadcrumb", breadCrumbElements);
        model.put("children", childElements);
        model.put("markedurl", markedUrl);
    }

    private List<BreadcrumbElement> getBreadcrumbElements() throws Exception {
        String breadcrumbName = "breadcrumb";
        BreadCrumbProvider p = new BreadCrumbProvider();
        p.setSkipCurrentResource(true);
        p.setService(viewService);
        p.setRepository(repository);
        p.setBreadcrumbName(breadcrumbName);
        p.setSkipIndexFile(true);
        PropertyTypeDefinition titleProp[] = new PropertyTypeDefinition[1];
        titleProp[0] = titlePropDef;
        p.setTitleOverrideProperties(titleProp);
        p.afterPropertiesSet();
        Map<String, BreadcrumbElement[]> map = new HashMap<String, BreadcrumbElement[]>();
        p.referenceData(map, RequestContext.getRequestContext().getServletRequest());
        BreadcrumbElement[] list = map.get(breadcrumbName);
        List<BreadcrumbElement> result = new ArrayList<BreadcrumbElement>();
        for (int i = 0; i < list.length; i++) {
            result.add(list[i]);
        }
        return result;
    }

    private List<MenuItem<PropertySet>> generateChildElements(List<Path> children, Principal principal)
            throws Exception {
        List<MenuItem<PropertySet>> items = new ArrayList<MenuItem<PropertySet>>();
        for (Path childPath : children) {
            Resource childResource = null;
            try {
                childResource = repository.retrieve(token, childPath, false);
            } catch (Exception e) {
                continue; // can't access resource - not displayed in menu
            }
            if (!childResource.isCollection()) {
                continue;
            }
            if(childResource.getProperty(this.hiddenPropDef) != null){
                continue; // hidden
            }
            items.add(buildItem(childResource));
        }
        return items;
    }

    private String getMenuTitle(Resource resource) {
        Property prop = resource.getProperty(this.navigationTitlePropDef);
        if (prop != null) {
            return prop.getStringValue();
        }
        return resource.getTitle();
    }

    private void initRequestParameters(DecoratorRequest request) {
        String displayFromLevel = request.getStringParameter(PARAMETER_DISPLAY_FROM_LEVEL);
        if (displayFromLevel != null && !"".equals(displayFromLevel.trim())) {
            int level = Integer.parseInt(displayFromLevel);
            if (level <= 0) {
                throw new DecoratorComponentException("Parameter '" + PARAMETER_DISPLAY_FROM_LEVEL
                        + "' must be an integer > 0");
            }
            this.displayFromLevel = level;
        }
        boolean authenticated = "true".equals(request.getStringParameter(PARAMETER_AUTENTICATED));
        if (authenticated) {
            SecurityContext securityContext = SecurityContext.getSecurityContext();
            this.token = securityContext.getToken();
        }
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public Repository getRepository() {
        return repository;
    }

}
