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
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertyImpl;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceNotFoundException;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFormatter;
import org.vortikal.repository.resourcetype.ValueFormatterRegistry;
import org.vortikal.repository.search.QueryParserFactory;
import org.vortikal.repository.search.ResultSet;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.Searcher;
import org.vortikal.repository.search.query.Query;
import org.vortikal.resourcemanagement.ComponentDefinition;
import org.vortikal.resourcemanagement.StructuredResource;
import org.vortikal.resourcemanagement.StructuredResourceDescription;
import org.vortikal.resourcemanagement.StructuredResourceManager;
import org.vortikal.security.SecurityContext;
import org.vortikal.text.JSONUtil;
import org.vortikal.text.html.HtmlNodeFilter;
import org.vortikal.text.html.HtmlPage;
import org.vortikal.text.html.HtmlPageParser;
import org.vortikal.text.tl.Argument;
import org.vortikal.text.tl.Context;
import org.vortikal.text.tl.DefineNodeFactory;
import org.vortikal.text.tl.DirectiveNodeFactory;
import org.vortikal.text.tl.DirectiveParseContext;
import org.vortikal.text.tl.IfNodeFactory;
import org.vortikal.text.tl.ListNodeFactory;
import org.vortikal.text.tl.Literal;
import org.vortikal.text.tl.Node;
import org.vortikal.text.tl.Symbol;
import org.vortikal.text.tl.ValNodeFactory;
import org.vortikal.util.io.StreamUtil;
import org.vortikal.web.RequestContext;
import org.vortikal.web.decorating.ComponentResolver;
import org.vortikal.web.decorating.DecoratorComponent;
import org.vortikal.web.decorating.HtmlPageContent;
import org.vortikal.web.decorating.ParsedHtmlDecoratorTemplate;
import org.vortikal.web.decorating.Template;
import org.vortikal.web.decorating.TemplateManager;
import org.vortikal.web.referencedata.ReferenceDataProvider;
import org.vortikal.web.service.Service;

public class StructuredResourceDisplayController implements Controller, InitializingBean {

    private static final String MVC_MODEL_KEY = "__mvc_model__";

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

    private List<HtmlNodeFilter> parseFilters;

    public void setParseFilters(List<HtmlNodeFilter> parseFilters) {
        this.parseFilters = parseFilters;
    }

    // XXX: clean up this mess:
    private Map<StructuredResourceDescription, Map<String, TemplateLanguageDecoratorComponent>> components = new ConcurrentHashMap<StructuredResourceDescription, Map<String, TemplateLanguageDecoratorComponent>>();

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Path uri = RequestContext.getRequestContext().getResourceURI();
        String token = SecurityContext.getSecurityContext().getToken();
        Resource r = this.repository.retrieve(token, uri, true);

        InputStream stream = this.repository.getInputStream(token, uri, true);
        byte[] buff = StreamUtil.readInputStream(stream);
        String encoding = r.getCharacterEncoding();
        if (encoding == null)
            encoding = "utf-8";
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

        // XXX This is a most unwanted solution. We parse the entire document
        // after it's already been parsed once. This can cause recursive
        // invocation of components, e.g. the first round of parsing results in
        // output which contains a component definition, which is parsed and
        // resolved during the second pass.
        byte[] bytes = content.getHtmlContent().getStringRepresentation().getBytes();
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        HtmlPage page = this.htmlParser.parse(bais, encoding, this.parseFilters);
        bais.close();

