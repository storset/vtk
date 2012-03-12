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

import java.awt.Dimension;
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
    private int width;
    private Set<String> supportedFormats;
    private boolean scaleUp = false;
    
    private long maxSourceImageFileSize = 35000000;
    private long maxSourceImageRawMemoryUsage = 100000000;

    @Override
    public boolean evaluate(Property property, PropertyEvaluationContext ctx) throws PropertyEvaluationException {
        if (property.isValueInitialized() 
                && ctx.getEvaluationType() != Type.ContentChange
                && ctx.getEvaluationType() != Type.Create) {
            return true;
        }

        try {
            // Check max source content length constraint
            long contentLength = ctx.getContent().getContentLength();
            if (contentLength >= this.maxSourceImageFileSize) {
                log.info("Unable to create thumbnail, image size exceeds maximum limit: " + contentLength);
                return false;
            }
            
            // Check max source image memory usage constraint
            Dimension dim = (Dimension) ctx.getContent().getContentRepresentation(Dimension.class);
            if (dim != null) {
                long estimatedMemoryUsage = estimateMemoryUsage(dim);
                if (log.isDebugEnabled()) {
                    log.debug("Estimated memory usage for image of " 
                            + dim.width + "x" + dim.height + " = " + estimatedMemoryUsage + " bytes");
                }
                if (estimatedMemoryUsage > this.maxSourceImageRawMemoryUsage) {
                    log.warn("Memory usage estimate for source image of dimension " 
                            + dim.width + "x" + dim.height + " exceeds limit of "
                            + this.maxSourceImageRawMemoryUsage + " bytes.");
                    return false;
                }
            }

            BufferedImage image = (BufferedImage) ctx.getContent().getContentRepresentation(BufferedImage.class);
            if (image == null) {
                return false;
            }

            Property contentType = ctx.getNewResource().getProperty(Namespace.DEFAULT_NAMESPACE,
                    PropertyType.CONTENTTYPE_PROP_NAME);
            String mimetype = contentType.getStringValue();
            String imageFormat = mimetype.substring(mimetype.lastIndexOf("/") + 1);

            if (!supportedFormats.contains(imageFormat.toLowerCase())) {
                log.info("Unable to create thumbnail, unsupported format: " + imageFormat);
                return false;
            }

            if (!scaleUp && image.getWidth() <= this.width) {
                if (log.isDebugEnabled()) {
                    log.debug("Will not create a thumbnail: configured NOT to scale up");
                }
                return false;
            }

            ScaledImage thumbnail = imageService.scaleImage(image, imageFormat, width, ImageService.HEIGHT_ANY);

            // TODO lossy-compression -> jpeg
            String thumbnailFormat = !imageFormat.equalsIgnoreCase("png") ? "png" : imageFormat;

            property.setBinaryValue(thumbnail.getImageBytes(thumbnailFormat), "image/" + thumbnailFormat);
            return true;

        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Unable to create thumbnail of content", e);
            } else {
                log.info("Unable to create thumbnail of content");
            }
            return false;
        }
    }

    /**
     * Estimates the raw memory usage for an image where each pixel
     * uses 24 bits or 3 bytes of memory.
     * 
     * @param dim The <code>Dimension</code> of the image.
     * @return The estimated raw memory usage in bytes.
     */
    private long estimateMemoryUsage(Dimension dim) {
        return (long)dim.height * (long)dim.width * 24 / 8;
    }

    @Required
    public void setImageService(ImageService imageService) {
        this.imageService = imageService;
    }

    @Required
    public void setWidth(int width) {
        if (width < 1) {
            throw new IllegalArgumentException("scale width must be >= 1");
        }
        this.width = width;
    }

    @Required
    public void setSupportedFormats(Set<String> supportedFormats) {
        this.supportedFormats = supportedFormats;
    }

    public void setScaleUp(boolean scaleUp) {
        this.scaleUp = scaleUp;
    }
    
    public void setMaxSourceImageFileSize(long maxSourceImageFileSize) {
        if (maxSourceImageFileSize < 1) {
            throw new IllegalArgumentException("maxSourceImageFileSize must be >= 1");
        }
        this.maxSourceImageFileSize = maxSourceImageFileSize;
    }

    /**
     * Set cap on estimated raw memory usage on image during scale operation.
     * The estimate is based upon a memory usage of 24 bits per pixel, which
     * should be the most common type. 32bpp images will consume more than
     * actual estimate. To fix that, one needs to provide the bpp value
     * from the {@link org.vortikal.repository.content.ImageContentFactory}.
     * 
     * Default value of 350MB is roughly equivalent to an image of about 10000x10000.
     * 
     * @param maxSourceImageRawMemoryUsage 
     */
    public void setMaxSourceImageRawMemoryUsage(long maxSourceImageRawMemoryUsage) {
        if (maxSourceImageRawMemoryUsage < 1) {
            throw new IllegalArgumentException("maxSourceImageRawMemoryUsage must be >= 1");
        }
        this.maxSourceImageRawMemoryUsage = maxSourceImageRawMemoryUsage;
    }
}
