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
package org.vortikal.text.html;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class HtmlSelectUtil {

    private static final Pattern ID_PATTERN = Pattern.compile("([a-z0-9]+)?#(.+)");
    private static final Pattern EXP_PATTERN = Pattern.compile("([a-z0-9]+)\\(([0-9]+)\\)$");
    
    public static List<HtmlElement> select(HtmlPage page, String expression) {
        HtmlElement current = page.getRootElement();
        if (current == null) {
            return null;
        }
        Matcher matcher = ID_PATTERN.matcher(expression);
        if (matcher.matches()) {
            String name = null, id = null;
            if (matcher.groupCount() == 2) {
                name = matcher.group(1);
                id = matcher.group(2);
            } else {
                id = matcher.group(1);
            }
            List<HtmlElement> result = new ArrayList<HtmlElement>();
            HtmlElement selected = findById(page.getRootElement(), name, id);
            if (selected != null) {
                result.add(selected);
            }
            return result;
        }
        String[] path = expression.split("\\.");
        if (current == null || !current.getName().equalsIgnoreCase(path[0])) {
            return new ArrayList<HtmlElement>();
        }

        List<HtmlElement> elements = new ArrayList<HtmlElement>();
        elements.add(current);

        int i = 1;
        while (i < path.length && elements.size() > 0) {
            String pathElement = path[i];

            String name = pathElement;
            int elementIdx = -1;

            matcher = EXP_PATTERN.matcher(name);
            if (matcher.matches()) {
                name = matcher.group(1);
                elementIdx = Integer.valueOf(matcher.group(2));
            }

            List<HtmlElement> list = new ArrayList<HtmlElement>();
            for (HtmlElement elem : elements) {
                HtmlElement[] children = elem.getChildElements(name);
                if (elementIdx == -1) {
                    list.addAll(Arrays.asList(children));
                } else {
                    if (elementIdx >= 0 && elementIdx < children.length) {
                        list.add(children[elementIdx]);
                    }
                }
            }
            elements = list;
            i++;
        }
        return elements;
    }
    
    public static HtmlElement selectSingleElement(HtmlPage page, String expression) {
        List<HtmlElement> elements = select(page, expression);
        if (elements.isEmpty()) {
            return null;
        }
        return elements.get(0);
    }

    private static HtmlElement findById(HtmlElement elem, String name, String id) {
        HtmlAttribute attr = elem.getAttribute("id");
        if (attr != null) {
            if (attr.getValue().equals(id)) {
                if (name == null) {
                    return elem;
                }
                if (name.equalsIgnoreCase(elem.getName())) {
                    return elem;
                }
            }
        }
        HtmlElement[] children = elem.getChildElements();
        for (HtmlElement child: children) {
            HtmlElement found = findById(child, name, id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

}
