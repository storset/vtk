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
package org.vortikal.repository.resourcetype.property;

import java.util.ArrayList;
import java.util.List;

import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertyEvaluationContext;
import org.vortikal.repository.PropertyEvaluationContext.Type;
import org.vortikal.repository.resourcetype.PropertyEvaluator;

public class IndexFileEvaluator implements PropertyEvaluator {
    
    private List<String> indexFiles = new ArrayList<String>();

    @Override
    public boolean evaluate(Property property, PropertyEvaluationContext ctx)
            throws PropertyEvaluationException {
        
        if (! ctx.getNewResource().isCollection()) {
            return false;
        }

        // Property can only ever be created or removed when content of collection is changed.
        if (ctx.getEvaluationType() == Type.ContentChange) {
            List<Path> childUris = ctx.getNewResource().getChildURIs();
            if (childUris == null) {
                return false;
            }
            
            for (String indexFile : this.indexFiles) {
                for (Path p: childUris) {
                    String name = p.getName();
                    if (indexFile.equals(name)) {
                        property.setStringValue(name);
                        return true;
                    }
                }
            }
            // No index file match
            return false;
        }

        // Keep any existing value for other eval types than content change
        return property.isValueInitialized();
    }

    public void setIndexFiles(List<String> indexFiles) {
        if (indexFiles == null)
            return;
        List<String> result = new ArrayList<String>();
        for (String s: indexFiles) 
            result.add(s);
        this.indexFiles = result;
    }

}
