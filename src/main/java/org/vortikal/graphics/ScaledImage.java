package org.vortikal.graphics;

import java.awt.image.BufferedImage;

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

}
