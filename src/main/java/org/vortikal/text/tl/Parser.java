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

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Parser {

    private PeekableReader reader;
    private Map<String, DirectiveNodeFactory> directives = new HashMap<String, DirectiveNodeFactory>();
    private Map<String, Object> attributes = new HashMap<String, Object>();

    public Parser(Reader reader, Map<String, DirectiveNodeFactory> directives) {
        this.reader = new PeekableReader(reader);
        this.directives = directives;
    }
    
    /**
     * Parse input until EOF.
     */
    public ParseResult parse() throws Exception {
        return parse(Collections.<String>emptySet());
    }

    /**
     * Parse input until one of the supplied terminators are encountered.
     */
    public ParseResult parse(String... terminators) throws Exception {
        Set<String> set = new HashSet<String>(Arrays.asList(terminators));
        return parse(set);
    }
    
    /**
     * Parse input until one of the supplied terminators are encountered.
     */
    public ParseResult parse(Set<String> terminators) throws Exception {
        NodeList list = new NodeList();
        while (true) {
            ParseNode parseNode = nextNode();
            if (parseNode == null) {
                break;
            }
            switch (parseNode.type) {
            case Text:
                list.add(new TextNode(parseNode.text));
                break;
            case Comment:
                list.add(new CommentNode(parseNode.text));
                break;
            case Raw:
                list.add(new RawNode(parseNode.text));
                break;
            case Directive:
                String name = parseNode.name;
                List<Argument> args = parseNode.arguments;
                DirectiveParseContext info = new DirectiveParseContext(name, this, args, parseNode.text);

                if (this.directives.containsKey(name)) {
                    DirectiveNodeFactory nf = this.directives.get(name);
                    Node node = nf.create(info);
                    list.add(node);
                } else if (this.directives.containsKey("*")) {
                    DirectiveNodeFactory nf = this.directives.get("*");
                    Node node = nf.create(info);
                    list.add(node);
                }
                if (terminators.contains(name)) {
                    return new ParseResult(list, info);
                }
            }
        }
        return new ParseResult(list);
    }

    public int getLineNumber() {
        return this.reader.getLineNumber() + 1;
    }

    private void error(String msg) {
        throw new RuntimeException("Error at line " + getLineNumber() + ": " + msg);
    }
    
    public void setAttribute(String name, Object attribute) {
        this.attributes.put(name, attribute);
    }
    
    public Object getAttribute(String name) {
        return this.attributes.get(name);
    }
        
    private ParseNode nextNode() throws Exception {
        int c = this.reader.peek(1);
        if (c == -1) {
            return null;
        }
        if (isComment()) {
            return parseComment();
            
        } else if (isDirective()) {
            return parseDirective();
            
        } else if (isRawNode()) {
            return parseRawNode();
        }
        return parseText();
    }
    
    private ParseNode parseComment() throws IOException {
        // reader.skip() does not work after "\r\n":
        for (int i = 0; i < "[!--".length(); i++) {
            this.reader.read();
        }
        StringBuilder buf = new StringBuilder();
        while (true) {
            int c = this.reader.read();
            if (c == -1) {
                break;
            }
            if (c == '-' && this.reader.lookingAt("-]")) {
                this.reader.skip("-]".length());
                break;
            }
            buf.append((char) c);
        }
        return new ParseNode(buf.toString(), ParseNode.Type.Comment);
    }
    
    private ParseNode parseText() throws IOException {
        StringBuilder buf = new StringBuilder();
        while (true) {
            int c = this.reader.read();
            if (c == -1) {
                break;
            } else if (isComment()) {
                buf.append((char) c);
                break;
            } else if (isRawNode()) {
                buf.append((char) c);
                break;
            } else if (isDirective()) {
                buf.append((char) c);
                break;
            }
            buf.append((char) c);
        }
        return new ParseNode(buf.toString(), ParseNode.Type.Text);
    }


    private ParseNode parseRawNode() throws IOException {
        // reader.skip() does not work after "\r\n":
        for (int i = 0; i < "[#--".length(); i++) {
            this.reader.read();
        }
        StringBuilder buf = new StringBuilder();
        while (true) {
            int c = this.reader.read();
            if (c == -1) {
                error("Unterminated directive: [#--" + buf.toString());
            }
            if (c == '-' && this.reader.lookingAt("-]")) {
                this.reader.skip("-]".length());
                break;
            }
            buf.append((char) c);
        }
        return new ParseNode(buf.toString(), ParseNode.Type.Raw);
    }

    /**
     * The set of characters that are reserved 
     * (i.e. treated as separate symbols and thus 
     * not allowed to be part of other symbols)
     */
    public static final Set<Character> TOKENIZED_CHARS; 
    
    static {
        TOKENIZED_CHARS = new HashSet<Character>();
        TOKENIZED_CHARS.add('(');
        TOKENIZED_CHARS.add(')');
        TOKENIZED_CHARS.add('{');
        TOKENIZED_CHARS.add('}');
        TOKENIZED_CHARS.add(':');
        TOKENIZED_CHARS.add(',');
        TOKENIZED_CHARS.add('.');
    }
    
    
    private ParseNode parseDirective() throws Exception {
        // skip(1) does not work properly after reading "\r\n",
        // so use read() instead:
        this.reader.read();
        
        StringBuilder nodeText = new StringBuilder();
        StringBuilder curToken = new StringBuilder();
        List<String> tokens = new ArrayList<String>();
        
        boolean dquote = false, squote = false, escape = false;
        while (true) {
            int i = this.reader.read();
            if (i == -1) {
                error("Unterminated directive: [" + nodeText);
            }
            char c = (char) i;
            nodeText.append(c);
            if (c == '\\') {
                if (!escape) {
                    escape = true;
                    continue;
                }
            }
            if (escape) {
                if (c != '"' && c != '\'' && c != '\\') {
                    error("Illegal escape sequence: '\\" + c + "'");
                }
            }
            if (c == ' ' || c == '\t' || c == '\n') {
                if (dquote || squote) {
                    curToken.append(c);
                } else {
                    if (curToken.length() > 0) {
                        tokens.add(curToken.toString());
                        curToken.delete(0, curToken.length());
                    }
                }
            } else if (TOKENIZED_CHARS.contains(c)) {
                if (dquote || squote) {
                    curToken.append(c);
                } else {
                    if (curToken.length() > 0) {
                        tokens.add(curToken.toString());
                        curToken.delete(0, curToken.length());
                    }
                    tokens.add(String.valueOf(c));
                }
                
            } else if (c == '"') {
                if (escape) {
                    escape = false;
                    curToken.append('\\');
                } else if (!squote) {
                    dquote = !dquote;
                }
                curToken.append(c);
                
            } else if (c == '\'') {
                if (escape) {
                    escape = false;
                    curToken.append('\\');
                } else if (!dquote) {
                    squote = !squote;
                }
                curToken.append(c);
            } else if (c == '\\') {
                escape = false;
                curToken.append(c);
                
            } else if (c == ']' && !dquote && !squote) {
                if (curToken.length() > 0) {
                    tokens.add(curToken.toString());
                }
                nodeText.deleteCharAt(nodeText.length() - 1);
                break;
                
            } else {
                curToken.append(c);
            }
        }
        if (tokens.isEmpty()) {
            error("Empty directive");
        }        
        String name = tokens.remove(0);
        List<Argument> args = parseArguments(tokens);
        return new ParseNode(name, nodeText.toString(), args);
    }

    
    public List<String> splitDirectiveArgs(String value) {
        List<String> tokenList = new ArrayList<String>();

        boolean dquote = false, squote = false;

        StringBuilder cur = new StringBuilder();

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

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
            } else if (TOKENIZED_CHARS.contains(c)) {
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
    
    private boolean isComment() throws IOException {
        return this.reader.lookingAt("[!--");
    }

    
    private boolean isDirective() throws IOException {
        if (this.reader.lookingAt("[")) {
            char n = (char) reader.peek(2);
            // Allow [a-z], [A-Z], [/]
            if ((n >= 'a' && n <= 'z') || (n >= 'A' && n <= 'Z') || n == '/') {
                return true;
            }
        }
        return false;
    }
    
    private boolean isRawNode() throws IOException {
        return this.reader.lookingAt("[#--");
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
    
    
    private class PeekableReader extends LineNumberReader {

        public PeekableReader(Reader in) {
            super(in);
        }
        
        /**
         * Peeks N characters ahead, without changing the 
         * current position in the reader.
         * 
         * Note: Any mark set on the reader will be reset.
         *  
         * @param n the number of characters to peek
         * @return the character at position n, or -1 if EOF is encountered
         * @throws IOException
         */
        public int peek(int n) throws IOException {
            mark(n + 1);
            try {
                for (int i = 0; i < n - 1; i++) {
                    int c = read();
                    if (c == -1) {
                        return -1;
                    }
                }
                return read();
            } finally {
                reset();
            }
        }
        
        /**
         * Checks whether the next characters to be read 
         * from this reader comprise a given string, without 
         * changing the current position.
         * 
         * Note: Any mark set on the reader will be reset.
         * .
         * @param str the string to check against
         * @return whether the string matches
         * @throws IOException
         */
        public boolean lookingAt(String str) throws IOException {
            int n = str.length();
            this.mark(n + 1);
            try {
                for (int i = 0; i < n; i++) {
                    int c = read();
                    if (c == -1) {
                        return false;
                    }
                    if (str.charAt(i) != (char) c) {
                        return false;
                    }
                }
                return true;
            } finally {
                reset();
            }
        }
    }
    
}
