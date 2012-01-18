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
package org.vortikal.repository.resourcetype.property;

import java.awt.Dimension;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertyEvaluationContext;
import org.vortikal.repository.PropertyEvaluationContext.Type;
import org.vortikal.repository.resourcetype.Content;
import org.vortikal.repository.resourcetype.PropertyEvaluator;

public class ImageDimensionEvaluator implements PropertyEvaluator {

    private static Log logger = LogFactory.getLog(ImageDimensionEvaluator.class);
    private boolean evaluateHeight = true; // Otherwise width

    public void setEvaluateHeight(boolean evaluateHeight) {
        this.evaluateHeight = evaluateHeight;
    }
    
    @Override
    public boolean evaluate(Property property, PropertyEvaluationContext ctx) throws PropertyEvaluationException {
        if (property.isValueInitialized()
                && ctx.getEvaluationType() != Type.ContentChange
                && ctx.getEvaluationType() != Type.Create) {
            return true;
        }
        try {
            Content content = ctx.getContent();
            if (content == null) {
                logger.info("Unable to get Dimension representation of " 
                        + ctx.getNewResource().getURI());
                return false;
            }
            Dimension dim = (Dimension) content.getContentRepresentation(Dimension.class);
            if (dim == null) {
                logger.info("Unable to get Dimension representation of "
                        + ctx.getNewResource().getURI());
                return false;
            }
            property.setIntValue(this.evaluateHeight ? dim.height : dim.width);
            return true;

        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Unable to get Dimension representation of "
                        + ctx.getNewResource().getURI(), e);
            } else {
                logger.info("Unable to get Dimension representation of "
                        + ctx.getNewResource().getURI());
            }
            return false;
        }
    }

}

