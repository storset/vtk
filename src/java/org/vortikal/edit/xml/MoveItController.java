/* Copyright (c) 2004, University of Oslo, Norway
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
package org.vortikal.edit.xml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.jdom.Element;
import org.vortikal.util.Xml;

/**
 * Controller that moves elements marked for moving to a new location based on
 * request input.
 */
public class MoveItController implements ActionHandler {

    public Map handleRequestInternal(HttpServletRequest request,
            EditDocument document, SchemaDocumentDefinition documentDefinition)
            throws IOException, XMLEditException {

        Map model = new HashMap();
        String mode = document.getDocumentMode();

        if (!mode.equals("move"))
            return null;

        String con = request.getParameter("cont");
        if ("true".equals(con)) {

            /* find out where the elements should go: */
            String path = request.getParameter("to");

            if (path == null) {
                XmlEditController.setXsltParameter(model,
                        "ERRORMESSAGE", "MOVE_IT_MISSING_PATH_PARAMETER");
                return model;
            }

            /* prepare insertion in the new location: */
            String currentPath = path;
            if (currentPath.indexOf(".") >= 0) {
                // Strip away the leading '1.' (root element)
                currentPath = currentPath.substring(2, currentPath.length());
            }

            int index = 0;
            Element currentElement = document.getRootElement();
            while (true) {
                if (currentPath.indexOf(".") == -1) {
                    /* found the parent element */
                    index = Integer.parseInt(currentPath);
                    break;
                }
                index = Integer.parseInt(currentPath.substring(0, currentPath
                        .indexOf(".")));
                currentElement = (Element) currentElement.getChildren().get(
                        index - 1);
                currentPath = currentPath.substring(
                        currentPath.indexOf(".") + 1, currentPath.length());
            }

            /* actually insert the elements: */
            Enumeration enumeration = document.getElements().elements();
            List turnedElements = new ArrayList();

            /* Flip the elements (must be reversed) */
            while (enumeration.hasMoreElements()) {
                turnedElements.add(0, enumeration.nextElement());
            }

            List l = currentElement.getChildren();

            for (Iterator it = turnedElements.iterator(); it.hasNext();) {
                Element elem = (Element) it.next();

                Element clone = (Element) elem.clone();
                l.add(index, clone);
                Xml.removeProcessingInstruction(clone, "marked");
                document.resetElements(new Vector(clone.getChildren()));
            }

            enumeration = document.getElements().elements();
            while (enumeration.hasMoreElements()) {
                Element elem = (Element) enumeration.nextElement();
                elem.detach();
                l.remove(elem);
            }

            ArrayList newChildren = new ArrayList();
            for (Iterator i = l.iterator(); i.hasNext();) {
                Element e = (Element) i.next();
                newChildren.add(e.clone());
            }
            currentElement.removeContent();
            currentElement.setContent(newChildren);
            document.setDocumentMode("default");
            document.resetElements();

            document.save();
        } else {
            document.setDocumentMode("default");
            document.resetElements();
        }
        
        return model;
    }

}
