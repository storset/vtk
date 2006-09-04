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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.jdom.Element;
import org.jdom.ProcessingInstruction;

/**
 * 
 * if mode is 'default', finds the elements marked for deletion. if mode is
 * 'delete', and cont is set to true, delete selected elements
 */
public class DeleteController implements ActionHandler {

    public Map handleRequestInternal(HttpServletRequest request,
            EditDocument document,
            SchemaDocumentDefinition documentDefinition) throws IOException, XMLEditException {

        String mode = document.getDocumentMode();

        Map model = new HashMap();

        if (mode.equals("default")) {
            Enumeration enumeration = request.getParameterNames();

            Vector v = new Vector();
            while (enumeration.hasMoreElements()) {
                String param = (String) enumeration.nextElement();
                if (param.matches("\\d+(\\.\\d+)*")) {
                    Element e = document.findElementByPath(param);
                    e.addContent(new ProcessingInstruction("marked", "true"));
                    v.add(e);
                }
            }
            if (v.size() > 0) {
                document.setDocumentMode("delete");
                document.setElements(v);
            } else
                XmlEditController.setXsltParameter(
                        model,"ERRORMESSAGE", "MISSING_ELEMENTS_FOR_DELETION");

            return model;
        
        } else if (mode.equals("delete")) {
            String con = request.getParameter("cont");
            if ("true".equals(con)) {

                /* Delete elements */
                Enumeration enumeration = document.getElements().elements();
                while (enumeration.hasMoreElements()) {
                    Element e = (Element) enumeration.nextElement();
                    e.detach();
                }

                document.setDocumentMode("default");
                document.resetElements();
                document.save();
            } else {
                document.setDocumentMode("default");
                document.resetElements();
            }
            return model;
        }
        return null;
    }
}
