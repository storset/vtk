/* Copyright (c) 2011, University of Oslo, Norway
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
package org.vortikal.context;

import java.util.HashMap;
import java.util.Map;

/**
 * Creates mappings from string values to string values. 
 * 
 * <p>Syntax is
 * <code>key:value</code>. Colons in keys and values have to be
 * escaped using a backslash (<code>\:</code>). Entries without a 
 * colon are interpreted as mappings with <code>null</code> 
 * as the value.</p>
 * 
 */
public class CSVMapFactoryBean extends AbstractCSVFactoryBean {

    @Override
    protected Object createInstance() throws Exception {
        Map<String, String> map = new HashMap<String, String>();

        for (String element: super.elements) {
            String[] mapping = parseMapping(element);
            map.put(mapping[0], mapping[1]);
        }
        return map;
    }

    @SuppressWarnings({"rawtypes" })
    public Class getObjectType() {
        return Map.class;
    }

    private String[] parseMapping(String string) {
        StringBuilder key = new StringBuilder();
        StringBuilder value = null;
        StringBuilder cur = key;
        boolean escape = false;
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (cur == value && value == null) {
                value = new StringBuilder();
                cur = value;
            }
            if (c == '\\' && escape) {
                cur.append('\\');
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
                continue;
            }
            if (c == ':' && escape) {
                cur.append(c);
                escape = false;
                continue;
            }
            if (c == ':' && cur == value) {
                throw new IllegalArgumentException(
                        "Unescaped colon in mapping value: '" + string + "'");
            }
            if (escape) {
                throw new IllegalArgumentException("Illegal escape char: '\\" 
                        + c + "' in mapping: " + string);
            }
            if (c == ':') {
                value = new StringBuilder();
                cur = value;
                continue;
            }
            cur.append(c);
        }
        if (escape) {
            throw new IllegalArgumentException(
                    "Illegal escape sequence '\\' at end of mapping: '" + string + "'");
        }
        return new String[] { key.toString(), value == null ? null : value.toString() };
    }
}
