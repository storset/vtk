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
package org.vortikal.repository.store.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.vortikal.repository.PropertySetImpl;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.store.IndexDao;
import org.vortikal.repository.store.PropertySetHandler;
import org.vortikal.security.PrincipalFactory;

/**
 * Property set row handler which caches resource IDs, and generates 
 * ancestor IDs based on this cache. 
 * 
 * This row handler requires all resources from the root uri to exist in the
 * result iteration and be ordered by URI.
 *  
 */
class ResourceIdCachingPropertySetRowHandler extends
        PropertySetRowHandler {

    private Map<String, Integer> resourceIdCache = new HashMap<String, Integer>();

    public ResourceIdCachingPropertySetRowHandler(PropertySetHandler clientHandler,
            ResourceTypeTree resourceTypeTree, PrincipalFactory principalFactory, IndexDao indexDao) {
        super(clientHandler, resourceTypeTree, principalFactory, indexDao);
    }
    
    protected void populateAncestorIds(Map<String, Object> row, PropertySetImpl propSet) {
        Integer id = (Integer)row.get("id");
        String uri = (String)row.get("uri");
        putId(uri, id);
        
        propSet.setAncestorIds(getAncestorIdsForUri(uri));
    }
                
    private void putId(String uri, Integer id) {
        this.resourceIdCache.put(uri, id);
    }
    
    private int[] getAncestorIdsForUri(String uri) throws IllegalStateException {
        List<String> ancestorUris = new ArrayList<String>();
        
        if ("/".equals(uri)) return new int[0];
        
        int from = uri.length();
        while (from > 0) {
            from = uri.lastIndexOf('/', from);
            if (from == 0) {
                ancestorUris.add("/");
            } else {
                ancestorUris.add(uri.substring(0, from--));
            }
        }
        
        int[] ids = new int[ancestorUris.size()];
        
        int c = 0;
        for (String ancestor: ancestorUris) {
            Integer id = this.resourceIdCache.get(ancestor);
            
            if (id == null) {
                throw new IllegalStateException("Needed ID for URI '" + uri 
                        + "', but this URI has not yet been encountered in the iteration.");
            }
            
            ids[c++] = id.intValue();
        }
        
        return ids;
    }
    
}
