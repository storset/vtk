/* Copyright (c) 2007, University of Oslo, Norway
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
package org.vortikal.util.repository;

import java.util.Comparator;
import org.vortikal.repository.Property;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;

public class ResourcePropertyComparator implements Comparator<Resource> {

    private PropertyTypeDefinition propDef;

    public ResourcePropertyComparator(PropertyTypeDefinition propDef) {
        if (propDef == null) {
            throw new IllegalArgumentException("Invalid property type definition: " + propDef);
        }
        this.propDef = propDef;
    }

    public int compare(Resource r1, Resource r2) {
        Property p1 = r1.getProperty(this.propDef);
        if (p1 == null) {
            throw new IllegalArgumentException(
                "Unable to compare resources " + r1 + " and " + r2 + ": " +
                "resource " + r1 + " has no such property: " + this.propDef);
        }
        Property p2 = r2.getProperty(this.propDef);
        if (p2 == null) {
            throw new IllegalArgumentException(
                "Unable to compare resources " + r1 + " and " + r2 + ": " +
                "resource " + r2 + " has no such property: " + this.propDef);
        }
        
        return p1.getValue().compareTo(p2.getValue());
    }
    
    
}

