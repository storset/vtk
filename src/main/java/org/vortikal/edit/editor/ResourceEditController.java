/* Copyright (c) 2007, 2012 University of Oslo, Norway
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
package org.vortikal.edit.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.validation.Errors;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.vortikal.repository.Privilege;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceWrapper;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.security.Principal;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.ServiceUnlinkableException;

public class ResourceEditController extends SimpleFormController {
    protected ResourceWrapperManager resourceManager;
    protected List<Service> tooltipServices;
    protected Map<PropertyTypeDefinition, PropertyEditPreprocessor> propertyEditPreprocessors;
    protected Locale defaultLocale;

    public ResourceEditController() {
        super();
        setCommandName("resource");
    }

    protected ResourceWrapperManager getResourceManager() {
        return this.resourceManager;
    }

    @Override
    protected ModelAndView onSubmit(Object command) throws Exception {
        ResourceEditWrapper wrapper = (ResourceEditWrapper) command;
        Resource resource = wrapper.getResource();
        RequestContext requestContext = RequestContext.getRequestContext();
        Principal principal = requestContext.getPrincipal();
        Repository repository = requestContext.getRepository();
        String token = requestContext.getSecurityToken();

        if (wrapper.hasErrors()) {
            Map<String, Object> model = getModelProperties(command, resource, principal, repository);
            return new ModelAndView(getFormView(), model);
        }

        if (!wrapper.isSave()) {
            this.resourceManager.unlock();
            return new ModelAndView(getSuccessView(), new HashMap<String, Object>());
        }

        this.resourceManager.store(wrapper);

        if (!wrapper.isView()) {
            Map<String, Object> model = getModelProperties(command, resource, principal, repository);
            wrapper.setSave(false);
            return new ModelAndView(getFormView(), model);
        }

        this.resourceManager.unlock();
        return super.onSubmit(command);
    }

    protected Map<String, Object> getModelProperties(Object command, Resource resource, Principal principal,
            Repository repository) {
        Map<String, Object> model = new HashMap<String, Object>();
        model.put(getCommandName(), command);
        model.put("published", resource.isPublished());
        model.put("hasPublishDate", resource.hasPublishDate());
        model.put("onlyWriteUnpublished", !repository.authorize(principal, resource.getAcl(), Privilege.READ_WRITE));
        model.put("defaultLocale", this.defaultLocale);
        return model;
    }

    @Override
    protected ServletRequestDataBinder createBinder(HttpServletRequest request, Object command) throws Exception {
        ServletRequestDataBinder binder = new ResourceEditDataBinder(command, getCommandName(),
                resourceManager.getHtmlParser(), resourceManager.getHtmlPropsFilter(), propertyEditPreprocessors);
        prepareBinder(binder);
        initBinder(request, binder);
        return binder;
    }

    @Override
    protected Object formBackingObject(HttpServletRequest request) throws Exception {

        resourceManager.lock();
        return resourceManager.createResourceEditWrapper();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected Map referenceData(HttpServletRequest request, Object command, Errors errors) throws Exception {
        Resource resource = ((ResourceWrapper) command).getResource();
        RequestContext requestContext = RequestContext.getRequestContext();
        Principal principal = requestContext.getPrincipal();
        Repository repository = requestContext.getRepository();

        Map model = super.referenceData(request, command, errors);

        if (model == null) {
            model = new HashMap();
        }

        model.put("published", resource.isPublished());
        model.put("hasPublishDate", resource.hasPublishDate());
        model.put("onlyWriteUnpublished", !repository.authorize(principal, resource.getAcl(), Privilege.READ_WRITE));
        model.put("defaultLocale", this.defaultLocale);
        model.put("tooltips", resolveTooltips(resource, principal));

        return model;
    }

    public void setTooltipServices(List<Service> tooltipServices) {
        this.tooltipServices = tooltipServices;
    }

    private List<Map<String, String>> resolveTooltips(Resource resource, Principal principal) {
        if (this.tooltipServices == null) {
            return null;
        }
        List<Map<String, String>> tooltips = new ArrayList<Map<String, String>>();
        for (Service service : this.tooltipServices) {
            String url = null;
            try {
                url = service.constructLink(resource, principal);
                Map<String, String> tooltip = new HashMap<String, String>();
                tooltip.put("url", url);
                tooltip.put("messageKey", "plaintextEdit.tooltip." + service.getName());
                tooltips.add(tooltip);
            } catch (ServiceUnlinkableException e) {
                // Ignore
            }
        }
        return tooltips;
    }

    @Required
    public void setResourceManager(ResourceWrapperManager resourceManager) {
        this.resourceManager = resourceManager;
    }
    
    @Required
    public void setDefaultLocale(Locale defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public void setPropertyEditPreprocessors(
            Map<PropertyTypeDefinition, PropertyEditPreprocessor> propertyEditPreprocessors) {
        this.propertyEditPreprocessors = propertyEditPreprocessors;
    }

}
