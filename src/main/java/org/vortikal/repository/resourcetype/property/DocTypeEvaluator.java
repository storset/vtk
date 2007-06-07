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
package org.vortikal.repository.resourcetype.property;

import java.util.Date;

import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.security.Principal;

import au.id.jericho.lib.html.Source;
import au.id.jericho.lib.html.StartTagType;
import au.id.jericho.lib.html.Tag;


public class DocTypeEvaluator extends AbstractJerichoHtmlContentEvaluator {

    private static final String DOCTYPE_START = "!DOCTYPE";
    
    
    protected boolean doContentModification(
        Principal principal, Property property, PropertySet ancestorPropertySet,
        Date time, Source source) throws PropertyEvaluationException {

        String doctype = findDocType(source);
        if (doctype == null) {
            return false;
        }
        property.setStringValue(doctype);
        return true;
    }

    private String findDocType(Source source) {
        Tag doctypeTag = source.findNextTag(0, StartTagType.DOCTYPE_DECLARATION);

        if (doctypeTag == null) {
            return null;
        }
        String doctype = doctypeTag.toString();
        if (doctype == null || "".equals(doctype.trim())) {
            return null;
        }
        doctype = doctype.trim();
        
        if (doctype.startsWith("<")) {
            doctype = doctype.substring(1);
        }

        String compareString = doctype.toUpperCase();

        if (compareString.contains(DOCTYPE_START)) {
            doctype = doctype.substring(doctype.indexOf(DOCTYPE_START)
                                              + DOCTYPE_START.length());
        }
        if (compareString.endsWith(">")) {
            doctype = doctype.substring(0, doctype.length() - 1);
        }
        doctype = doctype.trim();
        doctype = doctype.replaceAll("\r\n", " ");
        doctype = doctype.replaceAll("\n", " ");

        while (doctype.indexOf("  ") != -1) {
            doctype = doctype.replaceAll("  ", " ");
        }
        return doctype;
    }
    

}
