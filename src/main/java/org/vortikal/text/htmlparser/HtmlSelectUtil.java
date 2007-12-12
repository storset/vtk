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
package org.vortikal.text.htmlparser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.vortikal.text.html.HtmlElement;
import org.vortikal.text.html.HtmlPage;


class HtmlSelectUtil {

    private static final Pattern EXP_PATTERN = Pattern.compile("([a-z0-9]+)\\(([0-9]+)\\)$");
    
    
    public static List<HtmlElement> select(HtmlPage page, String expression) {
        HtmlElement current = page.getRootElement();

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

            Matcher matcher = EXP_PATTERN.matcher(name);
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
}
