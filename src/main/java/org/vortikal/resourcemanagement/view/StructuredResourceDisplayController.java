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
package org.vortikal.resourcemanagement.view;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Path;
import org.vortikal.repository.PropertyImpl;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFormatterRegistry;
import org.vortikal.repository.search.QueryParserFactory;
import org.vortikal.repository.search.Searcher;
import org.vortikal.resourcemanagement.ComponentDefinition;
import org.vortikal.resourcemanagement.StructuredResource;
import org.vortikal.resourcemanagement.StructuredResourceDescription;
import org.vortikal.resourcemanagement.StructuredResourceManager;
import org.vortikal.resourcemanagement.view.tl.ComponentInvokerNodeFactory;
import org.vortikal.resourcemanagement.view.tl.JSONAttributeHandler;
import org.vortikal.resourcemanagement.view.tl.JSONDocumentProvider;
import org.vortikal.resourcemanagement.view.tl.LocalizationNodeFactory;
import org.vortikal.resourcemanagement.view.tl.ModelConfigHandler;
import org.vortikal.resourcemanagement.view.tl.PropertyValueFormatHandler;
import org.vortikal.resourcemanagement.view.tl.ResourcePropHandler;
import org.vortikal.resourcemanagement.view.tl.ResourcePropObjectValueHandler;
import org.vortikal.resourcemanagement.view.tl.ResourcePropsNodeFactory;
import org.vortikal.resourcemanagement.view.tl.RetrieveHandler;
import org.vortikal.resourcemanagement.view.tl.SearchResultValueProvider;
import org.vortikal.resourcemanagement.view.tl.ViewURLValueProvider;
import org.vortikal.security.SecurityContext;
import org.vortikal.text.html.HtmlPage;
import org.vortikal.text.html.HtmlPageFilter;
import org.vortikal.text.html.HtmlPageParser;
import org.vortikal.text.tl.DefineNodeFactory;
import org.vortikal.text.tl.DirectiveNodeFactory;
import org.vortikal.text.tl.IfNodeFactory;
import org.vortikal.text.tl.ListNodeFactory;
import org.vortikal.text.tl.ValNodeFactory;
import org.vortikal.util.io.StreamUtil;
import org.vortikal.web.RequestContext;
import org.vortikal.web.decorating.ComponentResolver;
import org.vortikal.web.decorating.HtmlPageContent;
import org.vortikal.web.decorating.ParsedHtmlDecoratorTemplate;
import org.vortikal.web.decorating.Template;
import org.vortikal.web.decorating.TemplateManager;
import org.vortikal.web.referencedata.ReferenceDataProvider;
import org.vortikal.web.service.Service;

public class StructuredResourceDisplayController implements Controller, InitializingBean {

    public static final String MVC_MODEL_KEY = "__mvc_model__";
    public static final String TEMPLATE_EXECUTION_REQ_ATTR = "__template_execution_";

    private static final String COMPONENT_NS = "comp";
    
    private Repository repository;
    private QueryParserFactory queryParserFactory;
    private Searcher searcher;
    private String viewName;
    private Service viewService;
    private StructuredResourceManager resourceManager;
    private TemplateManager templateManager;
    private HtmlPageParser htmlParser;
    private String resourceModelKey;
    private List<ReferenceDataProvider> configProviders;
    private ValueFormatterRegistry valueFormatterRegistry;

    private Map<String, DirectiveNodeFactory> directiveHandlers;

    private HtmlPageFilter postFilter;

    // XXX: clean up this mess:
    private Map<StructuredResourceDescription, Map<String, TemplateLanguageDecoratorComponent>> components = new ConcurrentHashMap<StructuredResourceDescription, Map<String, TemplateLanguageDecoratorComponent>>();

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Path uri = RequestContext.getRequestContext().getResourceURI();
        String token = SecurityContext.getSecurityContext().getToken();
        Resource r = this.repository.retrieve(token, uri, true);

        InputStream stream = this.repository.getInputStream(token, uri, true);
        byte[] buff = StreamUtil.readInputStream(stream);
        String encoding = "utf-8";
        String source = new String(buff, encoding);

        StructuredResourceDescription desc = this.resourceManager.get(r.getResourceType());
        if (desc == null) {
            throw new IllegalStateException("Unable to find description '" + r.getResourceType() + "' for resource "
                    + r.getURI());
        }
        if (!desc.getComponentDefinitions().isEmpty() && !this.components.containsKey(desc)) {
            initComponentDefs(desc);
        }

        Map<String, Object> model = new HashMap<String, Object>();
        StructuredResource res = new StructuredResource(desc);
        res.parse(source);
        model.put("structured-resource", res);
        model.put("resource", r);
        model.put(this.resourceModelKey, res);

        if (this.configProviders != null) {
            Map<String, Object> config = new HashMap<String, Object>();
            for (ReferenceDataProvider p : this.configProviders) {
                p.referenceData(config, request);
            }
            model.put("config", config);
        }

        HtmlPageContent content = renderInitialPage(res, model, request);
        
        HtmlPage page = content.getHtmlContent();
        if (this.postFilter != null) {
            page.filter(this.postFilter);
        }
        
