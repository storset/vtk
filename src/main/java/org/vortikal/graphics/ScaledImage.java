package org.vortikal.graphics;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

public class ScaledImage {
	
	private BufferedImage image;
	private String format;
	
	public ScaledImage(BufferedImage image, String format) {
		this.image = image;
		this.format = format;
	}
	
	public BufferedImage getImage() {
		return this.image;
	}
	
	public String getFormat() {
		return this.format;
	}
	
	public byte[] getImageBytes() throws IOException {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ImageIO.write(this.image, this.format, byteStream);
        byte[] imageBytes = byteStream.toByteArray();
        byteStream.close();
        return imageBytes;
	}

}
