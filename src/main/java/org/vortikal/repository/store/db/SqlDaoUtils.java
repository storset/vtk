/* Copyright (c) 2006, 2007, University of Oslo, Norway
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
package org.vortikal.repository.store.db;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.vortikal.repository.Path;


class SqlDaoUtils {

    private static final Set<Character> WILDCARDS = new HashSet<Character>();
    
    static {
        WILDCARDS.add('_');
        WILDCARDS.add('%');
    }

    public static String getUriSqlWildcard(Path uri, char escape) {
        String path = uri.toString();
        if ("/".equals(path)) {
            return "/%";
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == escape) {
                result.append(escape);
            } else if (WILDCARDS.contains(c)) {
                result.append(escape);
            }
            result.append(c);
        }
        result.append("/%");
        return result.toString();
    }
    


    public static class PropHolder {
        String namespaceUri = "";
        String name = "";
        int type;
        int resourceId;
        Object propID = null;
        boolean binary = false;
        List<String> values;
        
        public boolean equals(Object object) {
            if (object == null) return false;
            
            if (object == this) return true;
            
            PropHolder other = (PropHolder) object;
            if (this.namespaceUri == null && other.namespaceUri != null ||
               this.namespaceUri != null && other.namespaceUri == null)
                return false;

            return ((this.namespaceUri == null && other.namespaceUri == null)
                    || (this.namespaceUri.equals(other.namespaceUri) &&
                        this.name.equals(other.name)                 &&
                        this.resourceId == other.resourceId));
        }
        
        public int hashCode() {
            int hashCode = this.name.hashCode() + this.resourceId;
            if (this.namespaceUri != null) {
                hashCode += this.namespaceUri.hashCode();
            }
            if (this.propID != null) {
                hashCode += this.propID.hashCode();
            }
            return hashCode;
        }
    }

}
