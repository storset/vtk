/* Copyright (c) 2011, University of Oslo, Norway
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
package org.vortikal.repository.index.mapping;

import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.vortikal.repository.PropertySetImpl;
import org.vortikal.repository.search.PropertySelect;
import org.vortikal.security.Principal;

public interface DocumentMapper {

    /**
     * Map loaded {@link Document} to a {@link PropertySetImpl} instance.
     * 
     * @param doc
     * @return
     * @throws DocumentMappingException
     */
    public PropertySetImpl getPropertySet(Document doc)
        throws DocumentMappingException;
    
    /**
     * Get ACL read principal <em>names</em> from a document.
     * @param doc
     * @return
     * @throws DocumentMappingException
     */
    public Set<String> getACLReadPrincipalNames(Document doc)
        throws DocumentMappingException;
    
    /**
     * Map a {@link PropertySetImpl} instance to a {@link Document}.
     * 
     * @param propertySet
     * @return
     * @throws DocumentMappingException
     */
    public Document getDocument(PropertySetImpl propertySet, Set<Principal> aclReadPrincipals)
        throws DocumentMappingException;
    
    /**
     * Get a <code>FieldSelector</code> instance corresponding to
     * property selection in <code>PropertySelect</code> instance.
     * 
     * @param select
     * @return
     */
    public FieldSelector getDocumentFieldSelector(PropertySelect select);
    
}
