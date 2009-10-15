/* Copyright (c) 2009, University of Oslo, Norway
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
package org.vortikal.text.tl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ParseNode {
    public enum Type {Text, Directive};
    public Type type;
    public String value;

    public String toString() {
        if (this.type == Type.Text) {
            return "text('" + this.value + "')";
        }
        return "directive('" + this.value + "')";
    }

    /**
     * The set of characters that are reserved 
     * (i.e. treated as separate symbols and thus 
     * not allowed to be part of other symbols)
     */
    public static final Set<Character> RESERVED_CHARS; 
    
    static {
        RESERVED_CHARS = new HashSet<Character>();
        RESERVED_CHARS.add('(');
        RESERVED_CHARS.add(')');
        RESERVED_CHARS.add('{');
        RESERVED_CHARS.add('}');
        RESERVED_CHARS.add(':');
        RESERVED_CHARS.add(',');
    }
    
    public List<String> split() {
        if (this.type != Type.Directive) {
            throw new IllegalStateException("Cannot split a text token");
        }
        List<String> tokenList = new ArrayList<String>();
        
        boolean dquote = false, squote = false;

        StringBuilder cur = new StringBuilder();

        for (int i = 0; i < this.value.length(); i++) {
            char c = this.value.charAt(i);

            if (c == '"') {
                if (!squote) {
                    dquote = !dquote;
                }
                cur.append(c);
                
            } else if (c == '\'') {
                if (!dquote) {
                    squote = !squote;
                }
                cur.append(c);
                
            } else if (c == ' ' || c == '\n' || c == '\r') {
                if (dquote || squote) {
                    cur.append(c);
                } else {
                    if (cur.length() > 0) {
                        tokenList.add(cur.toString());
                        cur.delete(0, cur.length());
                    }
                }
            } else if (RESERVED_CHARS.contains(c)) {
                if (dquote || squote) {
                    cur.append(c);
                } else {
                    if (cur.length() > 0) {
                        tokenList.add(cur.toString());
                        cur.delete(0, cur.length());
                    }
                    tokenList.add(String.valueOf(c));
                }
            } else {
                cur.append(c);
            }
        }

        if (cur.length() > 0) {
            tokenList.add(cur.toString());
        }
        return tokenList;
    }
}