        model.put("page", content.getHtmlContent());
        return new ModelAndView(this.viewName, model);
    }

    @SuppressWarnings("unchecked")
    public HtmlPageContent renderInitialPage(StructuredResource res, Map model, HttpServletRequest request)
            throws Exception {

        String html = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n";
        html += "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n";
        html += "<head><title></title></head><body></body></html>";
        ByteArrayInputStream in = new ByteArrayInputStream(html.getBytes("utf-8"));
        final HtmlPage dummy = this.htmlParser.parse(in, "utf-8");

        HtmlPageContent content = new HtmlPageContent() {
            public HtmlPage getHtmlContent() {
                return dummy;
            }

            public String getContent() {
                return dummy.getStringRepresentation();
            }

            public String getOriginalCharacterEncoding() {
                return dummy.getCharacterEncoding();
            }
        };

        String templateRef = res.getType().getName();
        Template t = this.templateManager.getTemplate(templateRef);

        if (!(t instanceof ParsedHtmlDecoratorTemplate)) {
            throw new IllegalStateException("Template must be of class " + ParsedHtmlDecoratorTemplate.class.getName());
        }
        ParsedHtmlDecoratorTemplate template = (ParsedHtmlDecoratorTemplate) t;
        ParsedHtmlDecoratorTemplate.Execution execution = (ParsedHtmlDecoratorTemplate.Execution) template
                .newTemplateExecution(content, request, model);

        ComponentResolver resolver = execution.getComponentResolver();
        Map<String, TemplateLanguageDecoratorComponent> components = this.components.get(res.getType());

        execution.setComponentResolver(new DynamicComponentResolver(COMPONENT_NS, resolver, components));
        request.setAttribute(TEMPLATE_EXECUTION_REQ_ATTR, execution);
        content = (HtmlPageContent) execution.render();
        return content;
    }

    private void initComponentDefs(StructuredResourceDescription desc) throws Exception {
        // XXX: "concurrent initialization":

        Map<String, TemplateLanguageDecoratorComponent> comps = new HashMap<String, TemplateLanguageDecoratorComponent>();

        List<ComponentDefinition> defs = desc.getAllComponentDefinitions();
        for (ComponentDefinition def : defs) {
            String name = def.getName();
            TemplateLanguageDecoratorComponent comp = 
                new TemplateLanguageDecoratorComponent(COMPONENT_NS, def, MVC_MODEL_KEY,
                        this.directiveHandlers, this.htmlParser);
            comps.put(name, comp);
        }
        this.components.put(desc, comps);
    }

    public void afterPropertiesSet() {

        // TODO: inject directive handlers instead of 
        // initializing all of them here:

        Map<String, DirectiveNodeFactory> directiveHandlers = new HashMap<String, DirectiveNodeFactory>();
        directiveHandlers.put("if", new IfNodeFactory());

        ValNodeFactory val = new ValNodeFactory();
        val.addValueFormatHandler(PropertyImpl.class, new PropertyValueFormatHandler(this.valueFormatterRegistry));
        val.addValueFormatHandler(Value.class, new PropertyValueFormatHandler(this.valueFormatterRegistry));
        val.addValueFormatHandler(Value[].class, new PropertyValueFormatHandler(this.valueFormatterRegistry));
        directiveHandlers.put("val", val);

        directiveHandlers.put("list", new ListNodeFactory());
        directiveHandlers.put("resource-props", new ResourcePropsNodeFactory(this.repository));
        DefineNodeFactory def = new DefineNodeFactory();
        def.addValueProvider("resource", new RetrieveHandler(this.repository));
        def.addValueProvider("resource-prop", new ResourcePropHandler(this.repository));
        def.addValueProvider("resource-prop-obj-val", new ResourcePropObjectValueHandler(this.repository)); // Hmm, better solution ?
        def.addValueProvider("config", new ModelConfigHandler());
        def.addValueProvider("structured-document", new JSONDocumentProvider());
        def.addValueProvider("json-attr", new JSONAttributeHandler());
        def.addValueProvider("search", new SearchResultValueProvider(this.queryParserFactory, this.searcher));
        def.addValueProvider("view-url", new ViewURLValueProvider(this.viewService));
        directiveHandlers.put("def", def);

        directiveHandlers.put("localized", new LocalizationNodeFactory(this.resourceModelKey));
        directiveHandlers.put("call-component", new ComponentInvokerNodeFactory(this.htmlParser));
        
        this.directiveHandlers = directiveHandlers;

        List<StructuredResourceDescription> allDescriptions = this.resourceManager.list();

        for (StructuredResourceDescription desc : allDescriptions) {
            try {
                initComponentDefs(desc);
            } catch (Exception e) {
                throw new BeanInitializationException("Unable to initialize component definitions "
                        + "for resource type " + desc, e);
            }
        }
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setQueryParserFactory(QueryParserFactory queryParserFactory) {
        this.queryParserFactory = queryParserFactory;
    }

    public void setSearcher(Searcher searcher) {
        this.searcher = searcher;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public void setResourceManager(StructuredResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    public void setViewService(Service viewService) {
        this.viewService = viewService;
    }

    @Required
    public void setTemplateManager(TemplateManager templateManager) {
        this.templateManager = templateManager;
    }

    @Required
    public void setHtmlParser(HtmlPageParser htmlParser) {
        this.htmlParser = htmlParser;
    }

    @Required
    public void setResourceModelKey(String resourceModelKey) {
        this.resourceModelKey = resourceModelKey;
    }

    public void setConfigProviders(List<ReferenceDataProvider> configProviders) {
        this.configProviders = configProviders;
    }

    @Required
    public void setValueFormatterRegistry(ValueFormatterRegistry valueFormatterRegistry) {
        this.valueFormatterRegistry = valueFormatterRegistry;
    }

    public void setPostFilter(HtmlPageFilter postFilter) {
        this.postFilter = postFilter;
    }
}
