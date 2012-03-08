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

package org.vortikal.repository.reporting;

import java.util.LinkedHashSet;
import java.util.Set;

import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;

/**
 * Only include resources with a certain set of property values in data reporting.
 * 
 * @author oyviste
 */
public class ResourcePropertyValueScope extends AbstractReportScope {

    private PropertyTypeDefinition def;
    private Set<Value> values;
    
    public ResourcePropertyValueScope(PropertyTypeDefinition def, Set<Value> values) {
        if (def == null) {
            throw new IllegalArgumentException("Def cannot be null");
        }

        if (values == null) {
            throw new IllegalArgumentException("Values cannot be null");
        }

        this.def = def;
        this.values = values;
    }

    public int hashCode() {
        int code = isProhibited() ? 1231 : 1237;

        code = 31 * code + this.getDef().hashCode();
        code = 31 * code + this.getValues().hashCode();

        return code;
    }

    public boolean equals(Object other) {
        if (other == this)
            return true;

        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        ResourcePropertyValueScope otherScope =
                (ResourcePropertyValueScope) other;

        if (isProhibited() != otherScope.isProhibited()) return false;

        if (!this.def.equals(otherScope.def)) return false;

        if (!this.values.equals(otherScope.values)) return false;

        return true;
    }

    public Object clone() {
        Set<Value> cloneValues = new LinkedHashSet<Value>(this.values.size());
        cloneValues.addAll(this.values);
        
        ResourcePropertyValueScope clone = new ResourcePropertyValueScope(this.def, cloneValues);
        clone.setProhibited(isProhibited());

        return clone;
    }

    /**
     * @return the def
     */
    public PropertyTypeDefinition getDef() {
        return def;
    }

    /**
     * @return the values
     */
    public Set<Value> getValues() {
        return values;
    }

}
