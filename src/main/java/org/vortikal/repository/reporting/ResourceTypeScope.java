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

import org.vortikal.repository.resourcetype.ResourceTypeDefinition;

/**
 *
 * @author oyviste
 */
public class ResourceTypeScope extends AbstractReportScope {

    private Set<ResourceTypeDefinition> types;

    public ResourceTypeScope(Set<ResourceTypeDefinition> types) {
        if (types == null) {
            throw new IllegalArgumentException("Types cannot be null");
        }
        this.types = types;
    }

    @Override
    public int hashCode() {
        int code = isProhibited() ? 1231 : 1237;

        return 31 * code + this.types.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this)
            return true;

        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        ResourceTypeScope otherScope = (ResourceTypeScope)other;

        if (isProhibited() != otherScope.isProhibited()) return false;

        if (!this.types.equals(otherScope.types)) return false;

        return true;
    }

    @Override
    public Object clone() {
        Set<ResourceTypeDefinition> cloneDefs =
                new LinkedHashSet<ResourceTypeDefinition>(this.types);

        ResourceTypeScope clone = new ResourceTypeScope(cloneDefs);
        clone.setProhibited(isProhibited());

        return clone;
    }
    
    /**
     * @return the types
     */
    public Set<ResourceTypeDefinition> getTypes() {
        return this.types;
    }


}
