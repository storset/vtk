/* Copyright (c) 2007, University of Oslo, Norway
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

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletRequest;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.vortikal.repository.Property;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.text.html.HtmlElement;
import org.vortikal.text.html.HtmlFragment;
import org.vortikal.text.html.HtmlPage;
import org.vortikal.text.html.HtmlPageFilter;
import org.vortikal.text.html.HtmlPageParser;

public class ResourceEditDataBinder extends ServletRequestDataBinder {

    private HtmlPageParser htmlParser;
    private HtmlPageFilter htmlPropsFilter;
    private final static String defaultCharacterEncoding = "utf-8";
    private Map<PropertyTypeDefinition, PropertyEditPreprocessor> propertyPreprocessors;


    public ResourceEditDataBinder(Object target, String objectName, HtmlPageParser htmlParser,
            HtmlPageFilter htmlPropsFilter, Map<PropertyTypeDefinition, PropertyEditPreprocessor> propertyPreprocessors) {
        super(target, objectName);
        this.htmlParser = htmlParser;
        this.htmlPropsFilter = htmlPropsFilter;
        this.propertyPreprocessors = propertyPreprocessors;
    }


    @Override
    public void bind(ServletRequest request) {
        if (getTarget() instanceof ResourceEditWrapper) {
            ResourceEditWrapper command = (ResourceEditWrapper) getTarget();

            if (request.getParameter("save") != null) {
                command.setSave(true);
            } else if (request.getParameter("savequit") != null) {
                command.setSave(true);
                command.setQuit(true);
            } else {
                return;
            }

            Resource resource = command.getResource();

            setProperties(request, command, resource, command.getPreContentProperties());
            setProperties(request, command, resource, command.getPostContentProperties());

            if (command.getContent() != null) {
                String content = command.getContent().getStringRepresentation();
                String postedHtml = request.getParameter("resource.content");
                if (!content.equals(postedHtml)) {
                    parseContent(command, postedHtml);
                }
            }
        } else {
            super.bind(request);
        }
    }


    protected void setProperties(ServletRequest request, ResourceEditWrapper command, Resource resource,
            List<PropertyTypeDefinition> propDefs) {

        for (PropertyTypeDefinition propDef : propDefs) {
            String value = null;

            if (propDef.getType().equals(PropertyType.Type.TIMESTAMP)
                    || propDef.getType().equals(PropertyType.Type.DATE)) {
                value = request.getParameter("resource." + propDef.getName() + ".date");
                String time = request.getParameter("resource." + propDef.getName() + ".hours");
                if (value != null && time != null && !time.trim().equals("")) {
                    String minutes = request.getParameter("resource." + propDef.getName() + ".minutes");
                    if (minutes != null && !minutes.trim().equals("")) {
                        time += ":" + minutes;
                    }
                    value += " " + time;
                }
            } else {
                value = request.getParameter("resource." + propDef.getName());
            }

            Property prop = resource.getProperty(propDef);
            if (prop == null) {
                if (value != null && !value.trim().equals("")) {
                    try {
                        prop = resource.createProperty(propDef);
                        setPropValue(value, prop);
                        command.setPropChange(true);
                    } catch (Throwable t) {
                        command.reject(propDef, t.getMessage());
                        resource.removeProperty(propDef);
                    }
                    continue;
                } else if (propDef.isMandatory()) {
                    command.reject(propDef, propDef.getName() + " is required ");
                    continue;
                }
            } else if (value == null || value.trim().equals("")) {
                if (propDef.isMandatory()) {
                    command.reject(propDef, propDef.getName() + " is required");
                    continue;
                }
                command.setPropChange(true);
                resource.removeProperty(propDef);
                continue;
            } else {
                try {
                    setPropValue(value, prop);
                    command.setPropChange(true);
                } catch (Throwable t) {
                    command.reject(propDef, t.getMessage());
                }
            }
        }
    }


    protected void setPropValue(String valueString, Property prop) throws IllegalArgumentException {
        PropertyTypeDefinition propDef = prop.getDefinition();

        if (this.propertyPreprocessors != null && this.propertyPreprocessors.containsKey(propDef)) {
            valueString = this.propertyPreprocessors.get(propDef).preprocess(valueString, prop);
        }

        if (propDef.isMultiple()) {
            String[] strings = valueString.split(",");
            if (strings.length == 0) {
                throw new IllegalArgumentException("Value cannot be empty");
            }

            List<Value> values = new ArrayList<Value>();
            for (String string : strings) {
                if (StringUtils.isNotBlank(string)) {
                    values.add(propDef.getValueFormatter().stringToValue(string.trim(), null, null));
                }
            }
            prop.setValues(values.toArray(new Value[values.size()]));
        } else if (prop.getDefinition().getType() == PropertyType.Type.HTML) {

            try {
                HtmlFragment fragment = this.htmlParser.parseFragment(valueString);
                fragment.filter(this.htmlPropsFilter);
                Value value = new Value(fragment.getStringRepresentation(), PropertyType.Type.HTML);
                prop.setValue(value);
            } catch (Throwable t) {
                throw new IllegalArgumentException(t);
            }

        } else {
            Value value = propDef.getValueFormatter().stringToValue(valueString, null, null);
            prop.setValue(value);
        }
    }


    protected void parseContent(ResourceEditWrapper command, String postedHtml) {

        if (postedHtml == null) postedHtml = "";
        try {
            postedHtml = "<html><head></head><body>" + postedHtml + "</body></html>";
            ByteArrayInputStream in = null;
            HtmlPage parsed = null;

            if (Charset.isSupported(command.getResource().getCharacterEncoding())) {
                in = new ByteArrayInputStream(postedHtml.getBytes(command.getResource().getCharacterEncoding()));
                parsed = this.htmlParser.parse(in, command.getResource().getCharacterEncoding());
            } else {
                in = new ByteArrayInputStream(postedHtml.getBytes(defaultCharacterEncoding));
                parsed = this.htmlParser.parse(in, defaultCharacterEncoding);
            }

            HtmlElement body = command.getContent().selectSingleElement("html.body");
            HtmlElement suppliedBody = parsed.selectSingleElement("html.body");

            // If body tag is non-excisting add standard web-page with supplied body-content
            if (body == null) {
                body = command.getContent().createElement("html.body");
                command.setContent(parsed);
                // Else: Normal behaviour
            } else {
                body.setChildNodes(suppliedBody.getChildNodes());
            }
            command.setContentChange(true);
        } catch (Throwable t) {
            throw new RuntimeException("Unable to save content", t);
        }
    }


    public void setHtmlParser(HtmlPageParser htmlParser) {
        this.htmlParser = htmlParser;
    }


    protected HtmlPageParser getHtmlParser() {
        return htmlParser;
    }

}
