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
package org.vortikal.web.view.decorating;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.text.html.EnclosingHtmlContent;
import org.vortikal.text.html.HtmlAttribute;
import org.vortikal.text.html.HtmlComment;
import org.vortikal.text.html.HtmlContent;
import org.vortikal.text.html.HtmlElement;
import org.vortikal.text.html.HtmlPage;
import org.vortikal.text.html.HtmlPageParser;
import org.vortikal.text.html.HtmlText;

/**
 * XXX: unfinished code, needs componentization
 */
public class ParsedHtmlDecoratorTemplate implements Template {

    private CompiledTemplate compiledTemplate;

    public ParsedHtmlDecoratorTemplate(HtmlPageParser htmlParser, 
            TextualComponentParser componentParser,
            ComponentResolver componentResolver,
            TemplateSource templateSource) throws InvalidTemplateException {

        if (htmlParser == null) {
            throw new IllegalArgumentException("Argument 'htmlParser' is NULL");
        }
        if (componentParser == null) {
            throw new IllegalArgumentException("Argument 'componentParser' is NULL");
        }
        if (componentResolver == null) {
            throw new IllegalArgumentException("Argument 'componentResolver' is NULL");
        }
        if (templateSource == null) {
            throw new IllegalArgumentException("Argument 'templateSource' is NULL");
        }
        this.compiledTemplate = new CompiledTemplate(htmlParser, componentParser, componentResolver, templateSource);
    }


    public PageContent render(HtmlPageContent html, HttpServletRequest request,
            Map<Object, Object> model) throws Exception {
        HtmlPage resultPage = 
            this.compiledTemplate.generate(html.getHtmlContent(), request, model);
        return new HtmlPageContentImpl(resultPage.getCharacterEncoding(), resultPage);
    }

    private class CompiledTemplate {
        private Node root;

        public CompiledTemplate(HtmlPageParser htmlParser, 
                TextualComponentParser componentParser,
                ComponentResolver componentResolver,
                TemplateSource templateSource) throws InvalidTemplateException {

            HtmlPage page = null;
            try {
                page = htmlParser.parse(
                        templateSource.getInputStream(), 
                        templateSource.getCharacterEncoding());
            } catch (Exception e) {
                throw new InvalidTemplateException(
                        "Error parsing template " + templateSource, e);
            }
            this.root = new Node(page.getRootElement(), componentParser, componentResolver);
        }

        public HtmlPage generate(HtmlPage userPage, 
                HttpServletRequest request, Map<Object, Object> model) throws Exception {
            List<HtmlContent> transformedContent = 
                this.root.generate(userPage, request, model);

            if (transformedContent.size() != 1) {
                throw new IllegalStateException("Invalid HTML result");
            }

            if (!(transformedContent.get(0) instanceof HtmlElement)) {
                throw new IllegalStateException("Invalid HTML result");
            }

            HtmlElement newRoot = ((HtmlElement) transformedContent.get(0));
            userPage.getRootElement().setChildNodes(newRoot.getChildNodes());
            return userPage;
        }
    }


    private class Node {
        private HtmlContent content;
        private List<Node> children = new ArrayList<Node>();
        private ComponentInvocation[] parsedContent = null;
        private String copyElementExpression = null;
        private String copyAttributesExpression = null;
        private Map<String, ComponentInvocation[]> attributesMap;
        private ComponentInvocation elementComponent;

