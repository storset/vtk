/* Copyright (c) 2013, University of Oslo, Norway
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
package org.vortikal.util.text;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple string template with variable substitution. 
 * 
 * <p>Configurable prefix and suffix delimiters (e.g. <code>${</code> and
 * <code>}</code>), allowing different styles of template syntax:
 * <ul>
 * <li><code>${foobar}</code>
 * <li><code>%{foobar]</code>
 * <li><code>...</code>
 * </ul> 
 * </p>
 * <p>Variable substitution and output writing is controlled via a {@link Handler} 
 * implementation supplied by the caller.</p>
 * 
 * <p>Sample usage:
 * <pre>
 *   SimpleTemplate t = new SimpleTemplate("My template with a ${variable}.");
 *   t.apply(new SimpleTemplate.Handler() {
 *       public String resolve(String variable) {
 *           return "piece of text";
 *       }
 *       public void write(String text) {
 *           System.out.print(text);
 *       }
 *   });
 * </pre>
 * 
 * <p>A backslash in template will prevent any interpretation of the next
 * character in input and can be used to include substitution
 * prefix literally in output. To get a literal backslash in output, use two
 * consecutive backslashes in template.
  * 
 */
public class SimpleTemplate {

    /**
     * The character used for escaping interpretation of delimiter prefix in template.
     */
    public static final char ESCAPE_CHAR = '\\';
    
    /**
     * Parsing flag that disables all handling of escaping. This will make
     * outputting a literal string containing the prefix or suffix of variable
     * delimiter impossible (unless output by a variable itself), since the
     * delimiters will always be interpreted as parts of variable expression,
     * regardless of any preceding escape character. Also, all escape characters
     * that occur in template input will be included in rendered output.
     */
    public static final int NO_ESCAPE_HANDLING = 1;
    
    /**
     * Parsing flag meaning that <em>all</em> backslash characters in input are
     * kept as-is in template output. The backslash will still have the effect
     * of escaping the variable delimiter prefix.
     */
    public static final int KEEP_ALL_ESCAPE_CHARS = 2;
    
    /**
     * Parsing flag meaning that the escape char is kept as-is in output
     * when it forms an invalid escape sequence with the following character.
     * This will keep all backslashes in template input, except those that
     * form valid escapes.
     * <p>
     * Valid escape sequences are:
     * <ol>
     *   <li>An escape character preceding the delimiter prefix when outside variable.
     *   <li>An escape character preceding the delimiter suffix when inside a variable.
     *   <li>An escape character escaping itself.
     * </ol>
     * Escape chars in valid escape sequences will be consumed in this mode of
     * operation.
     */
    public static final int KEEP_INVALID_ESCAPE_CHARS = 4;

    /**
     * Handler for template variable substitution and output writing.
     */
    public static interface Handler {
    
        /**
         * Called to resolve a template variable.
         */
        public String resolve(String variable);

        /**
         * Requests a piece of text be written to output.
         */
        public void write(String text);
    }

    /**
     * Applies a compiled template using a supplied {@link Handler}
     * @param handler
     */
    public void apply(Handler handler) {
        for (Node node: nodes) {
            String s = node.text;
            if (node.var) {
                s = handler.resolve(s);
            }
            if (s != null) {
                handler.write(s);
            }
        }
    }

    /**
     * Compiles a template using <code>${</code> and <code>}</code> as delimiters
     * and default escape handling (no flags).
     * @see #compile(java.lang.String, java.lang.String, java.lang.String, int) 
     */
    public static SimpleTemplate compile(String template) {
        return compile(template, "${", "}");
    }

    /**
     * Compiles a template using custom delimiters and default escape handling (no flags).
     * @see #compile(java.lang.String, java.lang.String, java.lang.String, int) 
     */
    public static SimpleTemplate compile(String template, String delimPrefix,
                                         String delimSuffix) {
        return compile(template, delimPrefix, delimSuffix, 0);
    }
    
