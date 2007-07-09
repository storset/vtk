/* Copyright (c) 2004, 2007, University of Oslo, Norway
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
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.jdom.Element;


/**
 * Controller that inserts input from the request into the focused
 * element and saves the document.
 *
 */
public class EditDoneController implements ActionHandler {

    public Map handle(HttpServletRequest request,
            EditDocument document,
            SchemaDocumentDefinition documentDefinition) throws IOException, XMLEditException {

        String mode = document.getDocumentMode();

        if (!mode.equals("edit")) 
            return null;
            
        String con = request.getParameter("cont");
        if ("true".equals(con)) {

                /* Add element values */
            document.setDocumentMode("default");
            document.addContentsToElement(document.getEditingElement(),
                    Util.getRequestParameterMap(request),
                    documentDefinition);
            document.resetEditingElement();
            document.setClone(null);

            document.save();

        } else {
            Element element = document.getEditingElement();
            Element clone = document.getClone();
            Element parent = (Element) element.getParent();
            int childIndex = parent.indexOf(element);
            element.detach();
            parent.addContent(childIndex, clone);

            document.setClone(null);
            document.setEditingElement(null);
            document.setDocumentMode("default");
        }
        return new HashMap();
    }

}
