/* Copyright (c) 2013, University of Oslo, Norway
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
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.imageio.ImageIO;

import junit.framework.TestCase;

import org.apache.commons.lang.StringUtils;

public class ImageUtilTest extends TestCase {
    private String headless = null;
    
    private final String pngImage = "originalPNGImage.png";
    private final String jpgImage = "originalJPGImage.jpg";
    private final String gifImage = "originalGIFImage.gif";
    private final String bmpImage = "originalBMPImage.bmp";
    private final String notScaledImage = "originalNotScaledImage.png";
    private final String squeezedImage = "originalSqueezedImage.png";
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        headless = System.getProperty("java.awt.headless");
        System.setProperty("java.awt.headless", "true");
    }
    
    @Override
    protected void tearDown() throws Exception {
        System.setProperty("java.awt.headless",
                headless != null ? headless : "false");
    }
    
    // Check that returned image has equal dimensions when trying to
    // upscale (not supported)
    public void testUpscaleNoChange() throws IOException {
        BufferedImage originalImage = ImageIO.read(this.getClass().getResourceAsStream(pngImage));
        BufferedImage scaled = ImageUtil.downscale(originalImage, 4096, 4096);
        assertEquals("Expected no change in height", originalImage.getHeight(), scaled.getHeight());
        assertEquals("Expected no change in width", originalImage.getWidth(), scaled.getWidth());
    }

    // generate squeezed image using free form down-scaling
    public void testFreeformDownscale() throws IOException {
        assertProperResize(squeezedImage, 100, 290);
    }
    
    public void testScaleImagePNG() throws IOException {
        assertProperResize(pngImage, 300, -1);
    }
    
    public void testScaleImageJPG() throws IOException {
        assertProperResize(jpgImage, 300, -1);
    }
    
    public void testScaleImageGIF() throws IOException {
        assertProperResize(gifImage, -1, 300);
    }
    
    public void testScaleImageTIF() throws IOException {
        assertProperResize(bmpImage, 250, -1);
    }
    
    public void testDontScaleImage() throws IOException {
        assertProperResize(notScaledImage, -1, -1);
    }
    
    public void testGetImageStreamDimensionPNG() throws IOException {
        InputStream imageStream = this.getClass().getResourceAsStream(pngImage);
        Dimension dim = ImageUtil.getImageStreamDimension(imageStream);
        assertEquals("Unexpected width", 600, dim.width);
        assertEquals("Unexpected height", 600, dim.height);
    }

    public void testGetImageStreamDimensionJPG() throws IOException {
        InputStream imageStream = this.getClass().getResourceAsStream(jpgImage);
        Dimension dim = ImageUtil.getImageStreamDimension(imageStream);
        assertEquals("Unexpected width", 500, dim.width);
        assertEquals("Unexpected height", 500, dim.height);
    }
    
    public void testGetImageBytes() throws Exception {
    	int scaledWidth = 100;
    	BufferedImage originalImage = ImageIO.read(this.getClass().getResourceAsStream(pngImage));
    	BufferedImage scaledImage = ImageUtil.downscaleToWidth(originalImage, scaledWidth);
    	assertNotNull("No image returned", scaledImage);
    	byte[] imageBytes = ImageUtil.getImageBytes(scaledImage, "png");
    	assertTrue("No imagebytes returned", imageBytes != null && imageBytes.length > 0);
    	ByteArrayInputStream in = new ByteArrayInputStream(imageBytes);
    	BufferedImage imageFromBytes = ImageIO.read(in);
    	assertNotNull("Could not recreate image from bytes", imageFromBytes);
    	assertEquals("Wrong width", scaledWidth, imageFromBytes.getWidth());
    }
    
    private void assertProperResize(String imageName, int width, int height) throws IOException {
        BufferedImage originalImage = ImageIO.read(this.getClass().getResourceAsStream(imageName));
        BufferedImage scaledImage;
        if (width != -1 && height != -1) {
            // Free form down-scaling in both directions
            scaledImage = ImageUtil.downscale(originalImage, width, height);
            assertEquals("Unexpected width", width, scaledImage.getWidth());
            assertEquals("Unexpected height", height, scaledImage.getHeight());
        } else if (width != -1) {
            scaledImage = ImageUtil.downscaleToWidth(originalImage, width);
            assertEquals("Unexpected width", width, scaledImage.getWidth());
        } else if (height != -1) {
            scaledImage = ImageUtil.downscaleToHeight(originalImage, height);
            assertEquals("Unexpected height", height, scaledImage.getHeight());
        } else {
            // No scaling specified
            scaledImage = ImageUtil.downscale(originalImage, originalImage.getWidth(), originalImage.getHeight());
            assertEquals("Unexpected width", originalImage.getWidth(), scaledImage.getWidth());
            assertEquals("Unexpected height", originalImage.getHeight(), scaledImage.getHeight());
        }
        
        // Use this method to print the scaled image for review
        String format = imageName.substring(imageName.lastIndexOf(".") + 1);
        if (StringUtils.isBlank(format)) {
            format = "png";
        }
        writeImageFile(imageName, scaledImage, format); // print it in it's original format
        if (!StringUtils.equalsIgnoreCase(format, "png")) { // print in in png if any other format
        	writeImageFile(imageName, scaledImage, "png");
        }
    }

    /**
     * Write the scaled image to file in the tests build folder,
     * typically <code>target/test-classes/org/vortikal/graphics/</code>.
     * Rename it from "original*" to "scaled*".
     * If 'desiredFormat' is set, print image to that format
     */
    private void writeImageFile(String testImageName, BufferedImage scaledImage, String format) throws IOException {
        URL url = this.getClass().getResource(testImageName);
        String path = url.toString();
        path = path.substring(path.indexOf("/")).replace("original", "scaled");
        path = path.substring(0, path.lastIndexOf('.')+1) + format;
        ImageIO.write(scaledImage, format, new File(path));
    }

}
