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
package org.vortikal.repository.resourcetype.property;

import java.awt.image.BufferedImage;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.graphics.ImageService;
import org.vortikal.graphics.ScaledImage;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertyEvaluationContext;
import org.vortikal.repository.PropertyEvaluationContext.Type;
import org.vortikal.repository.resourcetype.PropertyEvaluator;
import org.vortikal.repository.resourcetype.PropertyType;

public class ThumbnailEvaluator implements PropertyEvaluator {

    private static final Logger log = Logger.getLogger(ThumbnailEvaluator.class);

    private ImageService imageService;
    private String width;
    private Set<String> supportedFormats;
    private boolean scaleUp;

    public boolean evaluate(Property property, PropertyEvaluationContext ctx)
            throws PropertyEvaluationException {
        if (property.isValueInitialized()
                && ctx.getEvaluationType() != Type.ContentChange 
                && ctx.getEvaluationType() != Type.Create) {
            return true;
        }

        try {
            BufferedImage image = (BufferedImage) ctx.getContent()
                    .getContentRepresentation(BufferedImage.class);
            if (image == null) {
                return false;
            }

            Property contentType = ctx.getNewResource().getProperty(
                    Namespace.DEFAULT_NAMESPACE, PropertyType.CONTENTTYPE_PROP_NAME);
            String mimetype = contentType.getStringValue();
            String imageFormat = mimetype.substring(mimetype.lastIndexOf("/") + 1);

            if (!supportedFormats.contains(imageFormat.toLowerCase())) {
                log.warn("Unable to get create thumbnail, unsupported format: "
                        + imageFormat);
                return false;
            }

            if (!scaleUp && image.getWidth() <= Integer.parseInt(width)) {
                if (log.isDebugEnabled()) {
                    log.debug("Will not create a thumbnail: configured NOT to scale up");
                }
                return false;
            }

            ScaledImage thumbnail = imageService
                    .scaleImage(image, imageFormat, width, "");

            // TODO lossy-compression -> jpeg
            String thumbnailFormat = !imageFormat.equalsIgnoreCase("png") ? "png"
                    : imageFormat;

            property.setBinaryValue(thumbnail.getImageBytes(thumbnailFormat), "image/"
                    + thumbnailFormat);
            return true;

        } catch (Exception e) {
            log.warn("Unable to create thumbnail of content", e);
            return false;
        }
    }

    // @Required
    public void setImageService(ImageService imageService) {
        this.imageService = imageService;
    }

    @Required
    public void setWidth(String width) {
        this.width = width;
    }

    @Required
    public void setSupportedFormats(Set<String> supportedFormats) {
        this.supportedFormats = supportedFormats;
    }

    @Required
    public void setScaleUp(boolean scaleUp) {
        this.scaleUp = scaleUp;
    }
}
