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
package org.vortikal.repository.reporting;

import java.util.LinkedList;
import java.util.List;

import org.vortikal.repository.Path;

/**
 * A reporting scope defined by a set of resource URI prefixes.
 *
 */
public class UriPrefixScope extends AbstractReportScope {

    private List<Path> uris = new LinkedList<Path>();

    public UriPrefixScope() {}

    public UriPrefixScope(Path uri) {
        this.uris.add(uri);
    }

    public List<Path> getUriPrefixes() {
        return this.uris;
    }

    public void addUriPrefix(Path uri) {
        for (Path p: this.uris) {
            if (p.isAncestorOf(uri)) {
                // Don't bother adding it, since it would have no effect
                // (we alrady have an ancestor in the prefix list).
                return;
            }
        }

        this.uris.add(uri);
    }

    @Override
    public int hashCode() {
        return this.uris.hashCode() + (isProhibited() ? 1231 : 1237);
    }

    @Override
    public boolean equals(Object other) {
    	if (other == this)
            return true;

        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        UriPrefixScope otherScope = (UriPrefixScope)other;

        if (isProhibited() != otherScope.isProhibited()) return false;

        if (!this.uris.equals(otherScope.uris)) return false;

        return true;
    }

    
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder(getClass().getSimpleName() + "[");
        for (Path uri : this.uris) {
            if (Path.ROOT.equals(uri)) {
                buffer.append(uri).append("* ");
            } else {
                buffer.append(uri).append("/* ");
            }
        }
        buffer.append("]");

        return buffer.toString();
    }

    @Override
    public Object clone() {

        UriPrefixScope clone = new UriPrefixScope();
        clone.uris = new LinkedList<Path>(this.uris);
        clone.setProhibited(isProhibited());

        return clone;
    }

    
}
