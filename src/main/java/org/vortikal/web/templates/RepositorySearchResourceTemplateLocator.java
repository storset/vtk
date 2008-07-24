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
package org.vortikal.web.templates;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import org.vortikal.repository.search.Searcher;

/**
 * Template locator which uses repository search mechanism to locate templates. 
 *
 */
public class RepositorySearchResourceTemplateLocator implements ResourceTemplateLocator {

    // Repository searcher used to locate templates.
    private Searcher searcher;
    
    /**
     * @see ResourceTemplateLocator#findTemplates(String, String, Set, ResourceTypeDefinition)
     */
    public List<ResourceTemplate> findTemplates(String token, 
                                                Set<String> baseUris,
                                                ResourceTypeDefinition resourceType) {

        // Make a Search instance based on the givne criteria
        // ..
        
        // Execute query
        //ResultSet results = searcher.execute(token, search..);
        
        // Iterate query results in populate list of Template instances.
        
        return null;
    }
    
    

    /**
     * @see org.vortikal.web.templates.ResourceTemplateLocator#findTemplates(java.lang.String, java.util.Set, int, org.vortikal.repository.resourcetype.ResourceTypeDefinition)
     */
    public List<ResourceTemplate> findTemplates(String token,
                                                Set<String> baseUris, 
                                                int relativeDepth, 
                                                ResourceTypeDefinition type) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Required
    public void setSearcher(Searcher searcher) {
        this.searcher = searcher;
    }

}
