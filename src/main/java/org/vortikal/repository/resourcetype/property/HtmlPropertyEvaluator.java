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

import org.vortikal.repository.Property;
import org.vortikal.repository.PropertyEvaluationContext;
import org.vortikal.repository.PropertyEvaluationContext.Type;
import org.vortikal.repository.content.HtmlInfo;
import org.vortikal.repository.resourcetype.PropertyEvaluator;

public class HtmlPropertyEvaluator implements PropertyEvaluator {

    private String field;
    
    public void setField(String field) {
        this.field = field;
    }

    @Override
    public boolean evaluate(Property property, PropertyEvaluationContext ctx)
            throws PropertyEvaluationException {
        if (ctx.getContent() == null) {
            return false;
        }
        if (ctx.getEvaluationType() != Type.ContentChange && ctx.getEvaluationType() != Type.Create) {
            boolean exists = ctx.getOriginalResource().getProperty(property.getDefinition()) != null;
            return exists; 
        }

        try {
            HtmlInfo htmlInfo = (HtmlInfo) 
                ctx.getContent().getContentRepresentation(HtmlInfo.class);
            if ("doctype".equals(this.field)) {
                String doctype = htmlInfo.getDocType();
                if (doctype == null) {
                    return false;
                }
                property.setStringValue(doctype);
                return true;
            }
            if ("encoding".equals(this.field)) {
                String encoding = htmlInfo.getEncoding();
                if (encoding == null) {
                    return false;
                }
                property.setStringValue(encoding);
                return true;
            }
            if ("title".equals(this.field)) {
                String title = htmlInfo.getTitle();
                if (title == null) {
                    return false;
                }
                property.setStringValue(title);
                return true;
            }
            return false;
            
        } catch (Throwable t) {        
            return false;
        }
    }
}