    /**
     * Compiles a template with custom placeholder delimiters. You may
     * escape placeholder interpretation by preceding
     * <code>delimPrefix</code> with {@link #ESCAPE_CHAR the escape character} in
     * template string:
     * <code>"foo \\${literal-me}"</code>. Escaping chars with no special
     * meaning has no effect other than the backslash being removed. To get a
     * literal backslash in rendering output, use two consecutive backslashes in
     * template, or use a parsing flag which sets escape char policy explicitly.
     * <p>
     * Parsing flags that can be used:
     * <ul>
     * <li>0 - the default, escape handling is turned on, and one level of escape
     *         chars will be consumed, whether they are invalid nor not.
     * <li>{@link #NO_ESCAPE_HANDLING} - turn off escaping support. All escape chars will be included in output, and
     *                                   it will not be possible to escape delimiter prefix or suffix.
     * <li>{@link #KEEP_ALL_ESCAPE_CHARS} - allow escaping, and keep all escape chars verbatim in output as well.
     * <li>{@link #KEEP_INVALID_ESCAPE_CHARS} - allow escaping, and keep all escape chars that form invalid/unknown
     *                                          escape sequences in output. Those escape chars that actually
     *                                          do escape variable interpretation are consumed.
     * </ul>
      * 
     * Nesting placeholders is entirely unsupported.
     */
    public static SimpleTemplate compile(String template, String delimPrefix,
                                         String delimSuffix, int parseFlags) {
        if (template == null) {
            throw new IllegalArgumentException("Argument 'template' is null");
        }
        if (delimPrefix == null || delimPrefix.isEmpty()) {
            throw new IllegalArgumentException("Argument 'delimPrefix' null or empty");
        }
        if (delimSuffix == null || delimSuffix.isEmpty()) {
            throw new IllegalArgumentException("Argument 'delimSuffix' null or empty");
        }
        List<Node> nodes = new ArrayList<Node>();
        StringBuilder token = new StringBuilder();
        boolean escape = false;
        boolean varState = false;
        for (int i=0; i<template.length(); i++) {
            char c = template.charAt(i);
            if (c == ESCAPE_CHAR) {
                if (escape) {
                    token.append(ESCAPE_CHAR); // Valid escape
                    escape = false;
                } else {
                    if (hasFlag(NO_ESCAPE_HANDLING|KEEP_ALL_ESCAPE_CHARS, parseFlags)) {
                        token.append(ESCAPE_CHAR);
                    }
                    escape = !hasFlag(NO_ESCAPE_HANDLING, parseFlags);
                }
                continue;
            }
            if (!varState && template.startsWith(delimPrefix, i)) {
                if (escape) { // Valid escape
                    escape = false;
                    token.append(c);
                    continue;
                }
                if (token.length() > 0) {
                    nodes.add(textNode(token.toString()));
                    token.setLength(0);
                }
                varState = true;
                i += delimPrefix.length()-1;
                continue;
            }
            if (varState && template.startsWith(delimSuffix, i)) {
                if (escape) { // Valid escape
                    escape = false;
                    token.append(c);
                    continue;
                }
                if (token.length() > 0) {
                    nodes.add(varNode(token.toString()));
                    token.setLength(0);
                }
                varState = false;
                i += delimSuffix.length()-1;
                continue;
            }
            if (escape) {
                if (hasFlag(KEEP_INVALID_ESCAPE_CHARS, parseFlags) 
                         && !hasFlag(KEEP_ALL_ESCAPE_CHARS, parseFlags)) {
                    token.append(ESCAPE_CHAR);
                }
                escape = false;
            }
            token.append(c);
        }
        if (token.length() > 0 || varState) {
            if (varState) {
                // Incomplete placeholder syntax at end of input, output raw
                token.insert(0, delimPrefix);
            }
            nodes.add(textNode(token.toString()));
        }
        
        return new SimpleTemplate(nodes);
    }
    
    private List<Node> nodes;
    
    private static Node textNode(String text) {
        return new Node(text, false);
    }
    
    private static Node varNode(String text) {
        return new Node(text, true);
    }
    
    private static class Node {
        boolean var = false;
        String text;
        private Node(String text, boolean var) {
            this.text = text;
            this.var = var;
        }

        @Override
        public String toString() {
            return var ? "var:" + text : "text:\"" + text + "\"";
        }
    }
    
    private SimpleTemplate(List<Node> nodes) {
        this.nodes = nodes;
    }

    private static boolean hasFlag(int flag, int flags) {
        return (flag & flags) != 0;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + nodes.toString();
    }

}
