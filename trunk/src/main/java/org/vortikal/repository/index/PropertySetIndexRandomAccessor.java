/* Copyright (c) 2006, University of Oslo, Norway
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
package org.vortikal.repository.index;

import java.util.Set;

import org.vortikal.repository.Path;
import org.vortikal.repository.PropertySet;

/**
 * Interface for retrieving property sets from a <code>PropertySetIndex</code> 
 * by URI or UUID in a random access manner.
 * 
 * Must be closed after usage.
 * 
 * @author oyviste
 *
 */
public interface PropertySetIndexRandomAccessor {
   
    /**
     * Check if one or more property sets exists for the given URI.
     * 
     * @param uri
     * @return
     * @throws IndexException
     */
    public boolean exists(Path uri) throws IndexException;
    
    /**
     * Count number of existing property set instances for the given URI. This can be used
     * to detect inconsistencies.
     * 
     * @param uri
     * @return
     * @throws IndexException
     */
    public int countInstances(Path uri) throws IndexException;
    
    /**
     * Get a property set by URI
     * @param uri
     * @return
     * @throws IndexException
     */
    public PropertySet getPropertySetByURI(Path uri) throws IndexException;
    
    /**
     * Get a property set by UUID
     * @param uuid
     * @return
     * @throws IndexException
     */
    public PropertySet getPropertySetByUUID(String uuid) throws IndexException;
    
    /**
     * Get internal data stored in index for a property-set.
     * @return an instance of {@link PropertySetInternalData}.
     */
    public PropertySetInternalData getPropertySetInternalData(Path uri) throws IndexException;
    
    /**
     * This method should be called after usage to free index resources.
     * @throws IndexException
     */
    public void close() throws IndexException;

    /**
     * Represents "internal" data fields stored for each property set in index.
     * Typically not retrievable through the regular {@link PropertySet} interface.
     */
    public interface PropertySetInternalData {
        Path getURI();
        String getResourceType();
        int getResourceId();
        Set<String> getAclReadPrincipalNames();
        int getAclInheritedFromId();
    }
}
