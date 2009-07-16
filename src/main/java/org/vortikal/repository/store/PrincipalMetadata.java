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
package org.vortikal.repository.store;


import java.util.List;
import java.util.Set;

/**
 * Simplistic and generic interface for accessing principal metadata
 * as "attribute -> list of values"-mappings.
 * 
 * Not a permanent solution, as almost nothing is well defined (including
 * what type generic attribute values have). Intepretation of values are thus
 * up to the client code and actual types depend on data source implementing
 * the {@link PrincipalMetadataDAO} interface.
 * 
 * XXX: extend Principal interface, or provide this data through Principal interface itself ?
 * Right now, Principal metadata is something very separate to Principals.
 * 
 * This will typically be used by {@link org.vortikal.security.PrincipalFactory}
 * when creating {@link org.vortikal.security.Principal} instances.
 * 
 */
public interface PrincipalMetadata {
    
    /**
     * Get the qualified name of the principal to which this instance's
     * metadata applies.
     * 
     * @see org.vortikal.security.Principal#getQualifiedName()
     * 
     * @return The qualified name as a <code>String</code>.
     */
    public String getQualifiedName();

    /**
     * Get description for a principal.
     * 
     * @see org.vortikal.security.Principal#getDescription()
     * 
     * @return The description of the principal. May be <code>null</code>.
     */
    public String getDescription();
    
    /**
     * Get URL for a principal.
     * 
     * @see org.vortikal.security.Principal#getURL()
     * 
     * @return The URL of the principal. May be <code>null</code>.
     */
    public String getUrl();
    
    /**
     * Get value of attribute with the given name. If
     * the attribute has multiple values, then the first
     * value is returned. 
     * 
     * @param attributeName The name of the attribute.
     * @return An object representing the value or <code>null</code> if
     *         there is no value(s) associated with the attribute.
     */
    public Object getValue(String attributeName);
    
    /**
     * Get all values of attribute with the given name.
     * 
     * @param attributeName The name of the attribute.
     * @return An list of objects representing the values or <code>null</code> if
     *         there are no values associated with the attribute.
     */
    public List<Object> getValues(String attributeName);
    

    /**
     * Returns set of all attribute names for this instance.
     * 
     * @return
     */
    public Set<String> getAttributeNames();

}