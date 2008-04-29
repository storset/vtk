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
package org.vortikal.repository.resourcetype;

import java.util.Locale;

import org.vortikal.text.htmlparser.HtmlFlattener;

/**
 * This value formatter represents HTML value types. 
 * It supports a single format, namely the <code>escaped</code> format,
 * producing HTML escaping of markup.
 */
public class HtmlValueFormatter implements ValueFormatter {

    private static final String ESCAPED_FORMAT = "escaped";
    private static final String FLATTENED_FORMAT = "flattened";

    private HtmlFlattener htmlFlattener;
    
    public String valueToString(Value value, String format, Locale locale)
            throws IllegalValueTypeException {
        String html = value.toString();
        if (ESCAPED_FORMAT.equals(format)) {
            return escape(html);
        } else if (FLATTENED_FORMAT.equals(format) && this.htmlFlattener != null) {
            return this.htmlFlattener.flatten(html).toString();
        }
        return html;
    }

    public Value stringToValue(String string, String format, Locale locale) {
        if (ESCAPED_FORMAT.equals(format)) {
            return new Value(unescape(string));
        } 
        
        return new Value(string);
    }

    private String escape(String html) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < html.length(); i++) {
            char c = html.charAt(i);
            switch (c) {
            case '&':
                result.append("&amp;");
                break;
            case '"':
                result.append("&quot;");
                break;
            case '<':
                result.append("&lt;");
                break;
            case '>':
                result.append("&gt;");
                break;
            default:
                result.append(c);
                break;
            }
        }
        return result.toString();
    }
    
    
    private String unescape(String html) {
        html = html.replaceAll("&amp;", "&");
        html = html.replaceAll("&quot;", "\"");
        html = html.replaceAll("&lt;", "<");
        html = html.replaceAll("&gt;", ">");
        return html;
    }

    
    public void setHtmlFlattener(HtmlFlattener htmlFlattener) {
        this.htmlFlattener = htmlFlattener;
    }

}
