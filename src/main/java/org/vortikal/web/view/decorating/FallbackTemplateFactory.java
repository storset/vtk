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
package org.vortikal.web.view.decorating;

import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;


public class FallbackTemplateFactory implements TemplateFactory {
    private static Log logger = LogFactory.getLog(FallbackTemplateFactory.class);
    
    private List<TemplateFactory> templateFactories;
    
    public Template newTemplate(TemplateSource templateSource)
            throws InvalidTemplateException {
        for (int i = 0; i < this.templateFactories.size(); i++) {
            TemplateFactory tf = this.templateFactories.get(i);

            logger.info("Trying template factory " + tf);

            if (i == this.templateFactories.size() - 1) {
                return tf.newTemplate(templateSource);
            } else {
                try {
                    return tf.newTemplate(templateSource);
                } catch (InvalidTemplateException e) {
                    logger.info("Unable to create template using template factory " + tf, e);
                    continue;
                }
            }
        }
        throw new InvalidTemplateException("Unable to create template " 
                + templateSource);
    }

    @Required public void setTemplateFactories(List<TemplateFactory> templateFactories) {
        this.templateFactories = Collections.unmodifiableList(templateFactories);
    }
}
