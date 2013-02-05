/* Copyright (c) 2008-2013, University of Oslo, Norway
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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.vortikal.repository.Path;

/**
 * Parser for path mapping style configuration files.
 *
 * <p>
 * Parses a line-based configuration file format using a "key = value\n" base syntax,
 * where keys are paths understood specially and hierarchically.
 * <pre>
 * 
 *    / = VALUE1                   # A comment ..
 *    /a[p1:x,p2:y] = VALUE2
 *    /a/b = VALUE3
 *    /a/b/c/ = VALUE4             # exact match hint for path
 *
 *    # another comment
 *    /a/b/c[p1:x]/ = VALUE5       # exact match AND qualifier "p1:x"
 *    /a/b/c/[p1:x] = VALUE5       # alt. syntax for previous exact rule.
 * 
 * </pre> where paths are mapped to a list of qualifiers and a single VALUE.
 * Empty lines, lines not beginning with "/", lines with empty value and
 * "#"-comments are ignored.
 * The input file must use UTF-8 encoding for non-ASCII chars.
 * 
 * <p> 
 * Ending a path with "/" (or specifying "/" after qualifier-list) hints that the
 * particular config entry shall apply only to the specific path and not
 * descendant paths. This makes a difference for
 * {@link #getMatchAncestor(org.vortikal.repository.Path) getMatchAncestor},
 * which ignores exact rules on ancestor paths. The flag is also available
 * for each config entry as {@link ConfigEntry#isExact() }.
 * <p>
 * After parsing, the configuration will be organized into a hierarchy
 * corresponding to the paths present, and it can be queried with
 * {@link #get(org.vortikal.repository.Path) get}
 * and {@link #getMatchAncestor(org.vortikal.repository.Path) getMatchAncestor}.
 * <p>
 * The same path may occur in configuration multiple times, but with different
 * set of qualifiers and values. A single path can map to zero or more
 * {@link ConfigEntry} instances, each with list of qualifier name-value-pairs,
 * a single VALUE and a flag indicating that exact path matching is desired.
 * <p>
 * It is up to client code how to interpret the configuration entries that
 * apply for a given path - this class is merely a parser that understands path
 * hierarchies and the syntax described.
 */
public class PathMappingConfig {

    private final Node root = new Node();
    private int maxLineSize = 300;
    private int maxLines = 10000;

    /**
     * Construct a configuration instance from an input stream.
     * 
     * @param source the input stream to read configuration lines from.
     * @throws Exception in case of parsing errors.
     */
    public PathMappingConfig(InputStream source) throws Exception {
        load(source);
    }

    /**
     * Construct a configuration instance from an input stream.
     * 
     * @param source the input stream to read configuration lines from. The stream
     *               will be closed after parsing.
     * @param maxLineSize max size of lines in configuration source.
     * @param maxLines max number of lines read from configuration source.
     * @throws Exception in case of parsing errors.
     */
    public PathMappingConfig(InputStream source, int maxLineSize, int maxLines) throws Exception {
        if (maxLineSize <= 0) throw new IllegalArgumentException("maxLineSize must be > 0");
        if (maxLines <= 0) throw new IllegalArgumentException("maxLines must be > 0");
        
        this.maxLineSize = maxLineSize;
        this.maxLines = maxLines;
        
        load(source);
    }

    /**
     * Returns all config entries that apply exactly for the given path.
     */
    public List<ConfigEntry> get(Path path) {
        Node n = root;
        for (String name : path.getElements()) {
            if (name.equals("/")) {
                continue;
            }
            n = n.children.get(name);
            if (n == null) {
                break;
            }
        }
        if (n != null) {
            return n.entries;
        }
        return null;
    }
    
    /**
     * Returns config entries that apply exactly for the given path or
     * the config entries for the closes ancestor path are not exact match rules.
     * 
     * @return a list of config entries, or <code>null</code> if no entries
     * apply.
     */
    public List<ConfigEntry> getMatchAncestor(Path path) {
        Node n = root;
        List<ConfigEntry> entries = getApplicableEntries(n, path);
        List<String> nodeNames = path.getElements();
        for (int i=1; i<nodeNames.size();i++) {
            n = n.children.get(nodeNames.get(i));
            if (n == null) {
                break;
            }

            List<ConfigEntry> applicable = getApplicableEntries(n, path);
            if (!applicable.isEmpty()) {
                entries = applicable;
            }
        }
        if (entries.isEmpty()) {
            return null;
        }
        
        return entries;
    }
    