        model.put("page", page);
        return new ModelAndView(this.viewName, model);
    }

    @SuppressWarnings("unchecked")
    public HtmlPageContent renderInitialPage(StructuredResource res, Map model, HttpServletRequest request)
            throws Exception {
        String html = "<html><head><title></title></head><body></body></html>";
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

        final ComponentResolver resolver = execution.getComponentResolver();
        final Map<String, TemplateLanguageDecoratorComponent> components = this.components.get(res.getType());

        execution.setComponentResolver(new ComponentResolver() {
            public List<DecoratorComponent> listComponents() {
                return null;
            }

            public DecoratorComponent resolveComponent(String namespace, String name) {
                if ("comp".equals(namespace)) {
                    if (components == null) {
                        return null;
                    }
                    return components.get(name);
                }
                return resolver.resolveComponent(namespace, name);
            }
        });
        content = (HtmlPageContent) execution.render();
        return content;
    }

    private void initComponentDefs(StructuredResourceDescription desc) throws Exception {
        // XXX: "concurrent initialization":

        Map<String, TemplateLanguageDecoratorComponent> comps = new HashMap<String, TemplateLanguageDecoratorComponent>();

        List<ComponentDefinition> defs = desc.getAllComponentDefinitions();
        for (ComponentDefinition def : defs) {
            String name = def.getName();
            TemplateLanguageDecoratorComponent comp = new TemplateLanguageDecoratorComponent(def, MVC_MODEL_KEY,
                    this.directiveHandlers, this.htmlParser);
            comps.put(name, comp);
        }
        this.components.put(desc, comps);
    }

    public void afterPropertiesSet() {
        Map<String, DirectiveNodeFactory> directiveHandlers = new HashMap<String, DirectiveNodeFactory>();
        directiveHandlers.put("if", new IfNodeFactory());

        ValNodeFactory val = new ValNodeFactory();
        val.addValueFormatHandler(PropertyImpl.class, new PropertyValueFormatHandler());
        val.addValueFormatHandler(Value.class, new PropertyValueFormatHandler());
        val.addValueFormatHandler(Value[].class, new PropertyValueFormatHandler());
        directiveHandlers.put("val", val);

        directiveHandlers.put("list", new ListNodeFactory());
        directiveHandlers.put("resource-props", new ResourcePropsNodeFactory());
        DefineNodeFactory def = new DefineNodeFactory();
        def.addValueProvider("resource", new RetrieveHandler());
        def.addValueProvider("resource-prop", new ResourcePropHandler());
        def.addValueProvider("config", new ModelConfigHandler());
        def.addValueProvider("structured-document", new JSONDocumentProvider());
        def.addValueProvider("json-attr", new JSONAttributeHandler());
        def.addValueProvider("search", new SearchResultValueProvider());
        def.addValueProvider("view-url", new ViewURLValueProvider());
        directiveHandlers.put("def", def);

        directiveHandlers.put("localized", new LocalizationNodeFactory());

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

    private class ResourcePropsNodeFactory implements DirectiveNodeFactory {

        public Node create(DirectiveParseContext ctx) throws Exception {
            List<Argument> tokens = ctx.getArguments();
            if (tokens.size() != 1) {
                throw new Exception("Wrong number of arguments");
            }
            final Argument arg1 = tokens.get(0);

            return new Node() {
                public void render(Context ctx, Writer out) throws Exception {
                    Resource resource;
                    String ref;
                    if (arg1 instanceof Symbol) {
                        Object o = ((Symbol) arg1).getValue(ctx);
                        if (o == null) {
                            throw new Exception("Unable to resolve: " + arg1.getRawValue());
                        }
                        ref = o.toString();
                    } else {
                        ref = arg1.getValue(ctx).toString();
                    }

                    if (ref.equals(".")) {
                        Object o = ctx.get(MVC_MODEL_KEY);
                        if (o == null) {
                            throw new Exception("Unable to locate resource: no model: " + MVC_MODEL_KEY);
                        }
                        @SuppressWarnings("unchecked")
                        Map<String, Object> model = (Map<String, Object>) o;
                        resource = (Resource) model.get("resource");
                    } else {
                        Path uri = Path.fromString(ref);
                        String token = SecurityContext.getSecurityContext().getToken();
                        resource = repository.retrieve(token, uri, true);
                    }
                    if (resource == null) {
                        throw new RuntimeException("Unable to resolve resource");
                    }
                    for (Property prop : resource.getProperties()) {
                        if (prop.getDefinition().isMultiple()) {
                            ctx.define(prop.getDefinition().getName(), prop.getValues(), false);
                        } else {
                            ctx.define(prop.getDefinition().getName(), prop.getValue(), false);
                        }
                    }
                }
            };
        }

    }

    private class SearchResultValueProvider implements DefineNodeFactory.ValueProvider {

        public Object create(List<Argument> tokens, Context ctx) throws Exception {
            if (tokens.size() != 1) {
                throw new Exception("Illegal number of arguments");
            }
            Argument arg = tokens.get(0);
            String queryString;
            if (arg instanceof Symbol) {
                Object o = ((Symbol) arg).getValue(ctx);
                if (o == null) {
                    throw new Exception("Unable to resolve: " + arg.getRawValue());
                }
                queryString = o.toString();
            } else {
                queryString = arg.getValue(ctx).toString();
            }
            String token = SecurityContext.getSecurityContext().getToken();
            Query query = queryParserFactory.getParser().parse(queryString);
            Search search = new Search();
            search.setQuery(query);
            ResultSet resultSet = searcher.execute(token, search);
            return resultSet.iterator();
        }
    }

    private class ViewURLValueProvider implements DefineNodeFactory.ValueProvider {

        public Object create(List<Argument> tokens, Context ctx) throws Exception {
            if (tokens.size() != 1) {
                throw new Exception("Illegal number of arguments");
            }
            Argument arg = tokens.get(0);
            if (!(arg instanceof Symbol)) {
                throw new Exception("Argument must be a resource object: " + arg.getRawValue());
            }
            Object o = arg.getValue(ctx);
            if (o == null) {
                throw new Exception("Unable to resolve argument: " + arg.getRawValue());
            }
            if (!(o instanceof PropertySet)) {
                throw new Exception("Argument must be a resource object: " + arg.getRawValue());
            }
            return viewService.constructLink(((PropertySet) o).getURI());
        }

    }

    private class ResourcePropHandler implements DefineNodeFactory.ValueProvider {

        // Supported constructions:
        // "/foo/bar/" <propname>
        // "." <propname>
        // <var> <propname>

        public Object create(List<Argument> tokens, Context ctx) throws Exception {

            if (tokens.size() != 2) {
                throw new RuntimeException("Wrong number of arguments");
            }
            final Argument arg1 = tokens.get(0);
            final Argument arg2 = tokens.get(1);

            PropertySet resource = null;
            String ref = null;
            if (arg1 instanceof Symbol) {
                Object o = arg1.getValue(ctx);
                if (o instanceof PropertySet) {
                    resource = (PropertySet) o;
                } else {
                    if (o == null) {
                        throw new Exception("Unable to resolve: " + arg1.getRawValue());
                    }
                    ref = o.toString();
                }
            } else {
                ref = arg1.getValue(ctx).toString();
            }

            if (resource == null) {
                if (ref.equals(".")) {
                    Object o = ctx.get(MVC_MODEL_KEY);
                    if (o == null) {
                        throw new Exception("Unable to access MVC model: " + MVC_MODEL_KEY);
                    }
                    @SuppressWarnings("unchecked")
                    Map<String, Object> model = (Map<String, Object>) o;
                    resource = (Resource) model.get("resource");
                } else {
                    Path uri = Path.fromString(ref);
                    String token = SecurityContext.getSecurityContext().getToken();
                    resource = repository.retrieve(token, uri, true);
                }
            }
            String propName;
            if (arg2 instanceof Symbol) {
                Object o = arg2.getValue(ctx);
                if (o == null) {
                    throw new Exception("Unable to resolve: " + arg2.getRawValue());
                }
                propName = o.toString();
            } else {
                propName = arg2.getValue(ctx).toString();
            }
            Property property = resource.getProperty(Namespace.STRUCTURED_RESOURCE_NAMESPACE, propName);
            if (property == null) {
                property = resource.getProperty(Namespace.DEFAULT_NAMESPACE, propName);
            }
            if (property == null) {
                for (Property prop : resource.getProperties()) {
                    if (propName.equals(prop.getDefinition().getName())) {
                        property = prop;
                    }
                }
            }
            if (property == null) {
                return null;
            }
            if (property.getDefinition().isMultiple()) {
                return property.getValues();
            } else {
                return property.getValue();
            }
        }
    }

    private class JSONDocumentProvider implements DefineNodeFactory.ValueProvider {

        public Object create(List<Argument> tokens, Context ctx) throws Exception {
            if (tokens.size() != 0) {
                throw new Exception("Wrong number of arguments");
            }
            Object o = ctx.get(MVC_MODEL_KEY);
            if (o == null) {
                throw new Exception("Unable to access MVC model");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> model = (Map<String, Object>) o;
            StructuredResource res = (StructuredResource) model.get("structured-resource");
            if (res == null) {
                throw new Exception("No structured resource found in MVC model");
            }
            return res.toJSON();
        }
    }

    private class JSONAttributeHandler implements DefineNodeFactory.ValueProvider {

        // Supported constructions:
        // object "expression"
        // object expression (from variable)
        public Object create(List<Argument> tokens, Context ctx) throws Exception {
            if (tokens.size() != 2) {
                throw new Exception("Wrong number of arguments");
            }

            final Argument arg1 = tokens.get(0);
            Object object;
            if (!(arg1 instanceof Symbol)) {
                throw new Exception("First argument must be a symbol");
            }
            object = arg1.getValue(ctx);
            if (object == null) {
                throw new Exception("Unable to resolve: " + arg1.getRawValue());
            }

            final Argument arg2 = tokens.get(1);
            String expression;
            if (arg2 instanceof Symbol) {
                Object o = arg2.getValue(ctx);
                if (o == null) {
                    throw new Exception("Unable to resolve: " + arg2.getRawValue());
                }
                expression = o.toString();
            } else {
                expression = arg2.getValue(ctx).toString();
            }

            if (!(object instanceof JSONObject)) {
                throw new Exception("Cannot apply expression '" + expression + "' on object: not JSON data: "
                        + object.getClass());
            }

            JSONObject json = (JSONObject) object;
            return JSONUtil.select(json, expression);
        }
    }

    private class ModelConfigHandler implements DefineNodeFactory.ValueProvider {

        @SuppressWarnings("unchecked")
        public Object create(List<Argument> tokens, Context ctx) throws Exception {
            if (tokens.size() < 1) {
                throw new RuntimeException("Expected at least one argument");
            }
            List<String> keys = new ArrayList<String>();
            for (Argument arg : tokens) {
                String key;
                if (arg instanceof Symbol) {
                    Object o = arg.getValue(ctx);
                    if (o == null) {
                        throw new RuntimeException("Unable to resolve symbol: " + arg);
                    }
                    key = o.toString();
                } else {
                    key = ((Literal) arg).getStringValue();
                }
                keys.add(key);
            }
            Object o = ctx.get(MVC_MODEL_KEY);
            if (o == null) {
                throw new RuntimeException("Unable to locate resource: no model: " + MVC_MODEL_KEY);
            }
            Map<String, Object> model = (Map<String, Object>) o;
            if (!model.containsKey("config")) {
                throw new RuntimeException("No 'config' element present in model");
            }

            Map<String, Object> config = (Map<String, Object>) model.get("config");
            Map<String, Object> map = config;
            for (Iterator<String> iterator = keys.iterator(); iterator.hasNext();) {
                String key = iterator.next();
                Object obj = map.get(key);
                if (obj == null) {
                    throw new RuntimeException("Unable to find value for " + keys);
                }
                if (iterator.hasNext()) {
                    if (!(obj instanceof Map<?, ?>)) {
                        throw new RuntimeException("Unable to find value for " + keys);
                    }
                    map = (Map) obj;
                } else {
                    return obj;
                }
            }
            throw new RuntimeException("Unable to find value for " + keys);
        }
    }

    private class RetrieveHandler implements DefineNodeFactory.ValueProvider {

        // resource "/foo/bar"
        // resource "."
        // resource <var>

        public Object create(List<Argument> tokens, Context ctx) throws Exception {

            if (tokens.size() != 1) {
                throw new RuntimeException("Wrong number of arguments");
            }
            final Argument arg = tokens.get(0);

            Resource resource;

            String ref;
            if (arg instanceof Symbol) {
                Object o = arg.getValue(ctx);
                if (o == null) {
                    throw new RuntimeException("Unable to resolve: " + arg.getRawValue());
                }
                ref = o.toString();
            } else {
                ref = arg.getValue(ctx).toString();
            }

            if (ref.equals(".")) {
                Object o = ctx.get(MVC_MODEL_KEY);
                if (o == null) {
                    return null;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> model = (Map<String, Object>) o;
                resource = (Resource) model.get("resource");
            } else if (!ref.startsWith("/")) {
                Object o = ctx.get(ref);
                if (o == null) {
                    return null;
                }
                String s = o.toString();
                Path uri = Path.fromString(s);
                String token = SecurityContext.getSecurityContext().getToken();
                resource = repository.retrieve(token, uri, true);
            } else {
                Path uri = Path.fromString(ref);
                String token = SecurityContext.getSecurityContext().getToken();
                try {
                    resource = repository.retrieve(token, uri, true);
                } catch (ResourceNotFoundException rnfe) {
                    return null;
                }
            }
            return resource;
        }
    }

    private class PropertyValueFormatHandler implements ValNodeFactory.ValueFormatHandler {

        public Object handleValue(Object val, String format, Context ctx) {
            if (val instanceof Value[]) {
                StringBuilder sb = new StringBuilder();
                Value[] values = (Value[]) val;
                for (int i = 0; i < values.length; i++) {
                    ValueFormatter vf = valueFormatterRegistry.getValueFormatter(values[i].getType());
                    sb.append(vf.valueToString(values[i], format, ctx.getLocale()));
                    if (i < values.length - 1) {
                        sb.append(", ");
                    }
                }
                return sb.toString();
            } else {
                Value value;
                if (val instanceof Property) {
                    value = ((Property) val).getValue();
                } else if (val instanceof Value) {
                    value = (Value) val;
                } else {
                    throw new RuntimeException("Unknown type: " + val.getClass());
                }
                ValueFormatter vf = valueFormatterRegistry.getValueFormatter(value.getType());
                if (vf == null) {
                    throw new RuntimeException("Unable to find value formatter for value " + value);
                }
                return vf.valueToString(value, format, ctx.getLocale());
            }
        }
    }

    private class LocalizationNodeFactory implements DirectiveNodeFactory {

        public Node create(DirectiveParseContext ctx) throws Exception {
            List<Argument> args = ctx.getArguments();
            if (args.size() == 0) {
                throw new RuntimeException("Missing arguments: " + ctx.getNodeText());
            }
            final Argument code = args.remove(0);
            final List<Argument> rest = new ArrayList<Argument>(args);

            return new Node() {
                public void render(Context ctx, Writer out) throws Exception {
                    String key;
                    if (code instanceof Symbol) {
                        Object o = code.getValue(ctx);
                        if (o == null) {
                            throw new RuntimeException("Unable to resolve symbol: " + code);
                        }
                        key = o.toString();
                    } else {
                        key = ((Literal) code).getStringValue();
                    }
                    Object o = ctx.get(MVC_MODEL_KEY);
                    if (o == null) {
                        throw new RuntimeException("Unable to locate resource: no model: " + MVC_MODEL_KEY);
                    }
                    Object[] localizationArgs = new Object[rest.size()];
                    for (int i = 0; i < rest.size(); i++) {
                        Argument a = rest.get(i);
                        localizationArgs[i] = a.getValue(ctx);
                    }

                    @SuppressWarnings("unchecked")
                    Map<String, Object> model = (Map<String, Object>) o;
                    StructuredResource resource = (StructuredResource) model.get(resourceModelKey);
                    if (resource == null) {
                        throw new RuntimeException("Unable to localize string: " + key + ": no resource found in model");
                    }
                    String localizedMsg = resource.getType().getLocalizedMsg(key, ctx.getLocale(), localizationArgs);
                    out.write(ctx.htmlEscape(localizedMsg));
                }
            };
        }
    }

}
