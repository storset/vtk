package org.vortikal.graphics;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import junit.framework.TestCase;

import org.apache.commons.lang.StringUtils;

public class ImageServiceImplTestIntegration extends TestCase {
    
    private final String pngImage = "originalImage.png";
    private final String jpgImage = "originalImage.jpg";
    private final String gifImage = "originalImage.gif";
    private final String notScaledImage = "originalNotScaledImage.png";
    
    private ImageServiceImpl imageService;

    
    protected void setUp() throws Exception {
        super.setUp();
        imageService = new ImageServiceImpl();
    }
    
    public void testScaleImagePNG() throws IOException {
        assertProperResize(pngImage, "300", "");
    }
    
    public void testScaleImageJPG() throws IOException {
        assertProperResize(jpgImage, "300", null);
    }
    
    public void testScaleImageGIF() throws IOException {
        assertProperResize(gifImage, "  ", "300");
    }
    
    public void testDontScaleImage() throws IOException {
        assertProperResize(notScaledImage, "", "");
        assertProperResize(notScaledImage, null, "");
        assertProperResize(notScaledImage, "", null);
        assertProperResize(notScaledImage, null, null);
    }
    
    public void testFailUknownPathFormat() {
    	ScaledImage scaledImage = null;
    	String[] invalidPaths = {"", "http://www.uio.no/image.pn", "http://www.uio.no", "www.uio.no/image.jg"};
    	for (int i = 0; i < invalidPaths.length; i++) {
    		try {
    			scaledImage = imageService.scaleImage(invalidPaths[i], "200", "200");
    			fail();
    		} catch (IOException e) {
    			assertNull("Should be null", scaledImage);
    			assertTrue("Wrong exception", e.getMessage().contains("uknown pathformat"));
    		}
		}
    }
    
    public void testScaleByPath() throws IOException {
    	String scaledWidth = "100";
    	ScaledImage scaledImage = imageService.scaleImage(
    			"http://somstudenter.files.wordpress.com/2007/05/uio-logo.jpg", scaledWidth, "");
    	assertNotNull("No image was fetched", scaledImage);
    	assertEquals("Wrong format", "jpg", scaledImage.getFormat());
    	assertEquals("Wrong width after resizing", scaledWidth, String.valueOf(scaledImage.getImage().getWidth()));
    }
    
    private void assertProperResize(String imageName, String width, String height) throws IOException {
        BufferedImage originalImage = ImageIO.read(this.getClass().getResourceAsStream(imageName));
        String format = imageName.substring(imageName.lastIndexOf(".") + 1);
        ScaledImage scaledImage = imageService.scaleImage(originalImage, format, width, height);
        assertNotNull("No image returned", scaledImage);
        assertEquals("Wrong format", format, scaledImage.getFormat());
        
        if (StringUtils.isNotBlank(width)) {
        	String scaledWidth = String.valueOf(scaledImage.getImage().getWidth());
            assertEquals("Scaling did not return widht as expected", width, scaledWidth);
        } else if (StringUtils.isNotBlank(height)) {
        	String scaledHeight= String.valueOf(scaledImage.getImage().getHeight());
            assertEquals("Scaling did not return height as expected", height, scaledHeight);
        } else {
        	String originalWidth = String.valueOf(originalImage.getWidth());
        	String scaledWidth = String.valueOf(scaledImage.getImage().getWidth());
        	String originalHeight= String.valueOf(originalImage.getHeight());
        	String scaledHeight= String.valueOf(scaledImage.getImage().getHeight());
        	assertEquals("Scaling did not return widht as expected", scaledWidth, originalWidth);
        	assertEquals("Scaling did not return height as expected", scaledHeight, originalHeight);
        }
        
        // Use this to print the scaled image for review
        //printImage(imageName, scaledImage);
    }

    /**
     * Print the scaled image to the tests build folder (target/test-classes/org/vortikal/graphics)
     * Rename it from "original*" to "scaled*"
     */
    private void printImage(String testImageName, ScaledImage scaledImage) throws IOException {
        URL url = this.getClass().getResource(testImageName);
        String path = url.toString();
        path = path.substring(path.indexOf("/")).replace("original", "scaled");
        ImageIO.write(scaledImage.getImage(), scaledImage.getFormat(), new File(path));
    }

}
