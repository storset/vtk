/* Copyright (c) 2008, University of Oslo, Norway
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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.vortikal.repository.Path;


public class PathMappingConfig {
    
    private Node root = new Node();
    private int maxLineSize = 300;
    private int maxLines = 10000;
    
    public PathMappingConfig(InputStream source) throws Exception {
        load(source);
    }
    
    public List<ConfigEntry> get(Path path) {
        Node n = root;
        for (String name: path.getElements()) {
            if (name.equals("/")) continue;
            if (n.children != null) {
                n = n.children.get(name);
            }
            if (n == null) {
                break;
            }
        }
        if (n != null) {
            return n.entries;
        }
        return null;
    }

    public void load(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        try {
            String line = null;
            Node root = new Node();
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                if (++lineNum > this.maxLines) {
                    throw new IllegalStateException("File contains too many lines.");
                }
                if (line.length() > this.maxLineSize) {
                    throw new IllegalStateException(
                            "Line number " + lineNum + " is too long");
                }
                line = line.trim();
                line = stripComments(line);
                if ("".equals(line)) {
                    continue;
                }
                if (line.startsWith("/")) {
                    parseLine(line, root);
                } 
            }
            this.root = root;
        } finally {
            reader.close();
        }
    }

    private void parseLine(String line, Node root) {
        int separatorPos = line.indexOf('=');
        if (separatorPos == -1) {
            return;
        }
        if (separatorPos == 0 || separatorPos == line.length() - 1) {
            return;
        }
        String lhs = line.substring(0, separatorPos).trim();
        String rhs = line.substring(separatorPos + 1).trim();
        if ("".equals(lhs) || "".equals(rhs)) {
            return;
        }
        buildNode(lhs, rhs, root);
    }
    
    private void buildNode(String lhs, String rhs, Node root) {
        boolean exact = false;
        if (lhs.endsWith("/") && !lhs.equals("/")) {
            lhs = lhs.substring(0, lhs.length() - 1);
            exact = true;
        }

        int pathEndPos = lhs.length();
        int leftBracketPos = lhs.indexOf("[");
        int rightBracketPos = -1;
        if (leftBracketPos != -1) {
            rightBracketPos = lhs.indexOf(']');
            if (rightBracketPos == -1 || rightBracketPos < leftBracketPos) {
                return;
            }
            pathEndPos = leftBracketPos;
        }
        String pathStr = lhs.substring(0, pathEndPos);
        pathStr = pathStr.replaceAll("/+", "/");
        
        String qualifierStr = leftBracketPos == -1 ? null :
            lhs.substring(leftBracketPos + 1, rightBracketPos);
        List<Predicate> qualifiers = new ArrayList<Predicate>();
        if (qualifierStr != null) {
            String[] splitQualifiers = qualifierStr.split(",");
            for (String qualifier: splitQualifiers) {
                qualifier = qualifier.trim();
                int colonIdx = qualifier.indexOf(":");
                if (colonIdx == 0 || colonIdx == -1 
                        || colonIdx == qualifier.length() - 1) {
                    return;
                }
                String name = qualifier.substring(0, colonIdx);
                String value = qualifier.substring(colonIdx + 1);
                qualifiers.add(new Predicate(name, value));
            }
        }

        Path path = Path.fromString(pathStr);
        Node n = root;
        for (String name: path.getElements()) {
            if (name.equals("/")) {
                continue;
            }
            Node child = n.children.get(name);
            if (child == null) {
                child = new Node();
                n.children.put(name, child);
            }
            n = child;
        }
        if (n.entries == null) {
            n.entries = new ArrayList<ConfigEntry>();
        }
        n.entries.add(new ConfigEntry(qualifiers, rhs, exact));
    }

        
    private class Node {
        private Map<String, Node> children = new HashMap<String, Node>();
        private List<ConfigEntry> entries = null;
        
        public String toString() {
           StringBuilder sb = new StringBuilder();
           sb.append("(");
           for (String s: this.children.keySet()) {
               sb.append(s).append("=").append(this.children.get(s));
               sb.append(",");
           }
           sb.append(")");
           return sb.toString();
        }
    }
    
    
    private String stripComments(String line) {
        if (line.startsWith("#")) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '#') {
                break;
            }
            result.append(c);
        }
        return result.toString();
    }

    public class ConfigEntry {
        private List<Predicate> predicates;
        private String value;
        private boolean exact;

        public ConfigEntry(List<Predicate> predicates, String value, boolean exact) {
            this.predicates = predicates;
            this.value = value;
            this.exact = exact;
        }
        
        public List<Predicate> getPredicates() {
            return Collections.unmodifiableList(this.predicates);
        }
        
        public String getValue() {
            return this.value;
        }
        
        public boolean isExact() {
            return this.exact;
        }
        
        public String toString() {
            StringBuilder sb = new StringBuilder(this.getClass().getName());
            sb.append(": predicates=").append(this.predicates);
            sb.append(";value=").append(this.value);
            sb.append(";exact=").append(this.exact);
            return sb.toString();
        }
    }
    
    public class Predicate {
        private String name;
        private String value;

        public Predicate(String name, String value) {
            this.name = name;
            this.value = value;
        }
        
        public String getName() {
            return this.name;
        }
        
        public String getValue() {
            return this.value;
        }
        
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.name).append("=").append(this.value);
            return sb.toString();
        }
    }
}