    /**
     * Node n must correspond to path p or an ancestor path of p
     * @return list of corresponding config entries, empty list of none.
     */
    private List<ConfigEntry> getApplicableEntries(Node n, Path p) {
        List<ConfigEntry> entries = new ArrayList<ConfigEntry>();
        if (n.entries == null) return entries;
        for (ConfigEntry e: n.entries) {
            if (e.exact) {
                if (p.equals(e.path)) {
                    entries.add(e);
                }
            } else {
                // Node represents ancestor or equal path
                entries.add(e);
            }
        }
        return entries;
    }
    
    private void load(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        try {
            String line;
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
                    parseLine(line, this.root);
                }
            }
        } finally {
            reader.close();
        }
    }

    private void parseLine(String line, Node root) {
        String[] kv = TextUtils.parseKeyValue(line, '=', TextUtils.TRIM 
                                                         | TextUtils.IGNORE_UNESCAPED_SEP_IN_VALUE 
                                                         | TextUtils.IGNORE_ILLEGAL_ESCAPE);
        if (kv[1] == null) return;
        String lhs = kv[0];
        String rhs = kv[1];
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
            rightBracketPos = lhs.lastIndexOf(']');
            if (rightBracketPos == -1 || rightBracketPos < leftBracketPos) {
                return;
            }
            pathEndPos = leftBracketPos;
        }
        String pathStr = lhs.substring(0, pathEndPos);
        // Allow to specify exactly root as '//':
        if (!"//".equals(pathStr)) {
            pathStr = pathStr.replaceAll("/+", "/");
        }
        if (!"/".equals(pathStr) && pathStr.endsWith("/")) {
            exact = true;
            pathStr = pathStr.substring(0, pathStr.length()-1);
        }

        String qualifierStr = leftBracketPos == -1 ? null
                : lhs.substring(leftBracketPos + 1, rightBracketPos).trim();
        List<Qualifier> qualifiers = new ArrayList<Qualifier>();
        if (qualifierStr != null && !qualifierStr.isEmpty()) {
            String[] splitQualifiers = TextUtils.parseCsv(qualifierStr, ',', TextUtils.TRIM 
                                                                             | TextUtils.DISCARD
                                                                             | TextUtils.IGNORE_ILLEGAL_ESCAPE);
            for (String qualifier : splitQualifiers) {
                String[]  kv = TextUtils.parseKeyValue(qualifier, ':');
                if (kv[0].isEmpty() || kv[1] == null || kv[1].isEmpty()) {
                    return;
                }
                qualifiers.add(new Qualifier(kv[0], kv[1]));
            }
        }

        Path path = Path.fromString(pathStr);
        Node n = root;
        for (String name : path.getElements()) {
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
        n.entries.add(new ConfigEntry(qualifiers, rhs, exact, path));
    }

    private static class Node {

        private Map<String, Node> children = new HashMap<String, Node>();
        private List<ConfigEntry> entries = null;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("(");
            for (String s : this.children.keySet()) {
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

    /**
     * A config entry represents a single line in the configuration file. Note
     * multiple lines may have the same path mapping, and thus there can be
     * multiple instances of this class for a single path as returned by
     * {@link #get(org.vortikal.repository.Path) }.
     */
    public static final class ConfigEntry {

        private List<Qualifier> qualifiers;
        private String value;
        private boolean exact;
        private Path path;

        public ConfigEntry(List<Qualifier> qualifiers, String value, boolean exact, Path path) {
            this.qualifiers = qualifiers;
            this.value = value;
            this.exact = exact;
            this.path = path;
        }

        /**
         * Returns list of qualifiers for configuration entry.
         * @return 
         */
        public List<Qualifier> getQualifiers() {
            return Collections.unmodifiableList(this.qualifiers);
        }

        /**
         * Returns the configuration value as a string.
         */
        public String getValue() {
            return this.value;
        }

        /**
         * @return <code>true</code> if this config entry applies to its path
         * exactly, <code>false</code> if the entry applies to its path and
         * path descendants.
         */
        public boolean isExact() {
            return this.exact;
        }
        
        /**
         * Returns path in the configuration that this config entry is associated
         * with.
         */
        public Path getPath() {
            return this.path;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(this.getClass().getName());
            sb.append(": predicates=").append(this.qualifiers);
            sb.append(";value=").append(this.value);
            sb.append(";exact=").append(this.exact);
            sb.append(";path=").append(this.path);
            return sb.toString();
        }
    }

    /**
     * A qualifier is simply a generic name-value pair of strings.
     */
    public static final class Qualifier {

        private String name;
        private String value;

        public Qualifier(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return this.name;
        }

        public String getValue() {
            return this.value;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.name).append("=").append(this.value);
            return sb.toString();
        }
    }
    
}
