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
     * Compiles a template using <code>${</code> and <code>}</code> as delimiters.
     */
    public static SimpleTemplate compile(String template) {
        return compile(template, "${", "}");
    }

    /**
     * Compiles a template with configurable placeholder delimiters. You may escape
     * placeholder interpretation by preceding <code>delimPrefix</code> with
     * backslash in template string: <code>"foo \\${literal-me}"</code>. Escaping
     * chars with no special meaning has no effect other than the backslash
     * being removed. To get a literal backslash in rendering output, use two
     * consecutive backslashes in template.
     * 
     * Nesting placeholders is entirely unsupported.
     */
    public static SimpleTemplate compile(String template, String delimPrefix,
                                         String delimSuffix) {
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
            if (escape) {
                token.append(c);
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
                continue;
            }
            if (!varState && template.startsWith(delimPrefix, i)) {
                if (token.length() > 0) {
                    nodes.add(textNode(token.toString()));
                    token.setLength(0);
                }
                varState = true;
                i += delimPrefix.length()-1;
                continue;
            }
            if (varState && template.startsWith(delimSuffix, i)) {
                if (token.length() > 0) {
                    nodes.add(varNode(token.toString()));
                    token.setLength(0);
                }
                varState = false;
                i += delimSuffix.length()-1;
                continue;
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
    }
    
    private SimpleTemplate(List<Node> nodes) {
        this.nodes = nodes;
    }

}
