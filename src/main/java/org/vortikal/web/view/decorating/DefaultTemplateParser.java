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
package org.vortikal.web.view.decorating;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class DefaultTemplateParser implements TemplateParser {

    private static Log logger = LogFactory.getLog(DefaultTemplateParser.class);

    private ComponentResolver componentResolver;
    
    public void setComponentResolver(ComponentResolver componentResolver) {
        this.componentResolver = componentResolver;
    }
    
    public ComponentInvocation[] parseTemplate(Reader reader) throws Exception {
        BufferedReader bufferedReader = new BufferedReader(reader);
        
        StringBuffer sb = new StringBuffer();
        char[] buffer = new char[1024];

        int n = 0;
        while ((n = bufferedReader.read(buffer)) > 0) {
            sb.append(buffer, 0, n);
        }
        return parseInternal(sb.toString());
    }
    

    private ComponentInvocation[] parseInternal(String s) throws Exception {

        // Look for:
        // ${namespace:name param1=[value] param2=[value]}
        
        List fragmentList = new ArrayList();

        int contentIdx = 0;
        while (true) {
            int directiveStart = s.indexOf("${", contentIdx);
            if (directiveStart == -1) {
                break;
            }
            directiveStart += 2;

            int directiveEnd = nextIndexOf(s, '}', '\\', directiveStart);
            if (directiveEnd == -1) {
                break;
            }

            String componentContent = unescapedSubstring(
                s, '}', '\\', directiveStart, directiveEnd);
            ComponentInvocation c = parseDirective(componentContent);

            if (logger.isDebugEnabled()) {
                logger.debug("Parsed directive '" + componentContent + "' --> " + c);
            }

            if (c == null) {
                addStaticText(fragmentList, s.substring(contentIdx, directiveStart));
                contentIdx = directiveStart;
            } else {
                addStaticText(fragmentList, s.substring(contentIdx, directiveStart - 2));
                addDynamicComponent(fragmentList, c);
                contentIdx = directiveEnd + 1;
            }
        }
        String finalChunk = s.substring(contentIdx, s.length());
        addStaticText(fragmentList, finalChunk);

        return (ComponentInvocation[]) fragmentList.toArray(
            new ComponentInvocation[fragmentList.size()]);
    }

    private ComponentInvocation parseDirective(String s) {
        if (s != null) {
            s = s.trim();
        }
        // namespace:name param1=[value] param2=[value]
        String componentRef = parseComponentRef(s);
        if (componentRef == null) {
            return null;
        }
        
        String namespace = parseComponentNamespace(componentRef);
        String name = parseComponentName(componentRef);
        if (namespace == null || name == null) {
            return null;
        }


        DecoratorComponent component = this.componentResolver.resolveComponent(
            namespace, name);
        if (component == null) {
            logger.info("Unable to resolve component '" + namespace + "' : '" + name + "'");
            return null;
        } 

        LinkedHashMap parameters = splitParameterList(s);
        if (parameters == null) {
            logger.info("Malformed parameter list in directive: '" + s + "'");
            return null;
        }

        Map actualParameters = new HashMap();
        for (Iterator i = parameters.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            String parameterName = (String) entry.getKey();
            String parameterValue =  (String) entry.getValue();

            actualParameters.put(parameterName, parameterValue);
        }
        return new ComponentInvocationImpl(component, actualParameters);
    }



    private String parseComponentRef(String s) {
        if (s == null || s.trim().length() <= 1) {
            return null;
        }
        s = s.trim();
        // namespace:name
        int delimIdx = s.indexOf(":");
        if (delimIdx == -1) {
            return null;
        }
        int endIdx = s.indexOf(" ");
        if (endIdx == -1) {
            endIdx = s.length();
        }

        return s.substring(0, endIdx);
    }
    
    private String parseComponentNamespace(String s) {
        
        int delimIdx = s.indexOf(":");
        if (delimIdx == -1) {
            return null;
        }

        String namespace = s.substring(0, delimIdx);
        if (!namespace.matches("[a-zA-Z]+")) {
            return null;
        }
        return namespace.trim();
    }
    
    private String parseComponentName(String s) {
        
        int delimIdx = s.indexOf(":");
        if (delimIdx == -1) {
            return null;
        }
        String name = s.substring(delimIdx + 1, s.length());
        if (name != null) {
            name = name.trim();
        }
        if (!name.matches("[a-zA-Z]+")) {
            return null;
        }
        return name;
    }
    
    
    /**
     * Splits a parameter list into a map of <code>(name, value)</code> pairs.
     *
     * @params the raw parameter list
     * @return the parameter map, or <code>null</code> if the
     * parameter list is not well-formed.
     */
    private LinkedHashMap splitParameterList(String s) {
        LinkedHashMap result = new LinkedHashMap();
        int startIdx = 0;
        while (true) {
            int equalsIdx = s.indexOf("=", startIdx);
            if (equalsIdx == -1) {
                break;
            }

            int nameStartIdx = nextWhitespaceIdx(s, startIdx) + 1;
            if (nameStartIdx == 0 || nameStartIdx > equalsIdx) {
                break;
            }

            int valueStartIdx = s.indexOf("[", equalsIdx) + 1;
            if (valueStartIdx == 0) {
                break;
            }

            int valueEndIdx = nextIndexOf(s, ']', '\\', valueStartIdx);
            if (valueEndIdx == -1) {
                // Malformed parameter list:
                return null;
            }
            startIdx = valueEndIdx;
            String name = s.substring(nameStartIdx, equalsIdx).trim();
            String value = unescapedSubstring(
                s, ']', '\\', valueStartIdx, valueEndIdx).trim();
            result.put(name, value);
        }
        return result;
    }
    

    private int nextIndexOf(String string, char character, char escape, int startIndex) {
        for (int i = startIndex; i < string.length(); i++) {
            char current = string.charAt(i);
            if (current == character) {
                if (i > startIndex) {
                    char prev = string.charAt(i - 1);
                    if (prev == escape) {
                        continue;
                    }
                }
                return i;
            }
        }
        return -1;
    }
    
    private String unescapedSubstring(String string, char special,
                                     char escape, int startIndex, int endIndex) {
        StringBuffer sb = new StringBuffer();
        for (int i = startIndex; i < endIndex; i++) {
            char current = string.charAt(i);
            if (current == escape) {
                if (i < endIndex - 1) {
                    char next = string.charAt(i + 1);
                    if (next == special) {
                        continue;
                    }
                }
            }
            sb.append(current);
        }
        return sb.toString();
    }
    

    private int nextWhitespaceIdx(String s, int startIdx) {
        int nearest = -1;
        for (int i = startIdx; i < s.length(); i++) {
            boolean found = false;
            char c = s.charAt(i);
            switch (c) {
                case ' ':
                case '\t':
                case '\n':
                    found = true;
                    break;
                default:
                    found = false;
            }
            if (found) {
                nearest = i;
                break;
            }
        }
        return nearest;
    }

    
    private void addDynamicComponent(List fragmentList, ComponentInvocation inv) {
        if (logger.isDebugEnabled()) {
            logger.debug("Dynamic component: " + inv);
        }
        fragmentList.add(inv);
    }
    

    private void addStaticText(List fragmentList, String s) {
        if (logger.isDebugEnabled()) {
            logger.debug("Static text: '" + s + "'");
        }
        if (fragmentList.size() > 0) {
            ComponentInvocation inv = (ComponentInvocation)
                fragmentList.get(fragmentList.size() - 1);
            DecoratorComponent c = inv.getComponent();
            if (c instanceof StaticComponent) {
                StaticComponent staticComponent = (StaticComponent) c;
                staticComponent.getBuffer().append(s);
                return;
            }
        }
        StaticComponent c = new StaticComponent();
        c.setContent(new StringBuffer(s));
        fragmentList.add(new ComponentInvocationImpl(c, new HashMap()));
    }
    
    private class StaticComponent implements DecoratorComponent {
    
        private StringBuffer content;
    
        public void setContent(StringBuffer content) {
            this.content = content;
        }

        public void render(DecoratorRequest request, DecoratorResponse response)
            throws Exception {
            
            Writer out = response.getWriter();
            out.write(this.content.toString());
            out.close();
        }
    
    
        public String getNamespace() {
            return this.getClass().getName();
        }
    
        public String getName() {
            return "StaticText";
        }
    
        public String getDescription() {
            return "";
        }

        public Map getParameterDescriptions() {
            return new HashMap();
        }

        public String toString() {
            return getName();
        }
    
        public StringBuffer getBuffer() {
            return this.content;
        }
        
    }
    
}
