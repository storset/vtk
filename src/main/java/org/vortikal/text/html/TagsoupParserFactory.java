/* Copyright (c) 2012, University of Oslo, Norway
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

import org.ccil.cowan.tagsoup.Parser;
import org.ccil.cowan.tagsoup.Schema;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 *
 */
public class TagsoupParserFactory {
    
    /**
     * Single HTMLSchema instance that can be re-used across all Tagsoup parser
     * instances due to heavy constructor which can impact performance.
     * If this instance is to be used, then the following parser feature MUST be
     * set to 'true' to keep thread safety:
     * {@link org.ccil.cowan.tagsoup.Parser#ignoreBogonsFeature}
     * 
     * See for instance: <a href="https://issues.apache.org/jira/browse/TIKA-599">TIKA-599</a>
     */
    private static final org.ccil.cowan.tagsoup.HTMLSchema TAGSOUP_HTML_SCHEMA = 
                                         new org.ccil.cowan.tagsoup.HTMLSchema();

    /**
     * Get a new Tagsoup {@link Parser} instance.
     *
     * @param ignoreBogons if <code>true</code> unknown elements (according to
     *                     HTML schema) will be ignored. This allows re-use of internal parser
     *                     {@link Schema} instance, which improves parser construction
     *                     performance.
     * 
     * @return A new Tagsoup {@link Parser}.
     */
    public static Parser newParser(boolean ignoreBogons) {
        org.ccil.cowan.tagsoup.Parser parser = new org.ccil.cowan.tagsoup.Parser();
        if (ignoreBogons) {
            try {
                parser.setProperty(Parser.schemaProperty, TAGSOUP_HTML_SCHEMA);
                parser.setFeature(Parser.ignoreBogonsFeature, true);
            } 
            catch (SAXNotRecognizedException saxException) {}
            catch (SAXNotSupportedException saxException) {}
        }
        return parser;
    }
}
