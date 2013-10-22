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
package org.vortikal.graphics;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.ImageInputStream;


/**
 * Utility methods for downscaling, encoding and fetching metadata
 * from encoded images.
 */
public final class ImageUtil {
    
    /**
     * Downscale an image to a target width. Height is calculated so that
     * original proportions are kept intact. If target width is larger
     * than the original image width, no scaling will be performed !
     * 
     * @param image a {@link BufferedImage} 
     * @param targetWidth the desired width in pixels
     * @return
     */
    public static BufferedImage downscaleToWidth(BufferedImage image, int targetWidth) {
        Dimension scaleDimension = getScaleDimension(targetWidth, -1, image);
        return downscale(image, scaleDimension.width, scaleDimension.height);
    }
    
    /**
     * Downscale an image to a target height. Width is calculated so that
     * original proportions are kept intact. If target height is larger
     * than the original image, no scaling will be performed.
     * 
     * @param image a {@link BufferedImage}
     * @param targetHeight the desired height in pixels
     * @return a new <code>BufferedImage</code> which has been downscaled.
     */
    public static BufferedImage downscaleToHeight(BufferedImage image, int targetHeight) {
        Dimension scaleDimension = getScaleDimension(-1, targetHeight, image);
        return downscale(image, scaleDimension.width, scaleDimension.height);
    }
    
    private static Dimension getScaleDimension(int width, int height, BufferedImage originalImage) {
        // Width has precedence over height
        if (width != -1) {
            // If width is defined, use it as base for calculating
            // new dimensions (ignore height)
            height = width * originalImage.getHeight() / originalImage.getWidth();
            return new Dimension(width, height);
        } else if (height != -1) {
            // No width specified, use height as base instead
            width = height * originalImage.getWidth() / originalImage.getHeight();
            return new Dimension(width, height);
        }
        
        // Both parameters were blank, return original dimensions
        return new Dimension(originalImage.getWidth(), originalImage.getHeight());
    }

    /**
     * Downscale an image to a target height and width. If target width or height
     * are larger than the original, then there will be no change in respective size.
     * 
     * @param img BufferedImage to downscale.
     * @param targetWidth Desired width in pixels.
     * @param targetHeight Desired height in pixels.
     * @return 
     */
    public static BufferedImage downscale(BufferedImage img, int targetWidth, int targetHeight) {

        final int type = (img.getTransparency() == Transparency.OPAQUE) ?
                      BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage ret = img;
        int w = img.getWidth();
        int h = img.getHeight();

        // This algo only does downscaling, so if target sizes are bigger than original, we set them
        // to original (avoid loop below running 10 times unnecessarily when target sizes
        // are bigger than original sizes).
        if (targetWidth > w) targetWidth = w;
        if (targetHeight > h) targetHeight = h;

        int i = 0;
        do {
            if (w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }

            if (h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }

            BufferedImage tmp = new BufferedImage(Math.max(w, 1), Math.max(h, 1), type);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(ret, 0, 0, Math.max(w, 1), Math.max(h, 1), null);
            g2.dispose();

            ret = tmp;
            if (++i >= 10) {
                break;
            }
        } while (w != targetWidth || h != targetHeight);

        return ret;
    }

    /**
     * Encode a {@link BufferedImage} to given format and return the data.
     * @param image image in memory
     * @param format image format name (typicall same as file format extension)
     * @return encoded image data in requested format as a byte array
     * @throws IOException 
     */
    public static byte[] getImageBytes(BufferedImage image, String format) throws IOException {
        if (format == null || format.isEmpty()) {
            throw new IllegalArgumentException("Image format name cannot be null or empty");
        }
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ImageIO.write(image, format, byteStream);
        byte[] imageBytes = byteStream.toByteArray();
        byteStream.close();
        return imageBytes;
    }
    
    /**
     * Extract dimensions from an encoded image data stream.
     * 
     * @param stream the input stream with the encoded image data (any format).
     * @return a {@link Dimension} with width and height of the image in the data stream
     *         or <code>null</code> if the metadata could not be extracted.
     * @throws IOException 
     */
    public static Dimension getImageStreamDimension(InputStream stream) throws IOException {
        ImageInputStream iis = new FileCacheImageInputStream(stream, null);
        try {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                reader.setInput(iis);
                int width = reader.getWidth(reader.getMinIndex());
                int height = reader.getHeight(reader.getMinIndex());
                reader.dispose();
                return new Dimension(width, height);
            }
        } finally {
            iis.close();
        }

        return null;
    }
    
}
