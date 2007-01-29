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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.util.io.StreamUtil;
import java.util.Map;


public class DefaultTemplateParser implements TemplateParser {

    private static Log logger = LogFactory.getLog(DefaultTemplateParser.class);

    private ComponentResolver componentResolver;
    
    public void setComponentResolver(ComponentResolver componentResolver) {
        this.componentResolver = componentResolver;
    }
    

    
    public ComponentInvocation[] parseTemplate(TemplateSource source) throws Exception {
        String s = new String(StreamUtil.readInputStream(source.getTemplateInputStream()));
        return parseInternal(s);
    }
    

    public ComponentInvocation[] parseInternal(String s) throws Exception {

        // Searching for:
        // ${namespace:name param1=[value] param2=[value]}
        
        List fragmentList = new ArrayList();

        int contentIdx = 0;
        while (true) {
            int directiveStart = s.indexOf("${", contentIdx);
            if (directiveStart == -1) {
                break;
            }
            directiveStart += 2;

            // XXX: allow escape syntax: '\}'
            int directiveEnd = s.indexOf("}", directiveStart);
            if (directiveEnd == -1) {
                break;
            }

            ComponentInvocation c = parseDirective(s.substring(directiveStart, directiveEnd));
            if (c != null) {
                fragmentList.add(staticTextComponent(s.substring(contentIdx, directiveStart - 2)));
                fragmentList.add(c);
            } else {
                fragmentList.add(staticTextComponent(s.substring(directiveStart - 2, directiveEnd + 1)));
            }
            contentIdx = directiveEnd + 1;
        }
        String chunk = s.substring(contentIdx, s.length());
        fragmentList.add(staticTextComponent(chunk));

        return (ComponentInvocation[]) fragmentList.toArray(
            new ComponentInvocation[fragmentList.size()]);
    }


    private ComponentInvocation staticTextComponent(String s) {
        StaticTextDecoratorComponent c = new StaticTextDecoratorComponent();
        c.setContent(s);
        if (logger.isDebugEnabled()) {
            logger.debug("Static text: " + s);
        }
        return new ComponentInvocationImpl(c, new HashMap());
    }
    

    private ComponentInvocation parseDirective(String s) {

        // namespace:name param1=[value] param2=[value]
        String componentRef = parseComponentRef(s);
        if (componentRef == null) {
            return null;
        }
        
        String namespace = parseComponentNamespace(componentRef);
        String name = parseComponentName(componentRef);

        DecoratorComponent component = this.componentResolver.resolveComponent(
            namespace, name);
        if (component == null) {
            logger.info("Unable to resolve component " + namespace + ":" + name);
            return null;
        } 

        LinkedHashMap parameters = splitParameterList(s);
        Map actualParameters = new HashMap();
        for (Iterator i = parameters.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            String parameterName = (String) entry.getKey();
            String parameterValue =  (String) entry.getValue();

            actualParameters.put(parameterName, parameterValue);
        }
        return new ComponentInvocationImpl(component, actualParameters);
    }


    public ComponentInvocation[] parseTemplateOLD(TemplateSource source) throws Exception {
        

        String s = new String(StreamUtil.readInputStream(
                                  source.getTemplateInputStream()));

        parseInternal(s);
        logger.info("Compiling template from source " + source);

        if (logger.isDebugEnabled()) {
            logger.debug("Compiling template from string " + s);
        }
        
        // Original:
        Pattern pattern = Pattern.compile("\\$\\{([^\\}:]+):([^\\}:]+)\\}", Pattern.DOTALL);

        Matcher matcher = pattern.matcher(s);

        int idx = 0;
        int contentIdx = 0;

        List fragmentList = new ArrayList();


        while (matcher.find(idx)) {

            String namespace = matcher.group(1);
            String name = matcher.group(2);
            if (contentIdx < matcher.start()) {
                String chunk = s.substring(contentIdx, matcher.start());
                StaticTextDecoratorComponent c = new StaticTextDecoratorComponent();
                c.setContent(chunk);
                if (logger.isDebugEnabled()) {
                    logger.debug("Static text component:  " + c);
                }
                fragmentList.add(new ComponentInvocationImpl(c, new HashMap()));
            }

            DecoratorComponent component = this.componentResolver.resolveComponent(
                namespace, name);
            if (component == null) {
                logger.info("Unable to resolve component " + namespace + ":" + name);
            } else {
                fragmentList.add(new ComponentInvocationImpl(component, new HashMap()));
            }

            idx = matcher.end();
            contentIdx = idx;
        }

        String chunk = s.substring(contentIdx, s.length());
        StaticTextDecoratorComponent c = new StaticTextDecoratorComponent();
        c.setContent(chunk);
        if (logger.isDebugEnabled()) {
            logger.debug("Static text component:  " + c);
        }
        fragmentList.add(new ComponentInvocationImpl(c, new HashMap()));

        return (ComponentInvocation[]) fragmentList.toArray(
            new ComponentInvocation[fragmentList.size()]);
    }
    

    private String parseComponentRef(String s) {
        if (s == null || s.trim().length() <= 1) {
            return null;
        }
        s = s.trim();
        // namespace:name param1=[value] param2=[value]        
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
        return namespace;
    }
    
    private String parseComponentName(String s) {
        
        int delimIdx = s.indexOf(":");
        if (delimIdx == -1) {
            return null;
        }
        String name = s.substring(delimIdx + 1, s.length());
        return name;
    }
    
    
    private LinkedHashMap splitParameterList(String s) {
        LinkedHashMap result = new LinkedHashMap();
        int startIdx = 0;
        while (true) {
            
            int equalsIdx = s.indexOf("=", startIdx);
            if (equalsIdx == -1) {
                break;
            }

            int nameStartIdx = s.indexOf(" ", startIdx) + 1;
            if (nameStartIdx == 0 || nameStartIdx > equalsIdx) {
                break;
            }

            int valueStartIdx = s.indexOf("[", equalsIdx) + 1;
            if (valueStartIdx == 0) {
                break;
            }

            int valueEndIdx = s.indexOf("]", valueStartIdx);
            if (valueEndIdx == -1) {
                break;
            }
            startIdx = valueEndIdx;
            String name = s.substring(nameStartIdx, equalsIdx);
            String value = s.substring(valueStartIdx, valueEndIdx);
            result.put(name, value);
        }
        return result;
    }
    

    

}
