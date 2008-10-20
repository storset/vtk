package org.vortikal.graphics;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.commons.lang.StringUtils;

public class ScaledImage {
	
	private BufferedImage image;
	private String originalFormat;
	
	public ScaledImage(BufferedImage image, String originalFormat) {
		this.image = image;
		this.originalFormat = originalFormat;
	}
	
	public BufferedImage getImage() {
		return this.image;
	}
	
	public String getOriginalFormat() {
		return this.originalFormat;
	}
	
	public byte[] getImageBytes(String format) throws IOException {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		String outFormat = StringUtils.isBlank(format) ? getOriginalFormat() : format;
        ImageIO.write(this.image, outFormat, byteStream);
        byte[] imageBytes = byteStream.toByteArray();
        byteStream.close();
        return imageBytes;
	}

}
