/* Copyright (c) 2007, 2008, University of Oslo, Norway
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
package org.vortikal.web.decorating;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * XXX: interface should be extended with the following concepts:
 * <ul>
 *   <li>Supported doctype(s) and possibly character encoding
 *   <li>Language
 *   <li>Cache model: dynamic/static/"private" (per-principal)
 * </ul>
 *
 */
public interface DecoratorComponent {

    public String getNamespace();

    public String getName();

    public String getDescription();

    public Map<String, String> getParameterDescriptions();
    
    public Collection<UsageExample> getUsageExamples();

    public void render(DecoratorRequest request, DecoratorResponse response)
        throws Exception;


    public static final class UsageExample {
        
        private String description = null;
        private List<Param> parameters = null;
        
        public String example(String name) {
            StringBuilder sb = new StringBuilder();
            sb.append("${");
            sb.append(name);
            if (this.parameters != null) {
                for (Param p: this.parameters) {
                    sb.append(" ").append(p.name)
                        .append("=[").append(p.value).append("]");
                }
            }
            sb.append("}");
            return sb.toString();
        }
        
        public String description() {
            return this.description;
        }
        
        public UsageExample(String syntax) {
            this(null, syntax);
        }
        
        public UsageExample(String description, String syntax) {
            if (description != null && !"".equals(description.trim())) {
                this.description = description;
            }
            State state = State.BETWEEN;
            
            List<Param> params = new ArrayList<Param>();
            
            Param cur = new Param();
            boolean escape = false;
            for (int i = 0; i < syntax.length(); i++) {
                char c = syntax.charAt(i);
                
                if (c == '\\') {
                    if (!escape) {
                        escape = true;
                        continue;
                    }
                }
                if (escape) {
                    if (c != '[' && c != ']' && c != '\\') {
                        throw new IllegalArgumentException(
                                "Illegal escape sequence at position " 
                        + (i - 1) + " in input string '" + syntax + "'");
                    }
                    escape = false;
                    if (state == State.PARAM_NAME) {
                        cur.name.append(c);
                        continue;
                    }
                    if (state == State.PARAM_VALUE) {
                        cur.value.append(c);
                        continue;
                    }
                    throw new IllegalArgumentException(
                            "Unexpected character '\\' at position " 
                                    + (i - 1) + " in input string '" + syntax + "'");
                }

                if (Character.isWhitespace(c)) {
                    if (state == State.BETWEEN) {
                        continue;
                    }
                    if (state == State.PARAM_NAME) {
                        cur.name.append(c);
                        continue;
                    }
                    if (state == State.PARAM_VALUE) {
                        cur.value.append(c);
                        continue;
                    }
                }
                if (c == '=') {
                    if (state == State.PARAM_NAME) {
                        state = State.EQ;
                        continue;
                    }
                }
                if (state == State.EQ && c != '[') {
                    throw new IllegalArgumentException(
                            "Unexpected character '[' at position " 
                                    + i + " in input string '" + syntax + "'");
                    
                }
                if (c == '[') {
                    if (state == State.EQ) {
                        state = State.PARAM_VALUE;
                        continue;
                    }
                    throw new IllegalArgumentException(
                            "Unexpected character '[' at position " 
                                    + i + " in input string '" + syntax + "'");
                }
                if (c == ']') {
                    if (state == State.PARAM_VALUE) {
                        params.add(cur);
                        cur = new Param();
                        state = State.BETWEEN;
                        continue;
                    }
                    throw new IllegalArgumentException(
                            "Unexpected character ']' at position " 
                                    + i + " in input string '" + syntax + "'");
                }
                // c == whatever:
                if (state == State.BETWEEN) {
                    cur.name.append(c);
                    state = State.PARAM_NAME;
                    continue;
                }
                if (state == State.PARAM_NAME) {
                    cur.name.append(c);
                    continue;
                }
                if (state == State.PARAM_VALUE) {
                    cur.value.append(c);
                    continue;
                }
            }
            if (state != State.BETWEEN) {
                if (cur.value.length() > 0) {
                    throw new IllegalArgumentException(
                            "Unterminated parameter: '" 
                    + cur.name + "[" + cur.value + "'");
                }
                throw new IllegalArgumentException(
                        "Unterminated parameter: '" + cur.name + "'");
            }
            this.parameters = Collections.unmodifiableList(params);
        }
        
        private static final class Param {
            StringBuilder name = new StringBuilder();
            StringBuilder value = new StringBuilder();
            public String toString() {
                return name + "=[" + value + "]";
            }
        }
        
        private enum State {
            BETWEEN, PARAM_NAME, EQ, PARAM_VALUE;
        }
    }
}
