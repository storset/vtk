/* Copyright (c) 2005, 2007, University of Oslo, Norway
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
package org.vortikal.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.ProcessingInstruction;
import org.jdom.Text;
import org.jdom.xpath.XPath;


public class Xml {
    /**
     * Get text nodes by running provided XPath expression on provided 
     * {@link Document}.
     * 
     * XXX: Uses unsafe conversion, assuming expression only returns text nodes!
     * 
     * @param doc
     * @return
     * @throws JDOMException
     */
    public static List<String> getNodesByXPath(Document doc, XPath expression)
            throws JDOMException {
        // get nodes
        List<Text> nodes = expression.selectNodes(doc);
        
        // Convert text nodes to strings and add to ArrayList
        List<String> textNodes = new ArrayList<String>();
        for (Text textNode: nodes) {
            textNodes.add(textNode.getText());
        }
        return textNodes;
    }
    
    public static Element findElementByNumericPath(Document document, String path) {
        Element currentElement = document.getRootElement();

        String currentPath = new String(path);
        if (currentPath.indexOf(".") >= 0) {
            // Strip away the leading '1.' (root element)
            currentPath = currentPath.substring(2, currentPath.length());
        }
        while (true) {
            int index = 0;
            if (currentPath.indexOf(".") == -1) {
                index = Integer.parseInt(currentPath);
            } else {
                index = Integer.parseInt(currentPath.substring(0, currentPath
                        .indexOf(".")));
            }
            currentElement = (Element) currentElement.getChildren().get(
                    index - 1);
            if (currentPath.indexOf(".") == -1) {
                break;
            }
            currentPath = currentPath.substring(currentPath.indexOf(".") + 1,
                    currentPath.length());
        }
        return currentElement;
    }
    
    public static String createNumericPath(Element element) throws IllegalArgumentException {
        if (element.isRootElement()) { return "1"; }

        Element parent = (Element) element.getParent();
        int index = 1;

        for (Iterator i = parent.getChildren().iterator(); i.hasNext();) {
            Element child = (Element) i.next();
            if (child == element) {
                break;
            }
            index++;
        }
        return createNumericPath(parent) + "." + index;
    }

    public static void removeProcessingInstruction(Element element, String target) {
        ProcessingInstruction pi = findProcessingInstruction(element, target);
        if (pi != null) element.removeContent(pi);
    }

    public static ProcessingInstruction findProcessingInstruction(Element element, String target) {
        for (Iterator<?> it = element.getContent().iterator(); it.hasNext();) {
            Object o = it.next();
            if ((o instanceof ProcessingInstruction)
                    && target.equals(((ProcessingInstruction) o).getTarget())) { 

                return (ProcessingInstruction) o; 
            }
        }
        return null;
    }

}
