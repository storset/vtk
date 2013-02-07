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
 * </p>
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
     * Compiles a template with configurable delimiters.
     */
    public static SimpleTemplate compile(String template, String delimPrefix,
                                         String delimSuffix) {
        if (template == null) {
            throw new IllegalArgumentException("Argument 'template' is NULL");
        }
        if (delimPrefix == null) {
            throw new IllegalArgumentException("Argument 'prefix' is NULL");
        }
        if (delimSuffix == null) {
            throw new IllegalArgumentException("Argument 'suffix' is NULL");
        }
        List<Node> nodes = new ArrayList<Node>();
        int start = 0;
        int pos = 0;
        while (true) {
            pos = template.indexOf(delimPrefix, start);
            if (pos != -1) {
                String text = template.substring(start, pos);
                nodes.add(textNode(text));
                start = pos;
                pos = template.indexOf(delimSuffix, start);
                if (pos != -1) {
                    String var = template.substring(
                        start + delimPrefix.length(), pos);
                    
                    nodes.add(varNode(var));
                    start = pos + delimSuffix.length();
                }
            }
            if (pos == -1) {
                String text = template.substring(start);
                if (text.length() > 0)
                    nodes.add(textNode(text));
                break;
            }
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
