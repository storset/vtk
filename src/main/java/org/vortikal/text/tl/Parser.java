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

import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Parser {

    private LineNumberReader reader;
    private Map<String, DirectiveNodeFactory> directives = new HashMap<String, DirectiveNodeFactory>();

    private enum State {Init, Text, Directive};

    public Parser(Reader reader, Map<String, DirectiveNodeFactory> directives) {
        this.reader = new LineNumberReader(reader);
        this.directives = directives;
    }

    private void illegalCharacter(int c, StringBuilder context) {
        throw new RuntimeException(
                "Illegal character '" + (char) c + "' at line " 
                + (this.reader.getLineNumber() + 1) + ": " 
                + context + (char) c);
    }
    
    private Token nextToken() throws Exception {

        State state = State.Init;
        boolean escape = false;

        StringBuilder buffer = new StringBuilder();

        while (true) {
            this.reader.mark(1);
            int c = this.reader.read();
            if (c == -1) {
                break;
            }
            if (c == '\\') {
                escape = true;
                continue;
            } 

            if (c == '[') {

                if (state == State.Init) {
                    if (escape) {
                        buffer.append('[');
                        state = State.Text;
                        escape = false;
                    } else {
                        state = State.Directive;
                    }

                } else if (state == State.Text) {
                    if (escape) {
                        buffer.append("[");
                        escape = false;
                    } else {
                        this.reader.reset();
                        break;
                    }

                } else if (state == State.Directive) {
                    if (escape) {
                        escape = false;
                    } else {
                        illegalCharacter(c, buffer);
                    }
                }
            } else if (c == ']') {
                if (state == State.Init) {
                    if (escape) {
                        buffer.append(']');
                        state = State.Text;
                        escape = false;
                    } else {
                        illegalCharacter(c, buffer);
                    }
                } else if (state == State.Text) {
                    if (escape) {
                        buffer.append("]");
                        escape = false;
                    } else {
                        illegalCharacter(c, buffer);
                    }                    
                } else if (state == State.Directive) {
                    if (escape) {
                        escape = false;
                    } else {
                        break;
                    }
                }

            } else {
                if (state == State.Init) {
                    state = State.Text;
                }
                if (escape) {
                    escape = false;
                } else {
                    buffer.append((char) c);
                }
            }
        }

        if (state == State.Init) {
            return null;
        }
        Token token = new Token();
        token.value = buffer.toString();

        if (state == State.Text) {
            token.type = Token.Type.Text;
        }
        if (state == State.Directive) {
            token.type = Token.Type.Directive;
            if (token.value.trim().length() == 0) {
                throw new IllegalStateException(
                        "Empty directive encountered at line " 
                        + (this.reader.getLineNumber() + 1));
            }
        }
        return token;
    }

    public ParseResult parse(Set<String> terminators) throws Exception {
        NodeList list = new NodeList();
        while (true) {
            Token token = nextToken();
            if (token == null) {
                break;
            }
            switch (token.type) {
            case Text:
                list.add(new TextNode(token.value));
                break;
            case Directive:
                List<String> split = token.split();
                String name = split.remove(0);

                if (this.directives.containsKey(name)) {
                    DirectiveNodeFactory nf = this.directives.get(name);
                    List<Argument> args = parseArguments(split);
                    DirectiveParseContext info = new DirectiveParseContext(name, this, args, token.value);
                    Node node = nf.create(info);
                    list.add(node);
                }

                if (terminators.contains(name)) {
                    return new ParseResult(list, name);
                }
            }
        }
        return new ParseResult(list);
    }

    private List<Argument> parseArguments(List<String> list) {
        List<Argument> result = new ArrayList<Argument>();
        for (String token: list) {
            try {
                result.add(new Literal(token));
            } catch (Throwable t) {
                // Unless token is a string, integer or boolean,
                // it is interpreted as a symbol:
                result.add(new Symbol(token));
            }
        }
        return result;
    }
}
