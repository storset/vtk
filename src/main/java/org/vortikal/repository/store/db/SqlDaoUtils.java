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

import java.util.List;

import org.vortikal.repository.Path;

final class SqlDaoUtils {

    public static String getUriSqlWildcard(final Path uri, final char escape) {
        if (uri.isRoot()) {
            return "/%";
        }
        return SqlDaoUtils.getStringSqlWildcard(uri.toString(), escape);
    }

    /**
     * XXX Method is named like a generic string escape, but still does a path-like
     *     modification by appending '/%' at end of string.
     */
    public static String getStringSqlWildcard(final String s, final char escape) {
        StringBuilder result = new StringBuilder(s.length() + 2);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == escape || c == '%' || c == '_') {
                result.append(escape);
            }

            result.append(c);
        }
        result.append("/%");

        return result.toString();
    }

    /**
     * This internal class is used to aggregate property values from potentially
     * multiple rows for a single property into a single prop holder object.
     * 
     * Elements that together constitute prop holder identity are:
     * - The resource id
     * - The property namespace URI
     * - The property name
     * 
     * And nothing more. A single property instance at application level can map
     * to multiple propIDs in database because of de-normalized storage of
     * multi-value properties.
     */
    public static class PropHolder {
        String namespaceUri = "";
        String name = "";
        int propTypeId;
        int resourceId;
        Object propID = null;
        boolean binary = false;
        boolean inherited = false;
        List<Object> values;

        @Override
        public boolean equals(Object object) {
            if (object == this)
                return true;

            if (object == null || object.getClass() != this.getClass())
                return false;

            PropHolder other = (PropHolder) object;
            if (!this.name.equals(other.name))
                return false;

            if (this.resourceId != other.resourceId)
                return false;

            if (this.namespaceUri != null) {
                return this.namespaceUri.equals(other.namespaceUri);
            } else {
                return other.namespaceUri == null;
            }
        }

        @Override
        public int hashCode() {
            int code = this.resourceId + 7;
            code = 31 * code + this.name.hashCode();
            code = 31 * code + (this.namespaceUri == null ? 0 : this.namespaceUri.hashCode());
            return code;
        }
    }

}