        public Node(HtmlContent c, TextualComponentParser componentParser, ComponentResolver componentResolver) {
            this.content = c;

            if (c instanceof HtmlElement) {
                HtmlElement elem = (HtmlElement) c;
                if (elem.getName().startsWith("v:")) {
                    if (elem.getName().equals("v:element-content")) {
                        HtmlAttribute from = elem.getAttribute("from");
                        if (from != null) {
                            this.copyElementExpression = from.getValue();
                        }
                    } else if (elem.getName().equals("v:component")) {
                        
                        if (elem.getAttribute("name") == null) {
                            DecoratorComponent err = new StaticTextComponent(
                                    new StringBuilder("Missing 'name' attribute on element: " 
                                            + elem.getEnclosedContent()));
                            this.elementComponent = new ComponentInvocationImpl(err, new HashMap<String, Object>());
                        } else {
                            String componentRef = elem.getAttribute("name").getValue();
                            int separatorIdx = componentRef.indexOf(":");
                            if (separatorIdx == -1 || separatorIdx == 0 
                                    || separatorIdx == componentRef.length() - 1) {
                                DecoratorComponent err = new StaticTextComponent(
                                        new StringBuilder("Invalid component reference: " + componentRef));
                                this.elementComponent = new ComponentInvocationImpl(err, new HashMap<String, Object>());

                            } else {

                                String namespace = componentRef.substring(0, separatorIdx);
                                String name = componentRef.substring(separatorIdx + 1);
                                Map<String, Object> parameters = new HashMap<String, Object>();
                                HtmlElement[] paramElems = elem.getChildElements("v:parameter");

                                for (HtmlElement paramElem: paramElems) {
                                    HtmlAttribute paramName = paramElem.getAttribute("name");
                                    HtmlAttribute paramValue = paramElem.getAttribute("value");
                                    if (paramName == null || paramValue == null) {
                                        DecoratorComponent err = new StaticTextComponent(
                                                new StringBuilder("Component parameters must have 'name' and 'value' attributes: " 
                                                        + elem.getEnclosedContent()));
                                        this.elementComponent = new ComponentInvocationImpl(err, new HashMap<String, Object>());
                                        break;
                                    } else {
                                        parameters.put(paramName.getValue(), paramValue.getValue());
                                        DecoratorComponent component = 
                                            componentResolver.resolveComponent(namespace, name);
                                        if (component != null) {
                                            this.elementComponent = new ComponentInvocationImpl(component, parameters);
                                        } else {
                                            DecoratorComponent err = new StaticTextComponent(
                                                    new StringBuilder("Unknown component: " + namespace + ":" + name));
                                            this.elementComponent = new ComponentInvocationImpl(err, new HashMap<String, Object>());
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (!elem.getName().matches("[a-zA-Z0-9]+")) {
                    throw new InvalidTemplateException("Invalid element name: " + elem.getName());
                }

                if (elem.getAttribute("v:attrs-from") != null) {
                    this.copyAttributesExpression = 
                        elem.getAttribute("v:attrs-from").getValue();
                } else {
                    this.attributesMap = new LinkedHashMap<String, ComponentInvocation[]>();
                    for (HtmlAttribute attr: elem.getAttributes()) {
                        String value = attr.getValue(); 

                        ComponentInvocation[] parsedValue;
                        try {
                            parsedValue = 
                                componentParser.parse(new StringReader(value));
                        } catch (Exception e) {
                            final DecoratorComponent staticText = new StaticTextComponent(new StringBuilder(value));
                            parsedValue = new ComponentInvocation[]{
                                    new ComponentInvocation() {
                                        public DecoratorComponent getComponent() {
                                            return staticText;
                                        }
                                        public Map<String, Object> getParameters() {
                                            return new HashMap<String, Object>();
                                        }
                                    }
                            };
                        }
                        this.attributesMap.put(attr.getName(), parsedValue);
                    }
                }
            }

            if (c instanceof HtmlText) {
                try {
                    this.parsedContent = componentParser.parse(
                            new java.io.StringReader(c.getContent()));
                } catch (Exception e) {
                    // XXX:
//                    throw new InvalidTemplateException("XXX");
                }
            }

            if (c instanceof EnclosingHtmlContent) {
                for (HtmlContent child: ((EnclosingHtmlContent) c).getChildNodes()) {
                    if (child instanceof HtmlText) {
                        if (((HtmlText) child).getContent().length() == 0) {
                            continue;
                        }
                    }
                    this.children.add(new Node(child, componentParser, componentResolver));
                }
            }
        }

    List<HtmlContent> generate(HtmlPage userPage, HttpServletRequest req, 
            Map<Object, Object> model) throws Exception {

        List<HtmlContent> l = new ArrayList<HtmlContent>();

        if (this.content instanceof HtmlElement) {
            generateElement(userPage, req, model, l);

        } else if (this.content instanceof HtmlComment) {
            generateComment(userPage, req, model, l);

        } else if (this.content instanceof HtmlText) {
            generateTextNode(userPage, req, model, l);

        } else {
            l.add(this.content);
        }
        return l;
    }

    private void generateElement(HtmlPage userPage, HttpServletRequest req, 
            Map<Object, Object> model, List<HtmlContent> result) throws Exception {

        if (this.copyElementExpression != null) {
            HtmlElement copyElement = userPage.selectSingleElement(this.copyElementExpression);
            if (copyElement != null) {
                result.addAll(java.util.Arrays.asList(copyElement.getChildNodes()));
            }
        } else if (this.elementComponent != null) {

            Locale locale = 
                new org.springframework.web.servlet.support.RequestContext(req).getLocale();
            DecoratorRequest decoratorRequest = new DecoratorRequestImpl(
                    userPage, req, model, this.elementComponent.getParameters(), userPage.getDoctype(), locale);
            String renderedContent = renderComponent(this.elementComponent.getComponent(), decoratorRequest);
            result.add(userPage.createTextNode(renderedContent));

        } else {
            HtmlElement element = (HtmlElement) this.content;
            HtmlElement newElem = userPage.createElement(element.getName());

            if (this.copyAttributesExpression != null) {
                HtmlAttribute[] attrs = element.getAttributes();
                if (attrs != null && attrs.length > 0) {
                    List<HtmlAttribute> newAttrs = new ArrayList<HtmlAttribute>();
                    for (HtmlAttribute attr: attrs) {
                        newAttrs.add(attr);
                    }
                    newElem.setAttributes(newAttrs.toArray(new HtmlAttribute[newAttrs.size()]));
                }
            } else {
                List<HtmlAttribute> newAttrs = new ArrayList<HtmlAttribute>();
                for (String name: this.attributesMap.keySet()) {
                    String renderedValue = renderComponents(userPage, req, model, 
                            this.attributesMap.get(name));
                    HtmlAttribute attr = userPage.createAttribute(name, renderedValue);
                    newAttrs.add(attr);
                }
                newElem.setAttributes(newAttrs.toArray(
                        new HtmlAttribute[newAttrs.size()]));
            }

            List<HtmlContent> newElementContent = new ArrayList<HtmlContent>();
            for (Node childNode: this.children) {
                newElementContent.addAll(childNode.generate(userPage, req, model));
            }
            newElem.setChildNodes(newElementContent.toArray(new HtmlContent[newElementContent.size()]));
            result.add(newElem);
        }
    }


    private void generateComment(HtmlPage userPage, HttpServletRequest req, 
            Map<Object, Object> model, List<HtmlContent> result) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (Node childNode: this.children) {
            List<HtmlContent> nodes = childNode.generate(userPage, req, model);
            for (HtmlContent commentContent: nodes) {
                sb.append(commentContent.getContent());
            }
            HtmlComment newComment = userPage.createComment(sb.toString());
            result.add(newComment);
        }
    }

    private void generateTextNode(HtmlPage userPage, HttpServletRequest req, 
            Map<Object, Object> model, List<HtmlContent> result) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (ComponentInvocation inv: this.parsedContent) {
            Locale locale = 
                new org.springframework.web.servlet.support.RequestContext(req).getLocale();
            DecoratorRequest decoratorRequest = new DecoratorRequestImpl(
                    userPage, req, model, inv.getParameters(), userPage.getDoctype(), locale);
            String renderedContent = renderComponent(inv.getComponent(), decoratorRequest);
            sb.append(renderedContent);
        }
        HtmlText newText = userPage.createTextNode(sb.toString());
        result.add(newText);
    }

    private String renderComponents(HtmlPage userPage, HttpServletRequest req,
            Map<Object, Object> model, ComponentInvocation[] invocations) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (ComponentInvocation inv: invocations) {
            Locale locale = 
                new org.springframework.web.servlet.support.RequestContext(req).getLocale();
            DecoratorRequest decoratorRequest = new DecoratorRequestImpl(
                    userPage, req, model, inv.getParameters(), userPage.getDoctype(), locale);
            String renderedContent = renderComponent(inv.getComponent(), decoratorRequest);
            sb.append(renderedContent);
        }
        return sb.toString();            
    }

    private String renderComponent(DecoratorComponent c, DecoratorRequest request)
    throws Exception {

        String defaultResponseDoctype = request.getDoctype();
        String defaultResponseEncoding = "utf-8";
        Locale defaultResponseLocale = Locale.getDefault();

        DecoratorResponseImpl response = new DecoratorResponseImpl(
                defaultResponseDoctype, defaultResponseLocale, defaultResponseEncoding);
        c.render(request, response);
        String result = response.getContentAsString();
        return result;
    }

}


}