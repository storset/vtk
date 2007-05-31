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
package org.vortikal.repository.resourcetype.property;

import java.io.InputStream;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.resourcetype.Content;
import org.vortikal.repository.resourcetype.ContentModificationPropertyEvaluator;
import org.vortikal.security.Principal;

import au.id.jericho.lib.html.CharacterReference;
import au.id.jericho.lib.html.Element;
import au.id.jericho.lib.html.Source;
import au.id.jericho.lib.html.Tag;


public class HtmlTitleElementEvaluator implements ContentModificationPropertyEvaluator {

    private static Log logger = LogFactory.getLog(HtmlTitleElementEvaluator.class);

    
    public boolean contentModification(Principal principal, Property property,
            PropertySet ancestorPropertySet, Content content, Date time)
            throws PropertyEvaluationException {
        
        try {
            InputStream stream = (InputStream) content.getContentRepresentation(InputStream.class);
            Source source = new Source(stream);

            Element titleElement = source.findNextElement(0, Tag.TITLE);
            if (titleElement == null) {
                return false;
            }
            String title = CharacterReference.decodeCollapseWhiteSpace(titleElement.getContent());
            property.setStringValue(title);

            return true;
        } catch (Exception e) {
            logger.warn("Unable to get InputStream representation of resource '"
                        + ancestorPropertySet.getURI() + "'", e);
            return false;
        }
    }
    
//     public boolean contentModification(Principal principal, Property property,
//             PropertySet ancestorPropertySet, Content content, Date time)
//             throws PropertyEvaluationException {
        
//         try {
//             InputStream stream = (InputStream) content.getContentRepresentation(InputStream.class);
//             Source source = new Source(stream);
//             source.fullSequentialParse();
            
//             Element titleElement = source.findNextElement(0, HTMLElementName.TITLE);
//             if (titleElement == null) return false;
//             String title = CharacterReference.decodeCollapseWhiteSpace(titleElement.getContent());

//             property.setStringValue(title);

//             return true;
//         } catch (Exception e) {
//             logger.warn("Unable to get InputStream representation of resource '"
//                         + ancestorPropertySet.getURI() + "'", e);
//             return false;
//         }
//     }
    
    
}
