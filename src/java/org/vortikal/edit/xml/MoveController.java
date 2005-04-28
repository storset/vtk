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

import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom.Element;
import org.jdom.ProcessingInstruction;
import org.springframework.web.servlet.ModelAndView;




/**
 * Controller that marks elements for moving, based on request input.
 *
 * @version $Id$
 */
public class MoveController extends AbstractXmlEditController {



    protected ModelAndView handleRequestInternal(
        HttpServletRequest request, HttpServletResponse response,
        EditDocument document, SchemaDocumentDefinition documentDefinition) {

        Map model = new HashMap();
        String mode = document.getDocumentMode();

        if (mode.equals("default")) {
            Enumeration enumeration = request.getParameterNames();
            Vector v = new Vector();
            while (enumeration.hasMoreElements()) {
                String s = (String) enumeration.nextElement();
                if (s.matches("\\d+(\\.\\d+)*")) {
                    v.add(s);
                    logger.debug("Marking element " + s + " for moving");
                }
            }
            Object[] array = v.toArray();
            java.util.Arrays.sort(array, new PathComparator());
            List l = java.util.Arrays.asList(array);
            v = new Vector();
            for (Iterator it = l.iterator(); it.hasNext();) {
                String param = (String) it.next();
                Element e = document.findElementByPath(param);
                e.addContent(new ProcessingInstruction("marked", "true"));
                v.add(e);
            }
            if (v.size() > 0) {
                document.setDocumentMode("move");
                document.setElements(v);
            } else {
                setXsltParameter(model,"ERRORMESSAGE", "MISSING_ELEMENTS_TO_MOVE");
            }
            return new ModelAndView(viewName, model);
        }
        return null;
    }

    private class PathComparator implements Comparator {

        public PathComparator() {
        }

        public int compare(Object o1, Object o2) {

            String s1 = (String) o1;
            String s2 = (String) o2;

            StringTokenizer st1 = new StringTokenizer(s1, ".");
            StringTokenizer st2 = new StringTokenizer(s2, ".");

            while (st1.hasMoreElements() && st2.hasMoreElements()) {
                s1 = st1.nextToken();
                s2 = st2.nextToken();
                try {
                    int i1 = Integer.parseInt(s1);
                    int i2 = Integer.parseInt(s2);

                    if (i1 != i2) {
                        return new Integer(i1).compareTo(new Integer(i2));
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                        "Unable to compare objects "
                        + o1 + ", " + o2 + ": paths must must consist "
                        + "of numbers, separated by dots");
                }
            }

            return new Integer(st1.countTokens()).compareTo(new Integer(st2
                    .countTokens()));
        }
    }
}
