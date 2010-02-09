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
package org.vortikal.repository.search.jcr;

import java.util.Iterator;

import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.search.PropertySortField;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.SortField;
import org.vortikal.repository.search.SortFieldDirection;
import org.vortikal.repository.search.Sorting;
import org.vortikal.repository.search.TypedSortField;
import org.vortikal.repository.store.jcr.JcrDaoConstants;


/**
 * 
 *
 */
public class SqlOrderingImpl implements SqlOrdering {

    private Search search;
    
    public SqlOrderingImpl(Search search) {
        this.search = search;
    }
    
    public String getOrdering() throws UnsupportedQueryException {
        
        Sorting sorting = search.getSorting();
        
        StringBuilder buffer = new StringBuilder();
        
        Iterator<SortField> iterator = sorting.getSortFields().iterator();
        if (iterator.hasNext()) {
            buffer.append(" ORDER BY ");
        }
        while (iterator.hasNext()) {
            SortField sf = iterator.next();
            
            if (sf instanceof TypedSortField){
                TypedSortField tsf = (TypedSortField)sf;
                
                if ("uri".equals(tsf.getType())){
                    // XXX: doesn't seem like JCR sorts paths as should be expected .. don't know why.
                    buffer.append("jcr:path ");
                } else if ("name".equals(tsf.getType())){
                    // XXX: jcr:name pseudo property does not exist, so cannot be used in sorting, because JCR queries suck.
                } else if ("type".equals(tsf.getType())) {
                    buffer.append(JcrDaoConstants.RESOURCE_TYPE).append(" ");
                }
                
            } else if (sf instanceof PropertySortField) {
                PropertyTypeDefinition def = ((PropertySortField)sf).getDefinition();
                
                String propName = def.getName();
                String prefix = def.getNamespace().getPrefix();
                if (prefix != null) {
                    propName = prefix + JcrDaoConstants.VRTX_PREFIX_SEPARATOR + propName;
                }
                propName = JcrDaoConstants.VRTX_PREFIX + propName;
                
                buffer.append(propName).append(" ");
            }
            
            if (sf.getDirection() == SortFieldDirection.ASC) {
                buffer.append("ASC"); 
            } else {
                buffer.append("DESC");
            }

            if (iterator.hasNext()){
                buffer.append(", ");
            }
        }
        
        return buffer.toString();
    }
    
}
