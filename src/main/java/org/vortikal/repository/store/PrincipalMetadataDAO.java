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
package org.vortikal.repository.store;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.vortikal.security.Principal;

/**
 * This interface contains methods for acquiring metadata about a system's
 * principals. Methods should return <code>null</code> if nothing is found for a
 * search or lookup.
 * 
 */
public interface PrincipalMetadataDAO {

    /**
     * Get metadata instance for principal with the given fully qualified name
     * (username/id+domain).
     * 
     * 
     * @param qualifiedName
     *            The fully qualified name of the principal to get the
     *            description for.
     * @return A <code>PrincipalMetadata</code> instance with description or
     *         <code>null</code> if no metadata could be found.
     * 
     */
    PrincipalMetadata getMetadata(String qualifiedName, Locale preferredLocale);

    /**
     * Get metadata instance for given principal instance.
     * 
     * @param principal
     *            The <code>Principal</code> instance to get the description
     *            for.
     * @return A <code>PrincipalMetadata</code> instance or <code>null</code> if
     *         none found.
     * 
     */
    PrincipalMetadata getMetadata(Principal principal, Locale preferredLocale);

    /**
     * Searches for principals which satisfy the supplied search criteria.
     * 
     * @return List of metadata-instances (<code>PrincipalMetadata</code>) for
     *         each principal that satisfies the search criteria or
     *         <code>null</code> if nothing suitable was found.
     */
    List<PrincipalMetadata> search(PrincipalSearch search, Locale preferredLocale);

    /**
     * List all employees for a given unit and all its sub-units.
     * 
     * @param areacodeOrDn
     * @return
     */
    List<PrincipalMetadata> listPrincipalsInUnit(String areacodeOrDn, Locale preferredLocale);

    List<PrincipalMetadata> listPrincipalsInUnitXX(String areacodeOrDn, Locale preferredLocale, boolean considerSubUnits);

    /**
     * Return set of supported principal domains for this DAO.
     */
    Set<String> getSupportedPrincipalDomains();

}
