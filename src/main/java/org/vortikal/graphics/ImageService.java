package org.vortikal.graphics;

import java.awt.image.BufferedImage;
import java.io.IOException;

public interface ImageService {
    
    public ScaledImage scaleImage(String path, String width, String height) throws IOException;
    
    public ScaledImage scaleImage(BufferedImage image, String format, String width, String height) throws IOException;

}
