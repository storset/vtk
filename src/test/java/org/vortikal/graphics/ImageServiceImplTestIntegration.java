package org.vortikal.graphics;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import junit.framework.TestCase;

import org.apache.commons.lang.StringUtils;

public class ImageServiceImplTestIntegration extends TestCase {
    
    private final String pngImage = "originalPNGImage.png";
    private final String jpgImage = "originalJPGImage.jpg";
    private final String gifImage = "originalGIFImage.gif";
    private final String bmpImage = "originalBMPImage.bmp";
    private final String notScaledImage = "originalNotScaledImage.png";
    
    private ImageServiceImpl imageService;

    
    protected void setUp() throws Exception {
        super.setUp();
        imageService = new ImageServiceImpl();
    }
    
    public void testScaleImagePNG() throws Exception {
        assertProperResize(pngImage, 300, ImageService.HEIGHT_ANY);
    }
    
    public void testScaleImageJPG() throws Exception {
        assertProperResize(jpgImage, 300, ImageService.HEIGHT_ANY);
    }
    
    public void testScaleImageGIF() throws Exception {
        assertProperResize(gifImage, ImageService.WIDTH_ANY, 300);
    }
    
    public void testScaleImageTIF() throws Exception {
        assertProperResize(bmpImage, 250, ImageService.HEIGHT_ANY);
    }
    
    public void testDontScaleImage() throws Exception {
        assertProperResize(notScaledImage, ImageService.WIDTH_ANY, ImageService.HEIGHT_ANY);
    }
    
    public void testGetImageBytes() throws Exception {
    	int scaledWidth = 100;
    	BufferedImage originalImage = ImageIO.read(this.getClass().getResourceAsStream(pngImage));
    	ScaledImage scaledImage = imageService.scaleImage(originalImage, "png", scaledWidth, ImageService.HEIGHT_ANY);
    	assertNotNull("No image returned", scaledImage);
    	byte[] imageBytes = scaledImage.getImageBytes("png");
    	assertTrue("No imagebytes returned", imageBytes != null && imageBytes.length > 0);
    	ByteArrayInputStream in = new ByteArrayInputStream(imageBytes);
    	BufferedImage imageFromBytes = ImageIO.read(in);
    	assertNotNull("Could not recreate image from bytes", imageFromBytes);
    	assertEquals("Wrong width", scaledWidth, imageFromBytes.getWidth());
    }
    
    private void assertProperResize(String imageName, int width, int height) throws Exception {
        BufferedImage originalImage = ImageIO.read(this.getClass().getResourceAsStream(imageName));
        String format = imageName.substring(imageName.lastIndexOf(".") + 1);
        ScaledImage scaledImage = imageService.scaleImage(originalImage, format, width, height);
        assertNotNull("No image returned", scaledImage);
        assertEquals("Wrong format", format, scaledImage.getOriginalFormat());
        
        if (width != ImageService.WIDTH_ANY) {
            assertEquals("Scaling did not return widht as expected", width, scaledImage.getImage().getWidth());
        } else if (height != ImageService.HEIGHT_ANY) {
            assertEquals("Scaling did not return height as expected", height, scaledImage.getImage().getHeight());
        } else {
        	int originalWidth = originalImage.getWidth();
        	int scaledWidth = scaledImage.getImage().getWidth();
        	int originalHeight= originalImage.getHeight();
        	int scaledHeight= scaledImage.getImage().getHeight();
        	assertEquals("Scaling did not return widht as expected", scaledWidth, originalWidth);
        	assertEquals("Scaling did not return height as expected", scaledHeight, originalHeight);
        }
        
        // Use this method to print the scaled image for review
        printImage(imageName, scaledImage, null); // print it in it's original format
        if (!StringUtils.equalsIgnoreCase(format, "png")) { // print in in png if any other format
        	printImage(imageName, scaledImage, "png");
        }
    }

    /**
     * Print the scaled image to the tests build folder (target/test-classes/org/vortikal/graphics)
     * Rename it from "original*" to "scaled*"
     * If 'desiredFormat' is set, print image to that format
     */
    private void printImage(String testImageName, ScaledImage scaledImage, String desiredFormat) throws IOException {
        URL url = this.getClass().getResource(testImageName);
        String path = url.toString();
        path = path.substring(path.indexOf("/")).replace("original", "scaled");
        String format = scaledImage.getOriginalFormat();
        if (StringUtils.isNotBlank(desiredFormat)) {
        	format = desiredFormat;
        	path = path.substring(0, path.lastIndexOf(".") + 1) + desiredFormat;
        }
        ImageIO.write(scaledImage.getImage(), format, new File(path));
    }

}
